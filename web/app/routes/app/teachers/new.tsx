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
import type { TeacherResponse } from "~/types/api"

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
    department: form.get("department"),
    employmentType: form.get("employmentType") ?? "FULL_TIME",
    hireDate: form.get("hireDate"),
  }

  try {
    const teacher = await api.post<TeacherResponse>("/api/v1/teachers", session.token, payload)
    return redirect(`/teachers/${teacher.id}`)
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to create teacher." }, { status: 500 })
  }
}

export default function NewTeacherPage() {
  const actionData = useActionData<typeof action>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Add Teacher" description="Create a new teacher record" />
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
            <FormField label="Department" name="department" required />
            <FormField label="Hire Date" name="hireDate" type="date" />
            <div className="space-y-1">
              <Label htmlFor="employmentType">Employment Type</Label>
              <Select name="employmentType" defaultValue="FULL_TIME">
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
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Creating..." : "Create Teacher"}
              </Button>
              <Button variant="outline" render={<Link to="/teachers" />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
