import { Badge } from "~/components/ui/badge"

const statusVariants: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  ACTIVE: "default",
  ENROLLED: "default",
  SENT: "default",
  FULL_TIME: "default",
  INACTIVE: "secondary",
  COMPLETED: "secondary",
  CONTRACT: "secondary",
  PART_TIME: "outline",
  SUSPENDED: "destructive",
  DROPPED: "destructive",
  FAILED: "destructive",
  PENDING: "outline",
  GRADUATED: "outline",
}

export function StatusBadge({ status }: { status: string }) {
  const variant = statusVariants[status] ?? "secondary"
  return <Badge variant={variant}>{status}</Badge>
}
