/**
 * PKCE 工具 - 用于公开客户端的 OAuth2 授权码流程
 */
import { sha256 as jsSha256 } from 'js-sha256'

export function generateRandomString(length = 128) {
  const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'
  let result = ''
  const values = crypto.getRandomValues(new Uint8Array(length))
  for (let i = 0; i < length; i++) {
    result += charset[values[i] % charset.length]
  }
  return result
}

async function sha256(plain) {
  if (typeof crypto !== 'undefined' && crypto.subtle && crypto.subtle.digest) {
    const encoder = new TextEncoder()
    const data = encoder.encode(plain)
    return await crypto.subtle.digest('SHA-256', data)
  }
  const hashArray = jsSha256.array(plain)
  return new Uint8Array(hashArray).buffer
}

function base64UrlEncode(arrayBuffer) {
  const bytes = new Uint8Array(arrayBuffer)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

export async function generatePKCE() {
  const codeVerifier = generateRandomString(128)
  const hashed = await sha256(codeVerifier)
  const codeChallenge = base64UrlEncode(hashed)
  return { codeVerifier, codeChallenge, codeChallengeMethod: 'S256' }
}
