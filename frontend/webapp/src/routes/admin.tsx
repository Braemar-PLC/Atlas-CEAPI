
import { createFileRoute, redirect } from '@tanstack/react-router'
import { Route as Forbidden } from './forbidden'

function isAuthorized(): boolean {
  return false
}

export const Route = createFileRoute('/admin')({
  beforeLoad: () => {
    if (!isAuthorized()) {
      throw redirect({ to: Forbidden.to })
    }
  },
  component: () => <div style={{ padding: 24 }}>Admin Area</div>,
})