import { Link } from "react-router"
import type { Route } from "./+types/detail"
import { requireAuth } from "~/lib/auth"
import { api } from "~/lib/api"
import { PageHeader } from "~/components/layout/page-header"
import { StatusBadge } from "~/components/status-badge"
import { DataTable } from "~/components/data-table/data-table"
import { Button } from "~/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "~/components/ui/card"
import { Pencil } from "lucide-react"
import type { StudentResponse, EnrollmentResponse, GradeResponse } from "~/types/api"

export async function clientLoader({ params }: Route.LoaderArgs) {
  const session = requireAuth()
  const [student, enrollments, grades] = await Promise.allSettled([
    api.get<StudentResponse>(`/api/v1/students/${params.id}`, session.token),
    api.get<EnrollmentResponse[]>(`/api/v1/enrollments/student/${params.id}`, session.token),
    api.get<GradeResponse[]>(`/api/v1/grades/student/${params.id}`, session.token),
  ])

  return {
    student: student.status === "fulfilled" ? student.value : null,
    enrollments: enrollments.status === "fulfilled" ? enrollments.value : [],
    grades: grades.status === "fulfilled" ? grades.value : [],
    role: session.role,
  }
}

export default function StudentDetailPage({ loaderData }: Route.ComponentProps) {
  const { student, enrollments, grades, role } = loaderData

  if (!student) {
    return <p className="text-muted-foreground">Student not found.</p>
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${student.firstName} ${student.lastName}`}
        description={`Student ID: ${student.studentId}`}
        action={
          role === "ADMIN" ? (
            <Button render={<Link to={`/students/${student.id}/edit`} />}>
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
              <span>{student.email}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Phone</span>
              <span>{student.phoneNumber || "—"}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Date of Birth</span>
              <span>{student.dateOfBirth || "—"}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Enrollment Date</span>
              <span>{student.enrollmentDate || "—"}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Status</span>
              <StatusBadge status={student.status} />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Grades</CardTitle>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={[
                { header: "Module", accessorKey: "moduleId" },
                { header: "Grade", accessorKey: "grade" },
                { header: "Score", cell: (r: GradeResponse) => `${r.score}/${r.maxScore}` },
                { header: "Type", accessorKey: "assessmentType" },
              ]}
              data={grades as GradeResponse[]}
              emptyMessage="No grades recorded."
            />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Enrollments</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={[
              { header: "Module", accessorKey: "moduleId" as const },
              {
                header: "Status",
                cell: (r: EnrollmentResponse) => <StatusBadge status={r.status} />,
              },
              { header: "Enrolled", accessorKey: "enrollmentDate" as const },
            ]}
            data={enrollments as EnrollmentResponse[]}
            emptyMessage="No enrollments found."
          />
        </CardContent>
      </Card>
    </div>
  )
}
