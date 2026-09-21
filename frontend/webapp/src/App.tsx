import { createRouter, RouterProvider } from '@tanstack/react-router'
import { routeTree } from './routes/routeTree.gen'

// Create the router (you can add defaults like preloading/error/404 here)
export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

export default function App() {
  return <RouterProvider router={router} />
}