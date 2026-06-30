import request from '@/utils/request'

// ====== FR-01 auth(由刘家豪实现) ======
export function login(data) { return request.post('/auth/login', data) }
export function logout() { return request.post('/auth/logout') }
export function me() { return request.get('/auth/me') }
export function changePassword(data) { return request.post('/auth/change-password', data) }
export function pageUsers(params) { return request.get('/users', { params }) }
export function disableUser(id) { return request.post(`/users/${id}/disable`) }

// ====== FR-02 model(由向锏楠实现 ✅) ======
export function pageModels(params) { return request.get('/models', { params }) }
export function listEnabledModels() { return request.get('/models/enabled') }
export function getModel(id) { return request.get(`/models/${id}`) }
export function createModel(data) { return request.post('/models', data) }
export function updateModel(id, data) { return request.put(`/models/${id}`, data) }
export function deleteModel(id) { return request.delete(`/models/${id}`) }
export function toggleModelStatus(id, status) { return request.post(`/models/${id}/toggle`, { status }) }
export function testModel(id, question) { return request.post('/models/test', { id, question }) }
export function testModelConfig(data) { return request.post('/models/test', data) }

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

// ====== FR-04 evaluation(由梁倩倩实现 ✅) ======
export function createEvaluation(data) { return request.post('/evaluations', data) }
export function runEvaluation(id) { return request.post(`/evaluations/${id}/run`) }
export function getEvaluation(id) { return request.get(`/evaluations/${id}`) }
export function pageEvaluations(params) { return request.get('/evaluations', { params }) }
export function listAnswers(id) { return request.get(`/evaluations/${id}/answers`) }
export function deleteEvaluation(id) { return request.delete(`/evaluations/${id}`) }

// ====== FR-05 score(由宋子翔实现 ✅) ======
export function submitScore(data) { return request.post('/scores', data) }
export function listMyScores(evaluationId) { return request.get('/scores/by-evaluation', { params: { evaluationId } }) }
export function listScoresByAnswer(answerId) { return request.get(`/scores/by-answer/${answerId}`) }
export function checkScore(answerId) { return request.get('/scores/check', { params: { answerId } }) }

// ====== FR-06 stats(由宋子翔实现 ✅) ======
export function getKappa(evaluationId) { return request.get('/stats/kappa', { params: { evaluationId } }) }
export function getControversial(evaluationId) { return request.get('/stats/controversial', { params: { evaluationId } }) }
export function getScorerRanking(evaluationId) { return request.get('/stats/scorer-ranking', { params: { evaluationId } }) }
export function getModelRanking(evaluationId) { return request.get('/stats/model-ranking', { params: { evaluationId } }) }

// ====== FR-07 billing(由梁倩倩实现 ✅) ======
export function getBillingSummary(evaluationId) { return request.get('/billing/summary', { params: { evaluationId } }) }
export function getBillingTimeSeries(evaluationId, granularity = 'hour') {
  return request.get('/billing/time-series', { params: { evaluationId, granularity } })
}
export function getBillingByModel(evaluationId) { return request.get('/billing/by-model', { params: { evaluationId } }) }
export function getBillingPlatform() { return request.get('/billing/platform-summary') }
export async function downloadBillingCsv(evaluationId) {
  const r = await request.get(`/billing/export?evaluationId=${evaluationId}`, { responseType: 'blob' })
  const blob = new Blob([r], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `billing_eval_${evaluationId}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

// ====== FR-08 export(由周文泽实现 ✅) ======
export function getExportMeta(id) { return request.get(`/export/${id}/meta`) }
export function getExportHtml(id) { return request.get(`/export/${id}/html-content`) }
export async function downloadExcelUrl(id) {
  const r = await request.get(`/export/${id}/excel`, { responseType: 'blob' })
  const blob = new Blob([r], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `evaluation_${id}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
export async function downloadPdfUrl(id) {
  const r = await request.get(`/export/${id}/pdf`, { responseType: 'blob' })
  const blob = new Blob([r], { type: 'text/html' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `evaluation_${id}.html`
  a.click()
  URL.revokeObjectURL(url)
}

// ====== Arena 盲评 + Elo(由刘家豪实现 ✅) ======
export function arenaQuickEval(data) { return request.post('/arena/quick-eval', data) }
export function arenaBatchEval(data) { return request.post('/arena/batch-eval', data) }
export function arenaVote(data) { return request.post('/arena/vote', data) }
export function arenaRanking(category) {
  return request.get('/arena/ranking', { params: category ? { category } : {} })
}

// ====== Dashboard 总览(刘家豪 ✅) ======
export function getDashboardStats() { return request.get('/dashboard/stats') }
export function getDashboardRecent(limit = 10) { return request.get('/dashboard/recent', { params: { limit } }) }
