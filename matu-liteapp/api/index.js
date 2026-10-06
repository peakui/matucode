import { request } from './request.js'
const get = (p, d, guest = true) => request(p, d, 'GET', { guest })
const id = (value) => encodeURIComponent(String(value))
export const feedPaths = { article: '/posts', check: '/checks', qa: '/qa/questions' }
export const api = {
  feed: (type, data) => get(feedPaths[type], data),
  categories: (type) => get(type === 'qa' ? '/qa/categories' : '/posts/categories'),
  detail: (type, value) => get(`${feedPaths[type]}/${id(value)}`),
  discussions: (type, value, data) =>
    get(`${feedPaths[type]}/${id(value)}/${type === 'qa' ? 'answers' : 'comments'}`, data),
  interact: (type, value, action, active) =>
    request(`${feedPaths[type]}/${id(value)}/${action}`, {}, active ? 'DELETE' : 'POST'),
  comment: (type, value, content) => request(`${feedPaths[type]}/${id(value)}/comments`, { content }, 'POST'),
  courses: (data) => get('/courses', data),
  course: (value) => get('/courses/' + id(value)),
  play: (value) => get(`/courses/videos/${id(value)}/play`),
  progress: (value) => get(`/courses/${id(value)}/progress/mine`, {}, false),
  saveProgress: (value, data) => request(`/courses/${id(value)}/progress`, data, 'POST'),
  notes: (data) => get('/courses/notes', data, false),
  interviewCategories: () => get('/interview/categories'),
  questions: (data) => get('/interview/questions', data),
  question: (value) => get('/interview/questions/' + id(value)),
  questionProgress: (value) => get(`/interview/questions/${id(value)}/progress`, {}, false),
  saveQuestionProgress: (value, status) =>
    request(`/interview/questions/${id(value)}/progress`, { status }, 'PUT'),
  myQuestions: (data) => get('/qa/questions/mine', data, false),
  myChecks: (data) => get('/checks', data, false),
  statistics: (userId) => get('/checks/statistics', { userId }, false),
  me: () => get('/auth/mini/me', {}, false),
  profile: () => get('/auth/me', {}, false),
  saveProfile: (data) => request('/auth/me', data, 'PUT'),
  avatar: (avatarUrl) => request('/auth/me/avatar', { avatarUrl }, 'PUT'),
  capabilities: () => request('/auth/mini/capabilities', {}, 'GET', { publicAuth: true }),
  login: (data) => request('/auth/mini/login', data, 'POST', { publicAuth: true }),
  wechat: (code) => request('/auth/mini/wechat/login', { code }, 'POST', { publicAuth: true }),
  complete: (data) => request('/auth/mini/wechat/complete', data, 'POST', { publicAuth: true }),
  logout: () => request('/auth/mini/logout', {}, 'POST'),
}
