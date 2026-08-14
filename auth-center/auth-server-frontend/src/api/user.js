import api from './index'

export function listUsers() {
  return api.get('/api/admin/users')
}

export function setUserStatus(id, enabled) {
  return api.put(`/api/admin/users/${id}/status`, { enabled })
}