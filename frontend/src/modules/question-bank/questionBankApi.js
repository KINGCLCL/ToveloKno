import api from '../../api'

export function generatePractice(data) {
  return api.post('/question-bank/practice/questions', data)
}

export function submitPracticeAnswer(data) {
  return api.post('/question-bank/practice/answers', data)
}

export function fetchPracticeDashboard() {
  return api.get('/question-bank/dashboard')
}

export function fetchQuestionBankAnalytics() {
  return api.get('/question-bank/analytics')
}

export function fetchQuestionBankSettings() {
  return api.get('/question-bank/settings')
}

export function updateQuestionBankSettings(data) {
  return api.put('/question-bank/settings', data)
}
