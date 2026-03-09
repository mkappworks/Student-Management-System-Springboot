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
import { Textarea } from "~/components/ui/textarea"
import type { GradeResponse } from "~/types/api"

export async function clientLoader({ params }: Route.LoaderArgs) {
  const session = requireRole(["ADMIN", "TEACHER"])
  const grade = await api.get<GradeResponse>(`/api/v1/grades/${params.id}`, session.token)
  return { grade }
}

export async function clientAction({ request, params }: Route.ActionArgs) {
  const session = requireRole(["ADMIN", "TEACHER"])
  const form = await request.formData()

  const payload = {
    score: form.get("score"),
    maxScore: form.get("maxScore"),
    assessmentType: form.get("assessmentType"),
    remarks: form.get("remarks"),
    semester: form.get("semester"),
    academicYear: form.get("academicYear"),
  }

  try {
    await api.put<GradeResponse>(`/api/v1/grades/${params.id}`, session.token, payload)
    return redirect("/grades")
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to update grade." }, { status: 500 })
  }
}

export default function EditGradePage({ loaderData }: Route.ComponentProps) {
  const { grade } = loaderData
  const actionData = useActionData<typeof clientAction>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Edit Grade" description="Update grade record" />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Score" name="score" type="number" step="0.01" defaultValue={grade.score} required />
              <FormField label="Max Score" name="maxScore" type="number" step="0.01" defaultValue={grade.maxScore} required />
            </div>
            <div className="space-y-1">
              <Label htmlFor="assessmentType">Assessment Type</Label>
              <Select name="assessmentType" defaultValue={grade.assessmentType}>
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
              <FormField label="Semester" name="semester" defaultValue={grade.semester} required />
              <FormField label="Academic Year" name="academicYear" defaultValue={grade.academicYear} required />
            </div>
            <div className="space-y-1">
              <Label htmlFor="remarks">Remarks</Label>
              <Textarea id="remarks" name="remarks" defaultValue={grade.remarks} rows={3} />
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Saving..." : "Save Changes"}
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
