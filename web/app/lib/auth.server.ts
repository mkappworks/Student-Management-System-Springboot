import { redirect } from "react-router"
import { COOKIE_NAME, REFRESH_COOKIE_NAME, USER_ID_COOKIE } from "./constants"
import type { AuthResponse, Session } from "~/types/api"

export function getTokenFromRequest(request: Request): string | null {
  const cookieHeader = request.headers.get("Cookie") ?? ""
  const cookies = Object.fromEntries(
    cookieHeader.split(";").map((c) => {
      const [k, ...v] = c.trim().split("=")
      return [k, v.join("=")]
    }),
  )
  return cookies[COOKIE_NAME] ?? null
}

function getUserIdFromRequest(request: Request): number | undefined {
  const cookieHeader = request.headers.get("Cookie") ?? ""
  const cookies = Object.fromEntries(
    cookieHeader.split(";").map((c) => {
      const [k, ...v] = c.trim().split("=")
      return [k, v.join("=")]
    }),
  )
  const uid = cookies[USER_ID_COOKIE]
  return uid ? Number(uid) : undefined
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".")
    if (parts.length !== 3) return null
    const payload = parts[1]
    const padded = payload + "=".repeat((4 - (payload.length % 4)) % 4)
    const decoded = Buffer.from(padded, "base64").toString("utf-8")
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

export function getSessionFromRequest(request: Request): Session | null {
  const token = getTokenFromRequest(request)
  if (!token) return null

  const payload = decodeJwtPayload(token)
  if (!payload) return null

  const username = (payload.sub as string) ?? ""
  const role = (payload.role as string) ?? ""
  const userId = getUserIdFromRequest(request)

  return { token, username, role: role as Session["role"], userId }
}

export function requireAuth(request: Request): Session {
  const session = getSessionFromRequest(request)
  if (!session) throw redirect("/login")
  return session
}

export function requireRole(request: Request, roles: string[]): Session {
  const session = requireAuth(request)
  if (!roles.includes(session.role)) throw redirect("/")
  return session
}

export function createAuthCookies(authResponse: AuthResponse): string[] {
  const secure = process.env.NODE_ENV === "production" ? "; Secure" : ""
  return [
    `${COOKIE_NAME}=${authResponse.accessToken}; HttpOnly; SameSite=Lax; Path=/${secure}`,
    `${REFRESH_COOKIE_NAME}=${authResponse.refreshToken}; HttpOnly; SameSite=Lax; Path=/${secure}`,
  ]
}

export function createAuthCookiesWithUid(authResponse: AuthResponse, uid?: number): string[] {
  const cookies = createAuthCookies(authResponse)
  if (uid !== undefined) {
    const secure = process.env.NODE_ENV === "production" ? "; Secure" : ""
    cookies.push(`${USER_ID_COOKIE}=${uid}; HttpOnly; SameSite=Lax; Path=/${secure}`)
  }
  return cookies
}

export function clearAuthCookies(): string[] {
  return [
    `${COOKIE_NAME}=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0`,
    `${REFRESH_COOKIE_NAME}=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0`,
    `${USER_ID_COOKIE}=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0`,
  ]
}
