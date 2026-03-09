import { redirect } from "react-router"
import { Link } from "react-router"
import type { Route } from "./+types/list"
import { requireRole } from "~/lib/auth.server"
import { api, ApiError } from "~/lib/api.server"
import { DataTable } from "~/components/data-table/data-table"
import { DataTablePagination } from "~/components/data-table/data-table-pagination"
import { DataTableToolbar } from "~/components/data-table/data-table-toolbar"
import { PageHeader } from "~/components/layout/page-header"
import { StatusBadge } from "~/components/status-badge"
import { Button } from "~/components/ui/button"
import { ConfirmDialog } from "~/components/confirm-dialog"
import { Plus, Eye, Pencil, Trash2 } from "lucide-react"
import type { StudentResponse, Page } from "~/types/api"

export async function loader({ request }: Route.LoaderArgs) {
  const session = requireRole(request, ["ADMIN", "TEACHER"])
  const url = new URL(request.url)
  const page = Number(url.searchParams.get("page") ?? "0")
  const q = url.searchParams.get("q") ?? ""

  const endpoint = q
    ? `/api/v1/students/search?query=${encodeURIComponent(q)}&page=${page}&size=20`
    : `/api/v1/students?page=${page}&size=20`

  try {
    const data = await api.get<Page<StudentResponse>>(endpoint, session.token)
    return { data, role: session.role, q }
  } catch (err) {
    if (err instanceof ApiError) return { data: null, role: session.role, q, error: err.message }
    throw err
  }
}

export async function action({ request }: Route.ActionArgs) {
  const session = requireRole(request, ["ADMIN"])
  const form = await request.formData()
  const action = form.get("_action")
  const id = form.get("id")

  if (action === "delete") {
    await api.delete(`/api/v1/students/${id}`, session.token)
  }
  return redirect("/students")
}

export default function StudentsListPage({ loaderData }: Route.ComponentProps) {
  const { data, role } = loaderData

  const columns = [
    { header: "Student ID", accessorKey: "studentId" as const },
    {
      header: "Name",
      cell: (row: StudentResponse) => `${row.firstName} ${row.lastName}`,
    },
    { header: "Email", accessorKey: "email" as const },
    { header: "Status", cell: (row: StudentResponse) => <StatusBadge status={row.status} /> },
    {
      header: "Actions",
      cell: (row: StudentResponse) => (
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" render={<Link to={`/students/${row.id}`} />}>
            <Eye className="h-4 w-4" />
          </Button>
          {role === "ADMIN" && (
            <>
              <Button variant="ghost" size="sm" render={<Link to={`/students/${row.id}/edit`} />}>
                <Pencil className="h-4 w-4" />
              </Button>
              <ConfirmDialog
                trigger={
                  <Button variant="ghost" size="sm">
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                }
                title="Delete Student"
                description={`Are you sure you want to delete ${row.firstName} ${row.lastName}?`}
                actionName="delete"
                value={String(row.id)}
              />
            </>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-4">
      <PageHeader
        title="Students"
        description="Manage student records"
        action={
          role === "ADMIN" ? (
            <Button render={<Link to="/students/new" />}>
              <Plus className="mr-2 h-4 w-4" />
              Add Student
            </Button>
          ) : undefined
        }
      />
      <DataTableToolbar placeholder="Search students..." />
      <DataTable columns={columns} data={data?.content ?? []} emptyMessage="No students found." />
      {data && data.totalPages > 1 && (
        <DataTablePagination
          currentPage={data.number}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
        />
      )}
    </div>
  )
}
