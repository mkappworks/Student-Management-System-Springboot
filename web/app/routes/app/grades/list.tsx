import { redirect } from "react-router"
import { Link, useSearchParams, Form } from "react-router"
import type { Route } from "./+types/list"
import { requireAuth } from "~/lib/auth.server"
import { api, ApiError } from "~/lib/api.server"
import { DataTable } from "~/components/data-table/data-table"
import { PageHeader } from "~/components/layout/page-header"
import { Button } from "~/components/ui/button"
import { Input } from "~/components/ui/input"
import { Label } from "~/components/ui/label"
import { ConfirmDialog } from "~/components/confirm-dialog"
import { Plus, Pencil, Trash2 } from "lucide-react"
import type { GradeResponse } from "~/types/api"

export async function loader({ request }: Route.LoaderArgs) {
  const session = requireAuth(request)
  const url = new URL(request.url)
  const studentId = session.role === "STUDENT" ? session.userId : url.searchParams.get("studentId")

  if (!studentId) {
    return { grades: null, role: session.role, studentId: null }
  }

  try {
    const grades = await api.get<GradeResponse[]>(
      `/api/v1/grades/student/${studentId}`,
      session.token,
    )
    return { grades, role: session.role, studentId }
  } catch (err) {
    if (err instanceof ApiError) return { grades: null, role: session.role, studentId, error: err.message }
    throw err
  }
}

export async function action({ request }: Route.ActionArgs) {
  const session = requireAuth(request)
  const form = await request.formData()
  const actionName = form.get("_action")
  const id = form.get("id")

  if (actionName === "delete") {
    await api.delete(`/api/v1/grades/${id}`, session.token)
  }
  return redirect("/grades")
}

export default function GradesListPage({ loaderData }: Route.ComponentProps) {
  const { grades, role, studentId } = loaderData
  const [searchParams] = useSearchParams()

  const columns = [
    { header: "Module", accessorKey: "moduleId" as const },
    { header: "Assessment", accessorKey: "assessmentType" as const },
    {
      header: "Score",
      cell: (r: GradeResponse) => `${r.score}/${r.maxScore} (${r.percentage}%)`,
    },
    { header: "Grade", accessorKey: "grade" as const },
    { header: "Semester", accessorKey: "semester" as const },
    { header: "Year", accessorKey: "academicYear" as const },
    ...(role !== "STUDENT"
      ? [
          {
            header: "Actions",
            cell: (r: GradeResponse) => (
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="sm" render={<Link to={`/grades/${r.id}/edit`} />}>
                  <Pencil className="h-4 w-4" />
                </Button>
                <ConfirmDialog
                  trigger={
                    <Button variant="ghost" size="sm">
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  }
                  title="Delete Grade"
                  description="Are you sure you want to delete this grade record?"
                  actionName="delete"
                  value={String(r.id)}
                />
              </div>
            ),
          },
        ]
      : []),
  ]

  return (
    <div className="space-y-4">
      <PageHeader
        title="Grades"
        description="View grade records"
        action={
          role !== "STUDENT" ? (
            <Button render={<Link to="/grades/new" />}>
              <Plus className="mr-2 h-4 w-4" />
              Add Grade
            </Button>
          ) : undefined
        }
      />

      {role !== "STUDENT" && !studentId && (
        <Form method="get" className="flex gap-2 max-w-sm">
          <div className="flex-1 space-y-1">
            <Label htmlFor="studentId">Student ID</Label>
            <Input id="studentId" name="studentId" type="number" placeholder="Enter student ID" />
          </div>
          <Button type="submit" className="self-end">
            Load
          </Button>
        </Form>
      )}

      {studentId && grades ? (
        <DataTable columns={columns} data={grades} emptyMessage="No grades found for this student." />
      ) : studentId ? (
        <p className="text-muted-foreground text-sm">Failed to load grades.</p>
      ) : (
        <p className="text-muted-foreground text-sm">Enter a student ID to view grades.</p>
      )}
    </div>
  )
}
