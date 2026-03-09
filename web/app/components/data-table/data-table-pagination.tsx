import { Link, useLocation } from "react-router"
import { cn } from "~/lib/utils"
import { buttonVariants } from "~/components/ui/button"
import { ChevronLeft, ChevronRight } from "lucide-react"

interface DataTablePaginationProps {
  currentPage: number
  totalPages: number
  totalElements: number
}

export function DataTablePagination({
  currentPage,
  totalPages,
  totalElements,
}: DataTablePaginationProps) {
  const location = useLocation()
  const params = new URLSearchParams(location.search)

  const prevParams = new URLSearchParams(params)
  prevParams.set("page", String(currentPage - 1))

  const nextParams = new URLSearchParams(params)
  nextParams.set("page", String(currentPage + 1))

  return (
    <div className="flex items-center justify-between px-2">
      <p className="text-muted-foreground text-sm">
        {totalElements} result{totalElements !== 1 ? "s" : ""}
      </p>
      <div className="flex items-center gap-2">
        <span className="text-sm">
          Page {currentPage + 1} of {totalPages}
        </span>
        {currentPage === 0 ? (
          <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "opacity-50 pointer-events-none")}>
            <ChevronLeft className="h-4 w-4" />
          </span>
        ) : (
          <Link to={`?${prevParams}`} className={buttonVariants({ variant: "outline", size: "sm" })}>
            <ChevronLeft className="h-4 w-4" />
          </Link>
        )}
        {currentPage >= totalPages - 1 ? (
          <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "opacity-50 pointer-events-none")}>
            <ChevronRight className="h-4 w-4" />
          </span>
        ) : (
          <Link to={`?${nextParams}`} className={buttonVariants({ variant: "outline", size: "sm" })}>
            <ChevronRight className="h-4 w-4" />
          </Link>
        )}
      </div>
    </div>
  )
}
