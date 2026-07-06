import api from '../../api'

export function fetchClassificationOverview() {
  return api.get('/classifications/overview')
}

export function createCategory(data) {
  return api.post('/classifications/categories', data)
}

export function updateCategory(id, data) {
  return api.put(`/classifications/categories/${id}`, data)
}

export function deleteCategory(id) {
  return api.delete(`/classifications/categories/${id}`)
}

export function createTag(data) {
  return api.post('/classifications/tags', data)
}

export function updateTag(id, data) {
  return api.put(`/classifications/tags/${id}`, data)
}

export function deleteTag(id) {
  return api.delete(`/classifications/tags/${id}`)
}

export function renameKnowledgePoint(data) {
  return api.put('/classifications/knowledge-points', data)
}

export function clearKnowledgePoint(name, subject) {
  return api.delete('/classifications/knowledge-points', {
    params: { name, subject: subject || undefined },
  })
}
