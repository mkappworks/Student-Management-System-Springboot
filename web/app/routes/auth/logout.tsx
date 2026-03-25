import { redirect } from "react-router"
import type { Route } from "./+types/logout"
import { clearAuth } from "~/lib/auth"
import { API_BASE_URL } from "~/lib/constants"

export async function clientAction(_: Route.ActionArgs) {
  await fetch(`${API_BASE_URL}/api/v1/auth/logout`, {
    method: "POST",
    credentials: "include",
  })
  clearAuth()
  throw redirect("/login")
}

export async function clientLoader() {
  return redirect("/login")
}
