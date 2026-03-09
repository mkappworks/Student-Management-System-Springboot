import { data, redirect } from "react-router"
import { Form, Link, useActionData, useNavigation } from "react-router"
import type { Route } from "./+types/send"
import { requireRole } from "~/lib/auth.server"
import { api, ApiError } from "~/lib/api.server"
import { PageHeader } from "~/components/layout/page-header"
import { FormField } from "~/components/forms/form-field"
import { Button } from "~/components/ui/button"
import { Card, CardContent } from "~/components/ui/card"
import { Label } from "~/components/ui/label"
import { Textarea } from "~/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "~/components/ui/select"
import type { NotificationResponse } from "~/types/api"

export async function loader({ request }: Route.LoaderArgs) {
  requireRole(request, ["ADMIN", "TEACHER"])
  return null
}

export async function action({ request }: Route.ActionArgs) {
  const session = requireRole(request, ["ADMIN", "TEACHER"])
  const form = await request.formData()

  const payload = {
    recipientId: Number(form.get("recipientId")),
    recipientEmail: form.get("recipientEmail"),
    subject: form.get("subject"),
    message: form.get("message"),
    type: form.get("type") ?? "EMAIL",
  }

  try {
    await api.post<NotificationResponse>("/api/v1/notifications/send", session.token, payload)
    return redirect("/notifications")
  } catch (err) {
    if (err instanceof ApiError) return data({ error: err.message }, { status: err.status })
    return data({ error: "Failed to send notification." }, { status: 500 })
  }
}

export default function SendNotificationPage() {
  const actionData = useActionData<typeof action>()
  const navigation = useNavigation()
  const isSubmitting = navigation.state === "submitting"

  return (
    <div className="space-y-4">
      <PageHeader title="Send Notification" description="Send a notification to a student or teacher" />
      <Card>
        <CardContent className="pt-6">
          <Form method="post" className="space-y-4 max-w-lg">
            {actionData?.error && (
              <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
                {actionData.error}
              </div>
            )}
            <FormField label="Recipient ID" name="recipientId" type="number" required />
            <FormField label="Recipient Email" name="recipientEmail" type="email" required />
            <FormField label="Subject" name="subject" required />
            <div className="space-y-1">
              <Label htmlFor="type">Type</Label>
              <Select name="type" defaultValue="EMAIL">
                <SelectTrigger id="type">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="EMAIL">Email</SelectItem>
                  <SelectItem value="SMS">SMS</SelectItem>
                  <SelectItem value="PUSH">Push</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="message">Message</Label>
              <Textarea id="message" name="message" rows={5} required />
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Sending..." : "Send Notification"}
              </Button>
              <Button variant="outline" render={<Link to="/notifications" />}>
                Cancel
              </Button>
            </div>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
