import { data, redirect } from "react-router"
import { Form, Link, useActionData, useNavigation } from "react-router"
import type { Route } from "./+types/register"
import { createAuthCookiesWithUid } from "~/lib/auth.server"
import { api, ApiError } from "~/lib/api.server"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "~/components/ui/card"
import { Button } from "~/components/ui/button"
import { FormField } from "~/components/forms/form-field"
import { Label } from "~/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "~/components/ui/select"
import type { AuthResponse } from "~/types/api"

export async function action({ request }: Route.ActionArgs) {
  const form = await request.formData()
  const username = form.get("username") as string
  const email = form.get("email") as string
  const password = form.get("password") as string
  const role = form.get("role") as string

  try {
    const authResponse = await api.post<AuthResponse>("/api/v1/auth/register", null, {
      username,
      email,
      password,
      role,
    })

    const cookies = createAuthCookiesWithUid(authResponse)
    return redirect("/", {
      headers: cookies.map((c) => ["Set-Cookie", c]) as [string, string][],
    })
  } catch (err) {
    if (err instanceof ApiError) {
      return data({ error: err.message }, { status: err.status })
    }
    return data({ error: "Registration failed. Please try again." }, { status: 500 })
  }
}

export default function RegisterPage() {
  const actionData = useActionData<typeof action>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-2xl">Create Account</CardTitle>
          <CardDescription>Register for access to the student portal</CardDescription>
        </CardHeader>
        <CardContent>
          <Form method="post" className="space-y-4">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <FormField label="Username" name="username" type="text" required autoFocus />
            <FormField label="Email" name="email" type="email" required />
            <FormField label="Password" name="password" type="password" required />
            <div className="space-y-1">
              <Label htmlFor="role">Role</Label>
              <Select name="role" defaultValue="STUDENT">
                <SelectTrigger id="role">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ADMIN">Admin</SelectItem>
                  <SelectItem value="TEACHER">Teacher</SelectItem>
                  <SelectItem value="STUDENT">Student</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Creating account..." : "Create Account"}
            </Button>
          </Form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Already have an account?{" "}
            <Link to="/login" className="underline">
              Sign in
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
