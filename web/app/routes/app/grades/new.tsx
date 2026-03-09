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
import { Textarea } from "~/components/ui/textarea"
import type { GradeResponse } from "~/types/api"

export async function loader({ request }: Route.LoaderArgs) {
  requireRole(request, ["ADMIN", "TEACHER"])
  return null
}

export async function action({ request }: Route.ActionArgs) {
  const session = requireRole(request, ["ADMIN", "TEACHER"])
  const form = await request.formData()

  const payload = {
    studentId: Number(form.get("studentId")),
    moduleId: form.get("moduleId"),
    teacherId: Number(form.get("teacherId")),
    score: form.get("score"),
    maxScore: form.get("maxScore"),
    assessmentType: form.get("assessmentType"),
    remarks: form.get("remarks"),
    semester: form.get("semester"),
    academicYear: form.get("academicYear"),
  }

  try {
    await api.post<GradeResponse>("/api/v1/grades", session.token, payload)
    return redirect("/grades")
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to add grade." }, { status: 500 })
  }
}

export default function NewGradePage() {
  const actionData = useActionData<typeof action>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Add Grade" description="Record a new grade" />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Student ID" name="studentId" type="number" required />
              <FormField label="Teacher ID" name="teacherId" type="number" required />
            </div>
            <FormField label="Module ID (UUID)" name="moduleId" required />
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Score" name="score" type="number" step="0.01" required />
              <FormField label="Max Score" name="maxScore" type="number" step="0.01" required />
            </div>
            <div className="space-y-1">
              <Label htmlFor="assessmentType">Assessment Type</Label>
              <Select name="assessmentType" defaultValue="EXAM">
                <SelectTrigger id="assessmentType">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="EXAM">Exam</SelectItem>
                  <SelectItem value="ASSIGNMENT">Assignment</SelectItem>
                  <SelectItem value="QUIZ">Quiz</SelectItem>
                  <SelectItem value="PROJECT">Project</SelectItem>
                  <SelectItem value="LAB">Lab</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Semester" name="semester" required />
              <FormField label="Academic Year" name="academicYear" placeholder="2024/2025" required />
            </div>
            <div className="space-y-1">
              <Label htmlFor="remarks">Remarks</Label>
              <Textarea id="remarks" name="remarks" rows={3} />
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Saving..." : "Add Grade"}
              </Button>
              <Button variant="outline" render={<Link to="/grades" />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
