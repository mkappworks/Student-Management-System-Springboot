import { data, redirect } from "react-router"
import { Form, Link, useActionData, useNavigation } from "react-router"
import type { Route } from "./+types/edit"
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
import type { StudentResponse } from "~/types/api"

export async function clientLoader({ params }: Route.LoaderArgs) {
  const session = requireRole(["ADMIN"])
  const student = await api.get<StudentResponse>(`/api/v1/students/${params.id}`, session.token)
  return { student }
}

export async function clientAction({ request, params }: Route.ActionArgs) {
  const session = requireRole(["ADMIN"])
  const form = await request.formData()

  const payload = {
    firstName: form.get("firstName"),
    lastName: form.get("lastName"),
    email: form.get("email"),
    phoneNumber: form.get("phoneNumber"),
    dateOfBirth: form.get("dateOfBirth"),
    enrollmentDate: form.get("enrollmentDate"),
    status: form.get("status"),
  }

  try {
    await api.put<StudentResponse>(`/api/v1/students/${params.id}`, session.token, payload)
    return redirect(`/students/${params.id}`)
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to update student." }, { status: 500 })
  }
}

export default function EditStudentPage({ loaderData }: Route.ComponentProps) {
  const { student } = loaderData
  const actionData = useActionData<typeof clientAction>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Edit Student" description={`Editing ${student.firstName} ${student.lastName}`} />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="First Name" name="firstName" defaultValue={student.firstName} required />
              <FormField label="Last Name" name="lastName" defaultValue={student.lastName} required />
            </div>
            <FormField label="Email" name="email" type="email" defaultValue={student.email} required />
            <FormField label="Phone Number" name="phoneNumber" type="tel" defaultValue={student.phoneNumber} />
            <FormField label="Date of Birth" name="dateOfBirth" type="date" defaultValue={student.dateOfBirth} />
            <FormField label="Enrollment Date" name="enrollmentDate" type="date" defaultValue={student.enrollmentDate} />
            <div className="space-y-1">
              <Label htmlFor="status">Status</Label>
              <Select name="status" defaultValue={student.status}>
                <SelectTrigger id="status">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">Active</SelectItem>
                  <SelectItem value="INACTIVE">Inactive</SelectItem>
                  <SelectItem value="GRADUATED">Graduated</SelectItem>
                  <SelectItem value="SUSPENDED">Suspended</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Saving..." : "Save Changes"}
              </Button>
              <Button variant="outline" render={<Link to={`/students/${student.id}`} />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
