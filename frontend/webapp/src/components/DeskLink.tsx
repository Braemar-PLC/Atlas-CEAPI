import type { CSSProperties, ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import { pagesOf } from "@/application/registries/nav-tabs";

/** A link to a desk's home: its first screen when it has screens, otherwise its own page under /desks. */
export function DeskLink({ deskKey, style, children }: { deskKey: string; style?: CSSProperties; children: ReactNode }) {
  const [first] = pagesOf(deskKey);
  if (first) {
    return <Link to={first.to} preload="intent" style={style}>{children}</Link>;
  }
  return <Link to="/desks/$key" params={{ key: deskKey }} preload="intent" style={style}>{children}</Link>;
}
