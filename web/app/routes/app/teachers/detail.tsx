import { Link } from "react-router"
import type { Route } from "./+types/detail"
import { requireAuth } from "~/lib/auth"
import { api } from "~/lib/api"
import { PageHeader } from "~/components/layout/page-header"
import { StatusBadge } from "~/components/status-badge"
import { Button } from "~/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "~/components/ui/card"
import { Badge } from "~/components/ui/badge"
import { Pencil } from "lucide-react"
import type { TeacherResponse } from "~/types/api"

export async function clientLoader({ params }: Route.LoaderArgs) {
  const session = requireAuth()
  const teacher = await api.get<TeacherResponse>(`/api/v1/teachers/${params.id}`, session.token)
  return { teacher, role: session.role }
}

export default function TeacherDetailPage({ loaderData }: Route.ComponentProps) {
  const { teacher, role } = loaderData

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${teacher.firstName} ${teacher.lastName}`}
        description={`Employee ID: ${teacher.employeeId}`}
        action={
          role === "ADMIN" ? (
            <Button render={<Link to={`/teachers/${teacher.id}/edit`} />}>
              <Pencil className="mr-2 h-4 w-4" />
              Edit
            </Button>
          ) : undefined
        }
      />

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Personal Info</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Email</span>
              <span>{teacher.email}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Phone</span>
              <span>{teacher.phoneNumber || "—"}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Department</span>
              <span>{teacher.department}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Employment</span>
              <StatusBadge status={teacher.employmentType} />
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Hire Date</span>
              <span>{teacher.hireDate || "—"}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Status</span>
              <StatusBadge status={teacher.status} />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Assigned Modules</CardTitle>
          </CardHeader>
          <CardContent>
            {teacher.assignedModuleIds.length === 0 ? (
              <p className="text-muted-foreground text-sm">No modules assigned.</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {teacher.assignedModuleIds.map((moduleId) => (
                  <Link key={moduleId} to={`/modules/${moduleId}`}>
                    <Badge variant="outline">{moduleId}</Badge>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
