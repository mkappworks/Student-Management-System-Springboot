import { redirect } from "react-router"
import { API_BASE_URL, COOKIE_NAME, REFRESH_COOKIE_NAME, USER_ID_COOKIE } from "./constants"
import type { AuthResponse, Session } from "~/types/api"

function getToken(): string | null {
  if (typeof localStorage === "undefined") return null
  return localStorage.getItem(COOKIE_NAME)
}

function getUserId(): number | undefined {
  if (typeof localStorage === "undefined") return undefined
  const uid = localStorage.getItem(USER_ID_COOKIE)
  return uid ? Number(uid) : undefined
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".")
    if (parts.length !== 3) return null
    const payload = parts[1]
    const padded = payload + "=".repeat((4 - (payload.length % 4)) % 4)
    const decoded = atob(padded)
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

export function getSession(): Session | null {
  const token = getToken()
  if (!token) return null

  const payload = decodeJwtPayload(token)
  if (!payload) return null

  const username = (payload.sub as string) ?? ""
  const role = (payload.role as string) ?? ""
  const userId = getUserId()

  return { token, username, role: role as Session["role"], userId }
}

export function requireAuth(): Session {
  const session = getSession()
  if (!session) throw redirect("/login")
  return session
}

export function requireRole(roles: string[]): Session {
  const session = requireAuth()
  if (!roles.includes(session.role)) throw redirect("/")
  return session
}

export function saveAuth(authResponse: AuthResponse, uid?: number): void {
  if (typeof localStorage === "undefined") return
  localStorage.setItem(COOKIE_NAME, authResponse.accessToken)
  localStorage.setItem(REFRESH_COOKIE_NAME, authResponse.refreshToken)
  if (uid !== undefined) {
    localStorage.setItem(USER_ID_COOKIE, String(uid))
  }
}

export function clearAuth(): void {
  if (typeof localStorage === "undefined") return
  localStorage.removeItem(COOKIE_NAME)
  localStorage.removeItem(REFRESH_COOKIE_NAME)
  localStorage.removeItem(USER_ID_COOKIE)
}

export async function refreshTokens(): Promise<string | null> {
  const refreshToken = localStorage.getItem(REFRESH_COOKIE_NAME)
  if (!refreshToken) return null

  try {
    const res = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) {
      clearAuth()
      return null
    }
    const body = await res.json()
    const authRes = body.data as AuthResponse
    saveAuth(authRes)
    return authRes.accessToken
  } catch {
    clearAuth()
    return null
  }
}
