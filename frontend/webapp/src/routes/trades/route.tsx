
import { createFileRoute, Link, Outlet } from '@tanstack/react-router'
import { Route as TradeDetailRoute } from './$tradeId/route'

export const Route = createFileRoute('/trades')({
  component: () => (
    <div style={{ padding: 24 }}>
      <h2>Trades</h2>
      <ul>
        <li>
          <Link to={TradeDetailRoute.to} params={{ tradeId: 'T-100' }}>
            T‑100
          </Link>
        </li>
        <li>
          <Link to={TradeDetailRoute.to} params={{ tradeId: 'T-200' }}>
            T‑200</Link></li>
      </ul>
      <Outlet />
    </div >
  ),
})
