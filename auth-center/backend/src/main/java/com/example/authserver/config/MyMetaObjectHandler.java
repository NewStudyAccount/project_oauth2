package com.example.authserver.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器。
 *
 * <p>配合实体类中 {@code @TableField(fill = FieldFill.INSERT)} 和
 * {@code @TableField(fill = FieldFill.INSERT_UPDATE)} 注解使用，
 * 在 INSERT / UPDATE 操作时自动填充时间字段，无需手动 set。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * INSERT 操作自动填充：设置 createdAt 和 updatedAt 为当前时间。
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * UPDATE 操作自动填充：仅更新 updatedAt 为当前时间。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
