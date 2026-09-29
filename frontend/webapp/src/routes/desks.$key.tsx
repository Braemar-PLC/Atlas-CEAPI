import { createFileRoute, notFound, redirect } from "@tanstack/react-router";
import { pagesOf } from "@/application/registries/nav-tabs";

function DeskPage() {
  const { key } = Route.useParams();
  const { session } = Route.useRouteContext();
  const desk = session.desks.find(d => d.key === key)!;

  return (
    <div style={{ padding: 24, fontFamily: "Arial, Helvetica, sans-serif" }}>
      <h1 style={{ margin: "0 0 4px", fontSize: 22, color: "#ffffff" }}>{desk.name}</h1>
      <p style={{ margin: "0 0 20px", color: "#9aa3ad", fontSize: 14 }}>{desk.description}</p>
      <p style={{ color: "#e8ebef" }}>No products yet.</p>
    </div>
  );
}

// A desk's own page. A desk with screens goes straight to its first one; an unknown key is a 404.
export const Route = createFileRoute("/desks/$key")({
  beforeLoad: ({ params, context }) => {
    if (!context.session.desks.some(d => d.key === params.key)) {
      throw notFound();
    }
    const [first] = pagesOf(params.key);
    if (first) {
      throw redirect({ to: first.to });
    }
  },
  component: DeskPage,
});
