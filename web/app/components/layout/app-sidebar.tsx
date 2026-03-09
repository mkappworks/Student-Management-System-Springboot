import { Link, useLocation } from "react-router"
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "~/components/ui/sidebar"
import {
  LayoutDashboard,
  Users,
  GraduationCap,
  BookOpen,
  Award,
  ClipboardList,
  Bell,
} from "lucide-react"
import type { Session } from "~/types/api"

interface NavItem {
  label: string
  href: string
  icon: React.ElementType
  roles?: string[]
}

const navItems: NavItem[] = [
  { label: "Dashboard", href: "/", icon: LayoutDashboard },
  { label: "Students", href: "/students", icon: Users, roles: ["ADMIN", "TEACHER"] },
  { label: "Teachers", href: "/teachers", icon: GraduationCap, roles: ["ADMIN"] },
  { label: "Modules", href: "/modules", icon: BookOpen },
  { label: "Grades", href: "/grades", icon: Award },
  { label: "Enrollments", href: "/enrollments", icon: ClipboardList },
  { label: "Notifications", href: "/notifications", icon: Bell },
]

export function AppSidebar({ session }: { session: Session }) {
  const location = useLocation()

  const visibleItems = navItems.filter(
    (item) => !item.roles || item.roles.includes(session.role),
  )

  return (
    <Sidebar>
      <SidebarHeader className="border-b px-6 py-4">
        <Link to="/" className="flex items-center gap-2 font-semibold">
          <GraduationCap className="h-6 w-6" />
          <span>SMS Portal</span>
        </Link>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Navigation</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {visibleItems.map((item) => {
                const isActive =
                  item.href === "/"
                    ? location.pathname === "/"
                    : location.pathname.startsWith(item.href)
                return (
                  <SidebarMenuItem key={item.href}>
                    <SidebarMenuButton render={<Link to={item.href} />} isActive={isActive}>
                      <item.icon className="h-4 w-4" />
                      <span>{item.label}</span>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                )
              })}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  )
}
