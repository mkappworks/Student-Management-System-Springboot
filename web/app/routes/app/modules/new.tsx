import { data, redirect } from "react-router"
import { Form, Link, useActionData, useNavigation } from "react-router"
import type { Route } from "./+types/new"
import { requireRole } from "~/lib/auth"
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
import type { ModuleResponse } from "~/types/api"

export async function clientLoader() {
  requireRole(["ADMIN"])
  return null
}

export async function clientAction({ request }: Route.ActionArgs) {
  const session = requireRole(["ADMIN"])
  const form = await request.formData()

  const payload = {
    moduleCode: form.get("moduleCode"),
    moduleName: form.get("moduleName"),
    description: form.get("description"),
    credits: Number(form.get("credits")),
    semester: form.get("semester"),
    maxStudents: Number(form.get("maxStudents")),
    status: form.get("status") ?? "ACTIVE",
    teacherId: form.get("teacherId") ? Number(form.get("teacherId")) : undefined,
  }

  try {
    const module = await api.post<ModuleResponse>("/api/v1/modules", session.token, payload)
    return redirect(`/modules/${module.id}`)
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to create module." }, { status: 500 })
  }
}

export default function NewModulePage() {
  const actionData = useActionData<typeof clientAction>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Add Module" description="Create a new module" />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Module Code" name="moduleCode" required />
              <FormField label="Credits" name="credits" type="number" min="1" required />
            </div>
            <FormField label="Module Name" name="moduleName" required />
            <FormField label="Description" name="description" />
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Semester" name="semester" required />
              <FormField label="Max Students" name="maxStudents" type="number" min="1" required />
            </div>
            <FormField label="Teacher ID" name="teacherId" type="number" />
            <div className="space-y-1">
              <Label htmlFor="status">Status</Label>
              <Select name="status" defaultValue="ACTIVE">
                <SelectTrigger id="status">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">Active</SelectItem>
                  <SelectItem value="INACTIVE">Inactive</SelectItem>
                  <SelectItem value="COMPLETED">Completed</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Creating..." : "Create Module"}
              </Button>
              <Button variant="outline" render={<Link to="/modules" />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
