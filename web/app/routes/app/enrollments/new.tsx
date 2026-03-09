import { data, redirect } from "react-router"
import { Form, Link, useActionData, useNavigation } from "react-router"
import type { Route } from "./+types/new"
import { requireAuth } from "~/lib/auth"
import { api, ApiError } from "~/lib/api"
import { PageHeader } from "~/components/layout/page-header"
import { FormField } from "~/components/forms/form-field"
import { Button } from "~/components/ui/button"
import { Card, CardContent } from "~/components/ui/card"
import { Label } from "~/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "~/components/ui/select"
import type { EnrollmentResponse, ModuleResponse, Page } from "~/types/api"

export async function clientLoader() {
  const session = requireAuth()
  const modules = await api.get<Page<ModuleResponse>>("/api/v1/modules?size=100", session.token)
  return { modules: modules.content, userId: session.userId, role: session.role }
}

export async function clientAction({ request }: Route.ActionArgs) {
  const session = requireAuth()
  const form = await request.formData()

  const studentId =
    session.role === "STUDENT"
      ? session.userId
      : Number(form.get("studentId"))
  const moduleId = form.get("moduleId")

  try {
    await api.post<EnrollmentResponse>("/api/v1/enrollments", session.token, {
      studentId,
      moduleId,
    })
    return redirect("/enrollments")
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to create enrollment." }, { status: 500 })
  }
}

export default function NewEnrollmentPage({ loaderData }: Route.ComponentProps) {
  const { modules, userId, role } = loaderData
  const actionData = useActionData<typeof clientAction>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="New Enrollment" description="Enroll a student in a module" />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            {role !== "STUDENT" && (
              <FormField label="Student ID" name="studentId" type="number" required />
            )}
            {role === "STUDENT" && userId && (
              <div className="text-muted-foreground text-sm">
                Enrolling as student ID: <strong>{userId}</strong>
              </div>
            )}
            <div className="space-y-1">
              <Label htmlFor="moduleId">Module</Label>
              <Select name="moduleId" required>
                <SelectTrigger id="moduleId">
                  <SelectValue placeholder="Select a module" />
                </SelectTrigger>
                <SelectContent>
                  {modules.map((m) => (
                    <SelectItem key={m.id} value={m.id}>
                      {m.moduleCode} — {m.moduleName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Enrolling..." : "Enroll"}
              </Button>
              <Button variant="outline" render={<Link to="/enrollments" />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
