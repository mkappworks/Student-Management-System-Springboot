import type { Route } from "./+types/dashboard"
import { requireAuth } from "~/lib/auth"
import { api } from "~/lib/api"
import { StatCard } from "~/components/stat-card"
import { PageHeader } from "~/components/layout/page-header"
import { Users, GraduationCap, BookOpen, ClipboardList } from "lucide-react"
import type { Page, StudentResponse, TeacherResponse, ModuleResponse } from "~/types/api"

export async function clientLoader() {
  const session = requireAuth()
  const token = session.token

  const [students, teachers, modules] = await Promise.allSettled([
    api.get<Page<StudentResponse>>("/api/v1/students?size=1", token),
    api.get<Page<TeacherResponse>>("/api/v1/teachers?size=1", token),
    api.get<Page<ModuleResponse>>("/api/v1/modules?size=1", token),
  ])

  return {
    counts: {
      students: students.status === "fulfilled" ? students.value.totalElements : 0,
      teachers: teachers.status === "fulfilled" ? teachers.value.totalElements : 0,
      modules: modules.status === "fulfilled" ? modules.value.totalElements : 0,
    },
    role: session.role,
  }
}

export default function DashboardPage({ loaderData }: Route.ComponentProps) {
  const { counts, role } = loaderData

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Welcome to the Student Management System" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {(role === "ADMIN" || role === "TEACHER") && (
          <StatCard title="Students" value={counts.students} icon={Users} href="/students" />
        )}
        {role === "ADMIN" && (
          <StatCard
            title="Teachers"
            value={counts.teachers}
            icon={GraduationCap}
            href="/teachers"
          />
        )}
        <StatCard title="Modules" value={counts.modules} icon={BookOpen} href="/modules" />
        <StatCard title="Enrollments" value="—" icon={ClipboardList} href="/enrollments" />
      </div>
    </div>
  )
}
