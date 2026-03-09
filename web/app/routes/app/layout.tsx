import { Outlet } from "react-router"
import type { Route } from "./+types/layout"
import { requireAuth } from "~/lib/auth.server"
import { SidebarProvider } from "~/components/ui/sidebar"
import { AppSidebar } from "~/components/layout/app-sidebar"
import { AppHeader } from "~/components/layout/app-header"

export async function loader({ request }: Route.LoaderArgs) {
  const session = requireAuth(request)
  return { session }
}

export default function AppLayout({ loaderData }: Route.ComponentProps) {
  const { session } = loaderData

  return (
    <SidebarProvider>
      <AppSidebar session={session} />
      <div className="flex min-h-screen flex-1 flex-col">
        <AppHeader session={session} />
        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </SidebarProvider>
  )
}
