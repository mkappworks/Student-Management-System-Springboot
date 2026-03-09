import { data, redirect } from "react-router"
import { Form, Link, useActionData, useNavigation } from "react-router"
import type { Route } from "./+types/edit"
import { requireRole } from "~/lib/auth.server"
import { api, ApiError } from "~/lib/api.server"
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

export async function loader({ request, params }: Route.LoaderArgs) {
  const session = requireRole(request, ["ADMIN"])
  const module = await api.get<ModuleResponse>(`/api/v1/modules/${params.id}`, session.token)
  return { module }
}

export async function action({ request, params }: Route.ActionArgs) {
  const session = requireRole(request, ["ADMIN"])
  const form = await request.formData()

  const payload = {
    moduleCode: form.get("moduleCode"),
    moduleName: form.get("moduleName"),
    description: form.get("description"),
    credits: Number(form.get("credits")),
    semester: form.get("semester"),
    maxStudents: Number(form.get("maxStudents")),
    status: form.get("status"),
    teacherId: form.get("teacherId") ? Number(form.get("teacherId")) : undefined,
  }

  try {
    await api.put<ModuleResponse>(`/api/v1/modules/${params.id}`, session.token, payload)
    return redirect(`/modules/${params.id}`)
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to update module." }, { status: 500 })
  }
}

export default function EditModulePage({ loaderData }: Route.ComponentProps) {
  const { module } = loaderData
  const actionData = useActionData<typeof action>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Edit Module" description={`Editing ${module.moduleName}`} />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Module Code" name="moduleCode" defaultValue={module.moduleCode} required />
              <FormField label="Credits" name="credits" type="number" defaultValue={String(module.credits)} required />
            </div>
            <FormField label="Module Name" name="moduleName" defaultValue={module.moduleName} required />
            <FormField label="Description" name="description" defaultValue={module.description} />
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Semester" name="semester" defaultValue={module.semester} required />
              <FormField label="Max Students" name="maxStudents" type="number" defaultValue={String(module.maxStudents)} required />
            </div>
            <FormField label="Teacher ID" name="teacherId" type="number" defaultValue={module.teacherId ? String(module.teacherId) : ""} />
            <div className="space-y-1">
              <Label htmlFor="status">Status</Label>
              <Select name="status" defaultValue={module.status}>
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
                {isSubmitting ? "Saving..." : "Save Changes"}
              </Button>
              <Button variant="outline" render={<Link to={`/modules/${module.id}`} />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
