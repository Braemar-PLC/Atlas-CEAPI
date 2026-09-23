
import { createRootRoute, Link, Outlet } from '@tanstack/react-router'

import { NotFound } from '@/components/NotFound'
import { SignedInAs } from '@/components/SignedInAs'
import { NavTabs } from '@/application/registries/nav-tabs'
import { useSignIn } from '@/application/auth/use-sign-in'

const tabStyle = {
  padding: '8px 14px',
  borderRadius: 4,
  color: '#e6e6e6',
  fontSize: 16,
  fontWeight: 400,
  whiteSpace: 'nowrap',
} as const

function RootLayout() {
  const signIn = useSignIn()

  return (
    // The page is exactly one window tall: the bar on top, the screen filling the rest. The screen scrolls
    // inside itself, which is what keeps its title and column headers in view ("freeze panes").
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Black bar with the logo at the left, as in Braemar's other desk apps. The logo file is the official
          "light" version from braemar.com (off-white wordmark), so it needs a dark background behind it.
          The tabs come from NavTabs and scroll sideways if there are ever more than fit. Who is signed in
          sits at the right, when the site has a sign-in in front of it (Azure only). */}
      <nav style={{ display: 'flex', alignItems: 'center', gap: 24, padding: '10px 16px', background: '#000', color: '#fff' }}>
        <Link to="/" style={{ display: 'flex', flexShrink: 0 }}>
          <img src="/braemar-logo-light.png" alt="Braemar" height={34} />
        </Link>
        <div style={{ display: 'flex', gap: 4, overflowX: 'auto' }}>
          {NavTabs.map(tab => (
            <Link
              key={tab.to}
              to={tab.to}
              preload="intent"
              style={tabStyle}
              activeProps={{ style: { ...tabStyle, background: '#2b3a4e', color: '#ffffff' } }}
            >
              {tab.label}
            </Link>
          ))}
        </div>
        <SignedInAs signIn={signIn} />
      </nav>
      <main style={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
        <Outlet />
      </main>
    </div>
  )
}

export const Route = createRootRoute({
  component: RootLayout,
  notFoundComponent: NotFound,
})
