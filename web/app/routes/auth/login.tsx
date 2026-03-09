import { data, redirect } from "react-router"
import { Form, Link, useActionData, useNavigation } from "react-router"
import type { Route } from "./+types/login"
import { getSession, saveAuth } from "~/lib/auth"
import { api, ApiError } from "~/lib/api"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "~/components/ui/card"
import { Button } from "~/components/ui/button"
import { FormField } from "~/components/forms/form-field"
import type { AuthResponse, Page, StudentResponse } from "~/types/api"

export async function clientLoader() {
  const session = getSession()
  if (session) throw redirect("/")
  return null
}

export async function clientAction({ request }: Route.ActionArgs) {
  const form = await request.formData()
  const username = form.get("username") as string
  const password = form.get("password") as string

  try {
    const authResponse = await api.post<AuthResponse>(
      "/api/v1/auth/login",
      null,
      { username, password },
    )

    let uid: number | undefined
    if (authResponse.role === "STUDENT" || authResponse.role === "TEACHER") {
      try {
        const page = await api.get<Page<StudentResponse>>(
          `/api/v1/students/search?query=${encodeURIComponent(username)}&size=1`,
          authResponse.accessToken,
        )
        if (page.content.length > 0) uid = page.content[0].id
      } catch {
        // non-critical
      }
    }

    saveAuth(authResponse, uid)
    return redirect("/")
  } catch (err) {
    if (err instanceof ApiError) {
      return data({ error: err.message }, { status: err.status })
    }
    return data({ error: "Login failed. Please try again." }, { status: 500 })
  }
}

export default function LoginPage() {
  const actionData = useActionData<typeof clientAction>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-2xl">Sign In</CardTitle>
          <CardDescription>Enter your credentials to access the portal</CardDescription>
        </CardHeader>
        <CardContent>
          <Form method="post" className="space-y-4">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <FormField label="Username" name="username" type="text" required autoFocus />
            <FormField label="Password" name="password" type="password" required />
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Signing in..." : "Sign In"}
            </Button>
          </Form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Don&apos;t have an account?{" "}
            <Link to="/register" className="underline">
              Register
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
