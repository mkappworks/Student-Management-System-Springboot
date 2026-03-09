import { redirect } from "react-router"
import { Link } from "react-router"
import type { Route } from "./+types/list"
import { requireAuth } from "~/lib/auth"
import { api, ApiError } from "~/lib/api"
import { DataTable } from "~/components/data-table/data-table"
import { DataTablePagination } from "~/components/data-table/data-table-pagination"
import { DataTableToolbar } from "~/components/data-table/data-table-toolbar"
import { PageHeader } from "~/components/layout/page-header"
import { StatusBadge } from "~/components/status-badge"
import { Button } from "~/components/ui/button"
import { ConfirmDialog } from "~/components/confirm-dialog"
import { Plus, Eye, Pencil, Trash2 } from "lucide-react"
import type { ModuleResponse, Page } from "~/types/api"

export async function clientLoader({ request }: Route.LoaderArgs) {
  const session = requireAuth()
  const url = new URL(request.url)
  const page = Number(url.searchParams.get("page") ?? "0")
  const q = url.searchParams.get("q") ?? ""

  const endpoint = q
    ? `/api/v1/modules/search?query=${encodeURIComponent(q)}&page=${page}&size=20`
    : `/api/v1/modules?page=${page}&size=20`

  try {
    const data = await api.get<Page<ModuleResponse>>(endpoint, session.token)
    return { data, role: session.role }
  } catch (err) {
    if (err instanceof ApiError) return { data: null, role: session.role, error: err.message }
    throw err
  }
}

export async function clientAction({ request }: Route.ActionArgs) {
  const session = requireAuth()
  if (session.role !== "ADMIN") return redirect("/modules")
  const form = await request.formData()
  const actionName = form.get("_action")
  const id = form.get("id")

  if (actionName === "delete") {
    await api.delete(`/api/v1/modules/${id}`, session.token)
  }
  return redirect("/modules")
}

export default function ModulesListPage({ loaderData }: Route.ComponentProps) {
  const { data, role } = loaderData

  const columns = [
    { header: "Code", accessorKey: "moduleCode" as const },
    { header: "Name", accessorKey: "moduleName" as const },
    { header: "Credits", accessorKey: "credits" as const },
    { header: "Semester", accessorKey: "semester" as const },
    {
      header: "Enrollment",
      cell: (row: ModuleResponse) => `${row.currentEnrollment}/${row.maxStudents}`,
    },
    { header: "Status", cell: (row: ModuleResponse) => <StatusBadge status={row.status} /> },
    {
      header: "Actions",
      cell: (row: ModuleResponse) => (
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" render={<Link to={`/modules/${row.id}`} />}>
            <Eye className="h-4 w-4" />
          </Button>
          {role === "ADMIN" && (
            <>
              <Button variant="ghost" size="sm" render={<Link to={`/modules/${row.id}/edit`} />}>
                <Pencil className="h-4 w-4" />
              </Button>
              <ConfirmDialog
                trigger={
                  <Button variant="ghost" size="sm">
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                }
                title="Delete Module"
                description={`Are you sure you want to delete ${row.moduleName}?`}
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
        title="Modules"
        description="Browse available modules"
        action={
          role === "ADMIN" ? (
            <Button render={<Link to="/modules/new" />}>
              <Plus className="mr-2 h-4 w-4" />
              Add Module
            </Button>
          ) : undefined
        }
      />
      <DataTableToolbar placeholder="Search modules..." />
      <DataTable columns={columns} data={data?.content ?? []} emptyMessage="No modules found." />
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
