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
import type { TeacherResponse } from "~/types/api"

export async function loader({ request, params }: Route.LoaderArgs) {
  const session = requireRole(request, ["ADMIN"])
  const teacher = await api.get<TeacherResponse>(`/api/v1/teachers/${params.id}`, session.token)
  return { teacher }
}

export async function action({ request, params }: Route.ActionArgs) {
  const session = requireRole(request, ["ADMIN"])
  const form = await request.formData()

  const payload = {
    firstName: form.get("firstName"),
    lastName: form.get("lastName"),
    email: form.get("email"),
    phoneNumber: form.get("phoneNumber"),
    department: form.get("department"),
    employmentType: form.get("employmentType"),
    status: form.get("status"),
    hireDate: form.get("hireDate"),
  }

  try {
    await api.put<TeacherResponse>(`/api/v1/teachers/${params.id}`, session.token, payload)
    return redirect(`/teachers/${params.id}`)
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to update teacher." }, { status: 500 })
  }
}

export default function EditTeacherPage({ loaderData }: Route.ComponentProps) {
  const { teacher } = loaderData
  const actionData = useActionData<typeof action>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Edit Teacher" description={`Editing ${teacher.firstName} ${teacher.lastName}`} />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="First Name" name="firstName" defaultValue={teacher.firstName} required />
              <FormField label="Last Name" name="lastName" defaultValue={teacher.lastName} required />
            </div>
            <FormField label="Email" name="email" type="email" defaultValue={teacher.email} required />
            <FormField label="Phone Number" name="phoneNumber" type="tel" defaultValue={teacher.phoneNumber} />
            <FormField label="Department" name="department" defaultValue={teacher.department} required />
            <FormField label="Hire Date" name="hireDate" type="date" defaultValue={teacher.hireDate} />
            <div className="space-y-1">
              <Label htmlFor="employmentType">Employment Type</Label>
              <Select name="employmentType" defaultValue={teacher.employmentType}>
                <SelectTrigger id="employmentType">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="FULL_TIME">Full Time</SelectItem>
                  <SelectItem value="PART_TIME">Part Time</SelectItem>
                  <SelectItem value="CONTRACT">Contract</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="status">Status</Label>
              <Select name="status" defaultValue={teacher.status}>
                <SelectTrigger id="status">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">Active</SelectItem>
                  <SelectItem value="INACTIVE">Inactive</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Saving..." : "Save Changes"}
              </Button>
              <Button variant="outline" render={<Link to={`/teachers/${teacher.id}`} />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
