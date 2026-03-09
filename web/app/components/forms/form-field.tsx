import { Label } from "~/components/ui/label"
import { Input } from "~/components/ui/input"

interface FormFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string
  name: string
  error?: string
}

export function FormField({ label, name, error, ...props }: FormFieldProps) {
  return (
    <div className="space-y-1">
      <Label htmlFor={name}>{label}</Label>
      <Input id={name} name={name} {...props} />
      {error && <p className="text-destructive text-sm">{error}</p>}
    </div>
  )
}
