import { Link } from "react-router"
import type { Route } from "./+types/list"
import { requireAuth } from "~/lib/auth"
import { api, ApiError } from "~/lib/api"
import { DataTable } from "~/components/data-table/data-table"
import { PageHeader } from "~/components/layout/page-header"
import { StatusBadge } from "~/components/status-badge"
import { Button } from "~/components/ui/button"
import { Send } from "lucide-react"
import type { NotificationResponse } from "~/types/api"

export async function clientLoader() {
  const session = requireAuth()

  if (!session.userId) {
    return { notifications: [], role: session.role }
  }

  try {
    const notifications = await api.get<NotificationResponse[]>(
      `/api/v1/notifications/recipient/${session.userId}`,
      session.token,
    )
    return { notifications, role: session.role }
  } catch (err) {
    if (err instanceof ApiError) return { notifications: [], role: session.role, error: err.message }
    throw err
  }
}

export default function NotificationsListPage({ loaderData }: Route.ComponentProps) {
  const { notifications, role } = loaderData

  const columns = [
    { header: "Subject", accessorKey: "subject" as const },
    { header: "Type", accessorKey: "type" as const },
    {
      header: "Status",
      cell: (r: NotificationResponse) => <StatusBadge status={r.status} />,
    },
    {
      header: "Sent At",
      cell: (r: NotificationResponse) =>
        r.sentAt ? new Date(r.sentAt).toLocaleString() : "—",
    },
  ]

  return (
    <div className="space-y-4">
      <PageHeader
        title="Notifications"
        description="Your notifications"
        action={
          role !== "STUDENT" ? (
            <Button render={<Link to="/notifications/send" />}>
              <Send className="mr-2 h-4 w-4" />
              Send Notification
            </Button>
          ) : undefined
        }
      />
      <DataTable
        columns={columns}
        data={notifications}
        emptyMessage="No notifications found."
      />
    </div>
  )
}
