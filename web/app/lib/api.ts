import { redirect } from "react-router"
import { API_BASE_URL } from "./constants"
import type { ApiResponse } from "~/types/api"
import { clearAuth, refreshTokens } from "~/lib/auth"

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message)
    this.name = "ApiError"
  }
}

async function apiFetch<T>(
  path: string,
  token: string | null,
  options: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  }
  if (token) headers["Authorization"] = `Bearer ${token}`

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (response.status === 401 && token) {
    const newToken = await refreshTokens()
    if (newToken) {
      const retry = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: { ...headers, Authorization: `Bearer ${newToken}` },
      })
      if (retry.ok) {
        if (retry.status === 204) return undefined as T
        const retryBody = (await retry.json()) as ApiResponse<T>
        if (!retryBody.success) throw new ApiError(retry.status, retryBody.message)
        return retryBody.data
      }
    }
    clearAuth()
    throw redirect("/login")
  }

  if (!response.ok) {
    let message = `HTTP ${response.status}`
    try {
      const body = (await response.json()) as ApiResponse<unknown>
      message = body.message ?? message
    } catch {
      // ignore parse error
    }
    throw new ApiError(response.status, message)
  }

  // Handle 204 No Content
  if (response.status === 204) return undefined as T

  const body = (await response.json()) as ApiResponse<T>
  if (!body.success) throw new ApiError(response.status, body.message)
  return body.data
}

export const api = {
  get: <T>(path: string, token: string | null) => apiFetch<T>(path, token),
  post: <T>(path: string, token: string | null, body: unknown) =>
    apiFetch<T>(path, token, { method: "POST", body: JSON.stringify(body) }),
  put: <T>(path: string, token: string | null, body: unknown) =>
    apiFetch<T>(path, token, { method: "PUT", body: JSON.stringify(body) }),
  patch: <T>(path: string, token: string | null, body: unknown) =>
    apiFetch<T>(path, token, { method: "PATCH", body: JSON.stringify(body) }),
  delete: (path: string, token: string | null) =>
    apiFetch<void>(path, token, { method: "DELETE" }),
}
