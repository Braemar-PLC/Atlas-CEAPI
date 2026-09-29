import { createFileRoute, Link } from "@tanstack/react-router";
import { DeskLink } from "@/components/DeskLink";

const tileStyle = {
  display: "flex",
  flexDirection: "column",
  gap: 6,
  padding: "16px 18px",
  minHeight: 110,
  borderRadius: 6,
  border: "1px solid #2a3038",
  background: "#171b21",
  color: "#e8ebef",
  textDecoration: "none",
} as const;

function DeskChooser() {
  const { session } = Route.useRouteContext();

  return (
    <div style={{ padding: 24, fontFamily: "Arial, Helvetica, sans-serif" }}>
      <h1 style={{ margin: "0 0 4px", fontSize: 22, color: "#ffffff" }}>Choose a desk</h1>
      <p style={{ margin: "0 0 20px", color: "#9aa3ad", fontSize: 14 }}>
        {session.me.isAdmin
          ? "Administrators can access every desk."
          : session.desks.length === 0
            ? "You do not have access to a desk yet. Ask an Atlas administrator to add you."
            : "Choose one of the desks you belong to."}
      </p>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: 16 }}>
        {session.desks.map(desk => (
          <DeskLink key={desk.key} deskKey={desk.key} style={tileStyle}>
            <span style={{ fontSize: 18, fontWeight: 700, color: "#ffffff" }}>{desk.name}</span>
            <span style={{ fontSize: 14, color: "#9aa3ad" }}>{desk.description}</span>
          </DeskLink>
        ))}
        {session.me.isAdmin && (
          <Link to="/admin" preload="intent" style={{ ...tileStyle, borderColor: "#f2b632" }}>
            <span style={{ fontSize: 18, fontWeight: 700, color: "#ffffff" }}>Admin</span>
            <span style={{ fontSize: 14, color: "#9aa3ad" }}>Desks and who is in them</span>
          </Link>
        )}
      </div>
    </div>
  );
}

export const Route = createFileRoute("/desks/")({
  component: DeskChooser,
});
