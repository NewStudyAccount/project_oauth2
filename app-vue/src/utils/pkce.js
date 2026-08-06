/**
 * OAuth2 PKCE 工具函数
 * 参考: https://datatracker.ietf.org/doc/html/rfc7636
 */

/**
 * 生成随机字符串 (code_verifier)
 * @param {number} length - 长度 (43-128)
 * @returns {string}
 */
export function generateCodeVerifier(length = 128) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'
  const array = new Uint8Array(length)
  crypto.getRandomValues(array)
  return Array.from(array, byte => chars[byte % chars.length]).join('')
}

/**
 * 计算 SHA-256 哈希 (code_challenge)
 * @param {string} verifier - code_verifier
 * @returns {Promise<string>} - Base64URL 编码的哈希值
 */
export async function generateCodeChallenge(verifier) {
  const encoder = new TextEncoder()
  const data = encoder.encode(verifier)
  const digest = await crypto.subtle.digest('SHA-256', data)
  return base64UrlEncode(digest)
}

/**
 * Base64URL 编码
 * @param {ArrayBuffer} buffer
 * @returns {string}
 */
function base64UrlEncode(buffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  bytes.forEach(byte => binary += String.fromCharCode(byte))
  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

/**
 * 生成随机 state 参数
 * @returns {string}
 */
export function generateState() {
  const array = new Uint8Array(32)
  crypto.getRandomValues(array)
  return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('')
}
