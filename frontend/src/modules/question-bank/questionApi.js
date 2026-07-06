import api from '../../api'

export function fetchQuestions(params) {
  return api.get('/questions', { params })
}

export function fetchQuestionStats() {
  return api.get('/questions/stats')
}

export function fetchQuestion(id) {
  return api.get(`/questions/${id}`)
}

export function createQuestion(data) {
  return api.post('/questions', data)
}

export function updateQuestion(id, data) {
  return api.put(`/questions/${id}`, data)
}

export function moveQuestionToRecycleBin(id) {
  return api.delete(`/questions/${id}`)
}

export function batchMoveQuestionsToRecycleBin(ids) {
  return api.post('/questions/batch-delete', { ids })
}

export function batchUpdateQuestionStatus(ids, status) {
  return api.put('/questions/batch/status', { ids, status })
}

export function restoreQuestion(id) {
  return api.put(`/questions/${id}/restore`)
}

export function permanentlyDeleteQuestion(id) {
  return api.delete(`/questions/recycle-bin/${id}`)
}
