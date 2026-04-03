import request from '../utils/request'

export const login = (data) => request.post('/auth/login', data)

export const register = (data) => request.post('/auth/register', data)

export const phoneLogin = (data) => request.post('/auth/phone-login', data)

export const emailLogin = (data) => request.post('/auth/email-login', data)

export const sendVerifyCode = (phone) => request.post('/auth/send-code', { phone })

export const sendEmailCode = (email) => request.post('/auth/send-email-code', { email })

export const refreshToken = (data) => request.post('/auth/refresh', data)

export const logout = () => request.post('/auth/logout')

export const checkPhoneExists = (phone) => request.get('/auth/check-phone', { params: { phone } })

export const checkEmailExists = (email) => request.get('/auth/check-email', { params: { email } })

export const checkUsernameExists = (username) => request.get('/auth/check-username', { params: { username } })
