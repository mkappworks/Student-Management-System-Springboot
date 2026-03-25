import { redirect } from "react-router"
import { API_BASE_URL, COOKIE_NAME, USER_ID_COOKIE } from "./constants"
import type { AuthResponse, Session } from "~/types/api"

// In-memory storage — survives navigation but cleared on page reload.
// Restored on reload via silent refresh (see root.tsx clientLoader).
let _accessToken: string | null = null

function getToken(): string | null {
  return _accessToken
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
  _accessToken = authResponse.accessToken
  // COOKIE_NAME kept in localStorage only for legacy compat during transition;
  // the authoritative source is now _accessToken in memory.
  if (typeof localStorage !== "undefined") {
    localStorage.setItem(COOKIE_NAME, authResponse.accessToken)
    if (uid !== undefined) {
      localStorage.setItem(USER_ID_COOKIE, String(uid))
    }
  }
}

export function clearAuth(): void {
  _accessToken = null
  if (typeof localStorage !== "undefined") {
    localStorage.removeItem(COOKIE_NAME)
    localStorage.removeItem(USER_ID_COOKIE)
  }
}

export async function refreshTokens(): Promise<string | null> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: "POST",
      credentials: "include", // browser sends HttpOnly sms_refresh cookie automatically
      headers: { "Content-Type": "application/json" },
    })
    if (!res.ok) {
      clearAuth()
      return null
    }
    const body = await res.json()
    const authRes = body.data as AuthResponse
    _accessToken = authRes.accessToken
    return authRes.accessToken
  } catch {
    clearAuth()
    return null
  }
}
