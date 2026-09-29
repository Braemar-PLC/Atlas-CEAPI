import { createRootRoute, Link, Outlet, redirect, useLocation } from '@tanstack/react-router'

import { NotFound } from '@/components/NotFound'
import { SignedInAs } from '@/components/SignedInAs'
import { DeskBar } from '@/components/DeskBar'
import { barButtonActiveStyle, barButtonStyle } from '@/components/bar-styles'
import { pagesOf } from '@/application/registries/nav-tabs'
import { activeDeskKey } from '@/application/desks/desk-links'
import { useSignIn } from '@/application/auth/use-sign-in'
import { loadSession } from '@/application/auth/session'

function RootLayout() {
  const { session } = Route.useRouteContext()
  const signIn = useSignIn()
  const pathname = useLocation({ select: location => location.pathname })
  const activeKey = activeDeskKey(pathname)
  const pages = activeKey ? pagesOf(activeKey) : []

  return (
    // The page is exactly one window tall: the bars on top, the screen filling the rest. The screen scrolls
    // inside itself, which is what keeps its title and column headers in view ("freeze panes").
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Black bar with the logo at the left, as in Braemar's other desk apps. The logo file is the official
          "light" version from braemar.com (off-white wordmark), so it needs a dark background behind it.
          Then the desks as buttons, and who is signed in at the right. */}
      <nav style={{ display: 'flex', alignItems: 'center', gap: 24, padding: '10px 16px', background: '#000', color: '#fff' }}>
        <Link to="/" style={{ display: 'flex', flexShrink: 0 }}>
          <img src="/braemar-logo-light.png" alt="Braemar" height={34} />
        </Link>
        <DeskBar desks={session.desks} isAdmin={session.me.isAdmin} activeKey={activeKey} />
        <SignedInAs signIn={signIn} />
      </nav>
      {/* The active desk's pages, as tabs under the bar - the desks with screens (Natural Gas, Coal) have them. */}
      {pages.length > 0 && (
        <div style={{ display: 'flex', gap: 4, padding: '6px 16px', background: '#0b0d10', borderBottom: '1px solid #1c2128' }}>
          {pages.map(page => (
            <Link key={page.to} to={page.to} preload="intent" style={barButtonStyle} activeProps={{ style: barButtonActiveStyle }}>
              {page.label}
            </Link>
          ))}
        </div>
      )}
      <main style={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
        <Outlet />
      </main>
    </div>
  )
}

/** Shown when the session could not be loaded and it was not an expired sign-in (that reloads the page instead). */
function LoadFailed({ error }: { error: Error }) {
  return (
    <div style={{ padding: 24, fontFamily: 'Arial, Helvetica, sans-serif' }}>
      <p>Atlas could not load: {error.message}</p>
      <a href="/">Try again</a>
    </div>
  )
}

export const Route = createRootRoute({
  // Who is signed in and which desks exist, before any page draws: the bar and the landing depend on it.
  beforeLoad: async ({ location }) => {
    const session = await loadSession()
    const deskKey = activeDeskKey(location.pathname)
    if (deskKey && pagesOf(deskKey).length > 0 && !session.desks.some(desk => desk.key === deskKey)) {
      throw redirect({ to: '/desks' })
    }
    return { session }
  },
  component: RootLayout,
  errorComponent: LoadFailed,
  notFoundComponent: NotFound,
})
