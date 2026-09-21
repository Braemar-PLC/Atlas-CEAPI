
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/trades/$tradeId')({
  component: () => {
    const { tradeId } = Route.useParams()   // fully typed param
    return (
      <div style={{ padding: 24 }}>
        <h2>Trade detail</h2>
        <p>ID: {tradeId}</p>
      </div>
    )
  },
})