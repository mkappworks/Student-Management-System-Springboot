import { redirect } from "react-router"
import type { Route } from "./+types/logout"
import { clearAuthCookies } from "~/lib/auth.server"

export async function action(_: Route.ActionArgs) {
  const cookies = clearAuthCookies()
  return redirect("/login", {
    headers: cookies.map((c) => ["Set-Cookie", c]) as [string, string][],
  })
}

export async function loader() {
  return redirect("/login")
}
