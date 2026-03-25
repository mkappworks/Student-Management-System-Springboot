export const API_BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080"

export const COOKIE_NAME = "sms_token"
export const USER_ID_COOKIE = "sms_uid"

export const ROLES = {
  ADMIN: "ADMIN",
  TEACHER: "TEACHER",
  STUDENT: "STUDENT",
} as const

export type Role = (typeof ROLES)[keyof typeof ROLES]
