import type { ReactElement } from "react";
import { render } from "@testing-library/react";
import { createMemoryHistory, createRootRoute, createRouter, RouterProvider } from "@tanstack/react-router";

/** Renders a component that uses router links, inside a throwaway router sitting at `path`. */
export async function renderWithRouter(element: ReactElement, path = "/") {
  const routeTree = createRootRoute({ component: () => element });
  const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: [path] }) });
  const view = render(<RouterProvider router={router} />);
  await router.load();
  return view;
}
