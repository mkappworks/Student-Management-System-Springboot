import { Outlet } from "react-router"
import type { Route } from "./+types/layout"
import { requireAuth } from "~/lib/auth"
import { SidebarProvider } from "~/components/ui/sidebar"
import { AppSidebar } from "~/components/layout/app-sidebar"
import { AppHeader } from "~/components/layout/app-header"

export async function clientLoader() {
  const session = requireAuth()
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
