export const API_BASE_URL = "http://localhost:8080"

export const COOKIE_NAME = "sms_token"
export const REFRESH_COOKIE_NAME = "sms_refresh"
export const USER_ID_COOKIE = "sms_uid"

export const ROLES = {
  ADMIN: "ADMIN",
  TEACHER: "TEACHER",
  STUDENT: "STUDENT",
} as const

export type Role = (typeof ROLES)[keyof typeof ROLES]
