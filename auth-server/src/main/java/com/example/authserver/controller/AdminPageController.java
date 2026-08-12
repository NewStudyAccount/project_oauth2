package com.example.authserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.authserver.entity.*;
import com.example.authserver.repository.*;
import com.example.authserver.service.AccessControlService;
import com.example.authserver.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPageController {

    private final SysUserMapper sysUserMapper;
    private final OAuth2ClientMapper oauth2ClientMapper;
    private final UserClientAccessMapper userClientAccessMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final AccessControlService accessControlService;
    private final TokenBlacklistService tokenBlacklistService;

    // ========== 管理后台首页 ==========

    @GetMapping
    public String index(Model model) {
        long userCount = sysUserMapper.selectCount(null);
        long clientCount = oauth2ClientMapper.selectCount(null);
        long auditCount = auditLogMapper.selectCount(null);
        model.addAttribute("userCount", userCount);
        model.addAttribute("clientCount", clientCount);
        model.addAttribute("auditCount", auditCount);
        return "admin/index";
    }

    // ========== 用户管理 ==========

    @GetMapping("/users")
    public String users(Model model) {
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId));
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            ra.addFlashAttribute("error", "用户不存在");
            return "redirect:/admin/users";
        }
        int newStatus = user.getStatus() == 1 ? 0 : 1;
        user.setStatus(newStatus);
        sysUserMapper.updateById(user);

        if (newStatus == 0) {
            tokenBlacklistService.revokeAllUserTokens(id, "admin_disable");
        }

        ra.addFlashAttribute("message", "用户 " + user.getUsername() + (newStatus == 1 ? " 已启用" : " 已禁用"));
        return "redirect:/admin/users";
    }

    // ========== 客户端管理 ==========

    @GetMapping("/clients")
    public String clients(Model model) {
        List<OAuth2Client> clients = oauth2ClientMapper.selectList(null);
        model.addAttribute("clients", clients);
        return "admin/clients";
    }

    @GetMapping("/clients/add")
    public String addClientForm(Model model) {
        model.addAttribute("client", new OAuth2Client());
        return "admin/client-form";
    }

    @GetMapping("/clients/{id}/edit")
    public String editClientForm(@PathVariable Long id, Model model) {
        OAuth2Client client = oauth2ClientMapper.selectById(id);
        if (client == null) {
            return "redirect:/admin/clients";
        }
        model.addAttribute("client", client);
        return "admin/client-form";
    }

    @PostMapping("/clients/save")
    public String saveClient(@ModelAttribute OAuth2Client client, RedirectAttributes ra) {
        if (client.getId() != null) {
            oauth2ClientMapper.updateById(client);
            ra.addFlashAttribute("message", "客户端更新成功");
        } else {
            oauth2ClientMapper.insert(client);
            ra.addFlashAttribute("message", "客户端创建成功");
        }
        return "redirect:/admin/clients";
    }

    @PostMapping("/clients/{id}/delete")
    public String deleteClient(@PathVariable Long id, RedirectAttributes ra) {
        oauth2ClientMapper.deleteById(id);
        ra.addFlashAttribute("message", "客户端已删除");
        return "redirect:/admin/clients";
    }

    // ========== 权限管理 ==========

    @GetMapping("/access")
    public String access(@RequestParam(required = false) Long userId, Model model) {
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        List<OAuth2Client> clients = oauth2ClientMapper.selectList(
                new LambdaQueryWrapper<OAuth2Client>().eq(OAuth2Client::getStatus, 1));

        model.addAttribute("users", users);
        model.addAttribute("clients", clients);
        model.addAttribute("selectedUserId", userId);

        if (userId != null) {
            List<UserClientAccess> accesses = userClientAccessMapper.selectList(
                    new LambdaQueryWrapper<UserClientAccess>()
                            .eq(UserClientAccess::getUserId, userId));
            model.addAttribute("accesses", accesses);
            model.addAttribute("selectedUser", sysUserMapper.selectById(userId));
        }

        return "admin/access";
    }

    @PostMapping("/access/save")
    public String saveAccess(@RequestParam Long userId, @RequestParam String clientId,
                             @RequestParam boolean allowed, RedirectAttributes ra) {
        accessControlService.setAccess(userId, clientId, allowed);
        ra.addFlashAttribute("message", "权限更新成功");
        return "redirect:/admin/access?userId=" + userId;
    }

    // ========== 审计日志 ==========

    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(required = false) String action,
                            @RequestParam(required = false) String username,
                            Model model) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isEmpty()) {
            wrapper.eq(SysAuditLog::getAction, action);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.eq(SysAuditLog::getUsername, username);
        }
        wrapper.orderByDesc(SysAuditLog::getId).last("LIMIT 200");

        List<SysAuditLog> logs = auditLogMapper.selectList(wrapper);
        model.addAttribute("logs", logs);
        model.addAttribute("actionFilter", action);
        model.addAttribute("usernameFilter", username);
        return "admin/audit-logs";
    }

    // ========== Token 管理 ==========

    @GetMapping("/tokens")
    public String tokens(Model model) {
        // 查询有活跃授权的记录
        List<String> activeAuthorizations = Collections.singletonList(StringUtils.collectionToDelimitedString(
                List.of(), ","));
        model.addAttribute("revokeMessage", null);
        return "admin/tokens";
    }

    @PostMapping("/tokens/revoke")
    public String revokeTokens(@RequestParam Long userId, RedirectAttributes ra) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            ra.addFlashAttribute("error", "用户不存在");
            return "redirect:/admin/tokens";
        }
        tokenBlacklistService.revokeAllUserTokens(userId, "admin_revoke");
        ra.addFlashAttribute("message", "已撤销用户 " + user.getUsername() + " 的所有 Token");
        return "redirect:/admin/tokens";
    }
}
