import { Form } from "react-router"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "~/components/ui/dialog"
import { Button } from "~/components/ui/button"

interface ConfirmDialogProps {
  trigger: React.ReactNode
  title: string
  description: string
  actionName: string
  value?: string
  method?: "post" | "delete"
  action?: string
  variant?: "destructive" | "default"
}

export function ConfirmDialog({
  trigger,
  title,
  description,
  actionName,
  value,
  method = "post",
  action,
  variant = "destructive",
}: ConfirmDialogProps) {
  return (
    <Dialog>
      <DialogTrigger render={<span />}>{trigger}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Form method={method} action={action}>
            {value && <input type="hidden" name="id" value={value} />}
            <Button type="submit" variant={variant} name="_action" value={actionName}>
              Confirm
            </Button>
          </Form>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
