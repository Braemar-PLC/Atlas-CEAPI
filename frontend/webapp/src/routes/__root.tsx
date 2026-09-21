
import { createRootRoute, Link, Outlet } from '@tanstack/react-router'

import { Route as AboutRoute } from './about'
import { Route as TradeRoute } from './trades/route'
import { Route as AdminRoute } from './admin'
import { NotFound } from '@/components/NotFound'


export const Route = createRootRoute({
  component: () => (
    <>
      <nav style={{ padding: 8, borderBottom: '1px solid #ddd' }}>
        <Link to="/">Home</Link>{' | '}
        <Link to={AboutRoute.to}> About</Link>{' | '}
        <Link to={TradeRoute.to} preload="intent">Trades</Link>{' | '}
        {/* <Link to={NatgasGridRoute.to} preload="intent">Natgas</Link>{' | '} */}
        <Link to={AdminRoute.to} preload="intent">Admin</Link>
      </nav>
      <Outlet />

    </>
  ),
  notFoundComponent: NotFound,
})
