import request from '@/utils/request'

// ====== FR-01 auth(由刘家豪实现) ======
export function login(data) { return request.post('/auth/login', data) }
export function logout() { return request.post('/auth/logout') }
export function me() { return request.get('/auth/me') }
export function changePassword(data) { return request.post('/auth/change-password', data) }
export function pageUsers(params) { return request.get('/users', { params }) }
export function disableUser(id) { return request.post(`/users/${id}/disable`) }

// ====== FR-02 model(由向锏楠实现) ======
export function pageModels(params) { return request.get('/models', { params }) }
export function listEnabledModels() { return request.get('/models/enabled') }
export function createModel(data) { return request.post('/models', data) }
export function updateModel(id, data) { return request.put(`/models/${id}`, data) }
export function deleteModel(id) { return request.delete(`/models/${id}`, data) }
export function testModel(id, question) { return request.post('/models/test', { id, testQuestion: question }) }

// ====== FR-03 question(由向锏楠实现 ✅) ======
export function pageQuestions(params) { return request.get('/questions', { params }) }
export function getQuestion(id) { return request.get(`/questions/${id}`) }
export function createQuestion(data) { return request.post('/questions', data) }
export function updateQuestion(id, data) { return request.put(`/questions/${id}`, data) }
export function deleteQuestion(id) { return request.delete(`/questions/${id}`) }
export function listQuestionLibrary() { return request.get('/questions/library') }
export function importQuestions(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/questions/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// ====== FR-04 evaluation(由梁倩倩实现) ======
export function createEvaluation(data) { return request.post('/evaluations', data) }
export function runEvaluation(id) { return request.post(`/evaluations/${id}/run`) }
export function getEvaluation(id) { return request.get(`/evaluations/${id}`) }
export function pageEvaluations(params) { return request.get('/evaluations', { params }) }
export function listAnswers(id) { return request.get(`/evaluations/${id}/answers`) }

// ====== FR-05 score(由宋子翔实现) ======
export function submitScore(data) { return request.post('/scores', data) }
export function listMyScores(evaluationId) { return request.get('/scores/by-evaluation', { params: { evaluationId } }) }

// ====== FR-06 stats(由宋子翔实现) ======
export function getKappa(evaluationId) { return request.get('/stats/kappa', { params: { evaluationId } }) }
export function getControversial(evaluationId) { return request.get('/stats/controversial', { params: { evaluationId } }) }
export function getScorerRanking(evaluationId) { return request.get('/stats/scorer-ranking', { params: { evaluationId } }) }
export function getModelRanking(evaluationId) { return request.get('/stats/model-ranking', { params: { evaluationId } }) }

// ====== FR-07 billing(由梁倩倩实现) ======
export function getBillingSummary(evaluationId) { return request.get('/billing/summary', { params: { evaluationId } }) }
export function getBillingTimeSeries(evaluationId, granularity = 'hour') {
  return request.get('/billing/time-series', { params: { evaluationId, granularity } })
}
export function getBillingByModel(evaluationId) { return request.get('/billing/by-model', { params: { evaluationId } }) }

// ====== FR-08 export(由周文泽实现) ======
export function downloadExcelUrl(id) { return `/api/export/${id}/excel` }
export function downloadPdfUrl(id) { return `/api/export/${id}/pdf` }
