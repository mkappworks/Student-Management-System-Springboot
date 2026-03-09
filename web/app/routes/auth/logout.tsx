import { redirect } from "react-router"
import type { Route } from "./+types/logout"
import { clearAuth } from "~/lib/auth"

export async function clientAction(_: Route.ActionArgs) {
  clearAuth()
  return redirect("/login")
}

export async function clientLoader() {
  return redirect("/login")
}
