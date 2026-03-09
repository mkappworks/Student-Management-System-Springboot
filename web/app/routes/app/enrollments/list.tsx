import { redirect } from "react-router"
import { Link } from "react-router"
import type { Route } from "./+types/list"
import { requireAuth } from "~/lib/auth"
import { api, ApiError } from "~/lib/api"
import { DataTable } from "~/components/data-table/data-table"
import { PageHeader } from "~/components/layout/page-header"
import { StatusBadge } from "~/components/status-badge"
import { Button } from "~/components/ui/button"
import { ConfirmDialog } from "~/components/confirm-dialog"
import { Plus, Trash2 } from "lucide-react"
import type { EnrollmentResponse } from "~/types/api"

export async function clientLoader({ request }: Route.LoaderArgs) {
  const session = requireAuth()
  const url = new URL(request.url)
  const studentId = session.role === "STUDENT" ? session.userId : url.searchParams.get("studentId")

  if (!studentId) {
    return { enrollments: [], role: session.role, userId: session.userId }
  }

  try {
    const enrollments = await api.get<EnrollmentResponse[]>(
      `/api/v1/enrollments/student/${studentId}`,
      session.token,
    )
    return { enrollments, role: session.role, userId: session.userId }
  } catch (err) {
    if (err instanceof ApiError)
      return { enrollments: [], role: session.role, userId: session.userId, error: err.message }
    throw err
  }
}

export async function clientAction({ request }: Route.ActionArgs) {
  const session = requireAuth()
  const form = await request.formData()
  const actionName = form.get("_action")
  const id = form.get("id")
  const reason = form.get("reason") as string | null

  if (actionName === "drop") {
    await api.patch(`/api/v1/enrollments/${id}/drop`, session.token, { reason: reason ?? "" })
  }
  return redirect("/enrollments")
}

export default function EnrollmentsListPage({ loaderData }: Route.ComponentProps) {
  const { enrollments, role } = loaderData

  const columns = [
    { header: "Module ID", accessorKey: "moduleId" as const },
    { header: "Enrolled", accessorKey: "enrollmentDate" as const },
    {
      header: "Status",
      cell: (r: EnrollmentResponse) => <StatusBadge status={r.status} />,
    },
    { header: "Drop Date", cell: (r: EnrollmentResponse) => r.dropDate ?? "—" },
    ...(role === "ADMIN" || role === "STUDENT"
      ? [
          {
            header: "Actions",
            cell: (r: EnrollmentResponse) =>
              r.status === "ENROLLED" ? (
                <ConfirmDialog
                  trigger={
                    <Button variant="ghost" size="sm">
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  }
                  title="Drop Enrollment"
                  description="Are you sure you want to drop this module enrollment?"
                  actionName="drop"
                  value={String(r.id)}
                />
              ) : null,
          },
        ]
      : []),
  ]

  return (
    <div className="space-y-4">
      <PageHeader
        title="Enrollments"
        description="View module enrollments"
        action={
          <Button render={<Link to="/enrollments/new" />}>
            <Plus className="mr-2 h-4 w-4" />
            New Enrollment
          </Button>
        }
      />
      <DataTable
        columns={columns}
        data={enrollments}
        emptyMessage="No enrollments found."
      />
    </div>
  )
}
