import { redirect } from "react-router"
import { Link, Form } from "react-router"
import type { Route } from "./+types/detail"
import { requireAuth } from "~/lib/auth"
import { api } from "~/lib/api"
import { PageHeader } from "~/components/layout/page-header"
import { StatusBadge } from "~/components/status-badge"
import { Button } from "~/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "~/components/ui/card"
import { Pencil } from "lucide-react"
import type { ModuleResponse } from "~/types/api"

export async function clientLoader({ params }: Route.LoaderArgs) {
  const session = requireAuth()
  const module = await api.get<ModuleResponse>(`/api/v1/modules/${params.id}`, session.token)
  return { module, role: session.role, userId: session.userId }
}

export async function clientAction({ request, params }: Route.ActionArgs) {
  const session = requireAuth()
  const form = await request.formData()
  const actionName = form.get("_action")

  if (actionName === "enroll" && session.userId) {
    await api.post(
      "/api/v1/enrollments",
      session.token,
      { studentId: session.userId, moduleId: params.id },
    )
  }
  return redirect(`/modules/${params.id}`)
}

export default function ModuleDetailPage({ loaderData }: Route.ComponentProps) {
  const { module, role, userId } = loaderData

  return (
    <div className="space-y-6">
      <PageHeader
        title={module.moduleName}
        description={`Code: ${module.moduleCode}`}
        action={
          role === "ADMIN" ? (
            <Button render={<Link to={`/modules/${module.id}/edit`} />}>
              <Pencil className="mr-2 h-4 w-4" />
              Edit
            </Button>
          ) : undefined
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Module Info</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Description</span>
            <span>{module.description || "—"}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Credits</span>
            <span>{module.credits}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Semester</span>
            <span>{module.semester}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Enrollment</span>
            <span>{module.currentEnrollment} / {module.maxStudents}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Status</span>
            <StatusBadge status={module.status} />
          </div>
        </CardContent>
      </Card>

      {role === "STUDENT" && userId && (
        <Form method="post">
          <Button type="submit" name="_action" value="enroll">
            Enroll in Module
          </Button>
        </Form>
      )}
    </div>
  )
}
