import { data, redirect } from "react-router"
import { Form, Link, useActionData, useNavigation } from "react-router"
import type { Route } from "./+types/new"
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
import type { StudentResponse } from "~/types/api"

export async function loader({ request }: Route.LoaderArgs) {
  requireRole(request, ["ADMIN"])
  return null
}

export async function action({ request }: Route.ActionArgs) {
  const session = requireRole(request, ["ADMIN"])
  const form = await request.formData()

  const payload = {
    firstName: form.get("firstName"),
    lastName: form.get("lastName"),
    email: form.get("email"),
    phoneNumber: form.get("phoneNumber"),
    dateOfBirth: form.get("dateOfBirth"),
    enrollmentDate: form.get("enrollmentDate"),
    status: form.get("status") ?? "ACTIVE",
  }

  try {
    const student = await api.post<StudentResponse>("/api/v1/students", session.token, payload)
    return redirect(`/students/${student.id}`)
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to create student." }, { status: 500 })
  }
}

export default function NewStudentPage() {
  const actionData = useActionData<typeof action>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Add Student" description="Create a new student record" />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="First Name" name="firstName" required />
              <FormField label="Last Name" name="lastName" required />
            </div>
            <FormField label="Email" name="email" type="email" required />
            <FormField label="Phone Number" name="phoneNumber" type="tel" />
            <FormField label="Date of Birth" name="dateOfBirth" type="date" />
            <FormField label="Enrollment Date" name="enrollmentDate" type="date" />
            <div className="space-y-1">
              <Label htmlFor="status">Status</Label>
              <Select name="status" defaultValue="ACTIVE">
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
                {isSubmitting ? "Creating..." : "Create Student"}
              </Button>
              <Button variant="outline" render={<Link to="/students" />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
