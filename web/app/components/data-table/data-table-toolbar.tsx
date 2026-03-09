"use client"

import { useEffect, useState } from "react"
import { useNavigate, useSearchParams } from "react-router"
import { Input } from "~/components/ui/input"
import { useDebounce } from "~/hooks/use-debounce"
import { Search } from "lucide-react"

interface DataTableToolbarProps {
  placeholder?: string
}

export function DataTableToolbar({ placeholder = "Search..." }: DataTableToolbarProps) {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [value, setValue] = useState(searchParams.get("q") ?? "")
  const debouncedValue = useDebounce(value)

  useEffect(() => {
    const params = new URLSearchParams(searchParams)
    if (debouncedValue) {
      params.set("q", debouncedValue)
    } else {
      params.delete("q")
    }
    params.delete("page")
    navigate(`?${params}`, { replace: true })
  }, [debouncedValue])

  return (
    <div className="relative">
      <Search className="text-muted-foreground absolute top-1/2 left-2 h-4 w-4 -translate-y-1/2" />
      <Input
        className="pl-8"
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />
    </div>
  )
}
