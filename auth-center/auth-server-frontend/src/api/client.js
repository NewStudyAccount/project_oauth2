import api from './index'

export function listClients() {
  return api.get('/api/admin/clients')
}

export function getClient(id) {
  return api.get(`/api/admin/clients/${id}`)
}

export function createClient(data) {
  return api.post('/api/admin/clients', data)
}

export function updateClient(id, data) {
  return api.put(`/api/admin/clients/${id}`, data)
}

export function deleteClient(id) {
  return api.delete(`/api/admin/clients/${id}`)
}

export function setClientStatus(id, enabled) {
  return api.put(`/api/admin/clients/${id}/status`, { enabled })
}