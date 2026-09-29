import { useState } from "react";
import { createFileRoute, redirect, useRouter } from "@tanstack/react-router";
import { createDesk, fetchDesk, updateDesk } from "@atlas/external";
import type { DeskDetail } from "@atlas/data";
import { DeskEditor } from "@/components/DeskEditor";
import { resetSession } from "@/application/auth/session";

const buttonStyle = { padding: "6px 12px", borderRadius: 4, fontSize: 14, border: "1px solid #3a424c", background: "transparent", color: "#e6e6e6", cursor: "pointer" } as const;

type Editing = { desk?: DeskDetail };

function AdminPage() {
  const router = useRouter();
  const { session } = Route.useRouteContext();
  const [editing, setEditing] = useState<Editing>();
  const [error, setError] = useState<string>();

  const report = (e: unknown) => setError(e instanceof Error ? e.message : String(e));

  const openNew = () => {
    setError(undefined);
    setEditing({});
  };

  const openEdit = (key: string) => {
    setError(undefined);
    fetchDesk(key).then(desk => setEditing({ desk })).catch(report);
  };

  const save = async (desk: DeskDetail) => {
    try {
      if (editing?.desk) {
        await updateDesk(desk.key, { name: desk.name, description: desk.description, members: desk.members });
      } else {
        await createDesk(desk);
      }
      setEditing(undefined);
      resetSession();
      await router.invalidate();
    } catch (e) {
      report(e);
    }
  };

  return (
    <div style={{ padding: 24, fontFamily: "Arial, Helvetica, sans-serif", color: "#e8ebef" }}>
      <header style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
        <h1 style={{ margin: 0, fontSize: 22, color: "#ffffff" }}>Desks</h1>
        <button type="button" onClick={openNew} style={{ ...buttonStyle, background: "#2fcf75", color: "#06210f", fontWeight: 700 }}>New desk</button>
      </header>
      {error && !editing && <p role="alert" style={{ color: "#f2b632" }}>{error}</p>}
      <table style={{ borderCollapse: "collapse", width: "100%", maxWidth: 900, fontSize: 15 }}>
        <thead>
          <tr style={{ color: "#9aa3ad", fontSize: 13, textAlign: "left" }}>
            <th style={{ padding: "8px 10px" }}>Desk</th><th style={{ padding: "8px 10px" }}>Key</th><th style={{ padding: "8px 10px" }}>Description</th><th></th>
          </tr>
        </thead>
        <tbody>
          {session.desks.map(desk => (
            <tr key={desk.key} style={{ borderTop: "1px solid #2a3038" }}>
              <td style={{ padding: "10px" }}>{desk.name}</td>
              <td style={{ padding: "10px", color: "#9aa3ad" }}>{desk.key}</td>
              <td style={{ padding: "10px", color: "#9aa3ad" }}>{desk.description}</td>
              <td style={{ padding: "10px", textAlign: "right" }}><button type="button" onClick={() => openEdit(desk.key)} style={buttonStyle}>Edit</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      {editing && <DeskEditor desk={editing.desk} onSave={save} onCancel={() => setEditing(undefined)} error={error} />}
    </div>
  );
}

// The admin area: who is in which desk. Admins only - everyone else is sent to /forbidden.
export const Route = createFileRoute("/admin")({
  beforeLoad: ({ context }) => {
    if (!context.session.me.isAdmin) {
      throw redirect({ to: "/forbidden" });
    }
  },
  component: AdminPage,
});
