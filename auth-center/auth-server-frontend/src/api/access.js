import api from './index'

export function getUserAccess(userId) {
  return api.get('/api/admin/access', { params: { userId } })
}

export function setAccess(userId, clientId, allowed) {
  return api.put('/api/admin/access', { userId, clientId, allowed })
}