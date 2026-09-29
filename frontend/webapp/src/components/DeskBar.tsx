import { Link } from "@tanstack/react-router";
import type { Desk } from "@atlas/data";
import { AdminKey } from "@/application/registries/nav-tabs";
import { barButtonActiveStyle, barButtonStyle } from "./bar-styles";
import { DeskLink } from "./DeskLink";

/**
 * The desks as buttons in the top bar, with Admin last for admins. Every desk is shown to everyone; which desk a
 * person belongs to only decides where they land. `activeKey` lights the desk the current page belongs to.
 */
export function DeskBar({ desks, isAdmin, activeKey }: { desks: Desk[]; isAdmin: boolean; activeKey: string | undefined }) {
  const style = (key: string) => (key === activeKey ? barButtonActiveStyle : barButtonStyle);

  return (
    <nav aria-label="Desks" style={{ display: "flex", gap: 4, overflowX: "auto" }}>
      {desks.map(desk => (
        <DeskLink key={desk.key} deskKey={desk.key} style={style(desk.key)}>{desk.name}</DeskLink>
      ))}
      {isAdmin && <Link to="/admin" preload="intent" style={style(AdminKey)}>Admin</Link>}
    </nav>
  );
}
