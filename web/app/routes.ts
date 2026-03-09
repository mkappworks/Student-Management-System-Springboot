import { type RouteConfig, index, layout, prefix, route } from "@react-router/dev/routes"

export default [
  route("login", "routes/auth/login.tsx"),
  route("register", "routes/auth/register.tsx"),
  route("logout", "routes/auth/logout.tsx"),

  layout("routes/app/layout.tsx", [
    index("routes/app/dashboard.tsx"),

    ...prefix("students", [
      index("routes/app/students/list.tsx"),
      route("new", "routes/app/students/new.tsx"),
      route(":id", "routes/app/students/detail.tsx"),
      route(":id/edit", "routes/app/students/edit.tsx"),
    ]),

    ...prefix("teachers", [
      index("routes/app/teachers/list.tsx"),
      route("new", "routes/app/teachers/new.tsx"),
      route(":id", "routes/app/teachers/detail.tsx"),
      route(":id/edit", "routes/app/teachers/edit.tsx"),
    ]),

    ...prefix("modules", [
      index("routes/app/modules/list.tsx"),
      route("new", "routes/app/modules/new.tsx"),
      route(":id", "routes/app/modules/detail.tsx"),
      route(":id/edit", "routes/app/modules/edit.tsx"),
    ]),

    ...prefix("grades", [
      index("routes/app/grades/list.tsx"),
      route("new", "routes/app/grades/new.tsx"),
      route(":id/edit", "routes/app/grades/edit.tsx"),
    ]),

    ...prefix("enrollments", [
      index("routes/app/enrollments/list.tsx"),
      route("new", "routes/app/enrollments/new.tsx"),
    ]),

    ...prefix("notifications", [
      index("routes/app/notifications/list.tsx"),
      route("send", "routes/app/notifications/send.tsx"),
    ]),
  ]),
] satisfies RouteConfig
