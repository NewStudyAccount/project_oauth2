import api from './index'

export function getAuditLogs(params) {
  return api.get('/api/admin/audit-logs', { params })
}

export function getStats() {
  return api.get('/api/admin/stats')
}