import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/react";
import { DeskBar } from "@/components/DeskBar";
import { renderWithRouter } from "../helpers/render-with-router";
import type { Desk } from "@atlas/data";

const desks: Desk[] = [
  { key: "coal", name: "Coal", description: "" },
  { key: "cross-commodities", name: "Cross-Commodities", description: "" },
  { key: "dry-ffa", name: "Dry FFA", description: "" },
  { key: "natural-gas", name: "Natural Gas", description: "" },
  { key: "power", name: "Power", description: "" },
];

describe("DeskBar", () => {
  it("shows every desk as a button, in the order given, and Admin last for an admin", async () => {
    await renderWithRouter(<DeskBar desks={desks} isAdmin={true} activeKey={undefined} />);

    const links = screen.getAllByRole("link");
    expect(links.map(l => l.textContent)).toEqual(["Coal", "Cross-Commodities", "Dry FFA", "Natural Gas", "Power", "Admin"]);
  });

  it("does not show Admin to anyone else", async () => {
    await renderWithRouter(<DeskBar desks={desks} isAdmin={false} activeKey={undefined} />);

    expect(screen.queryByRole("link", { name: "Admin" })).toBeNull();
  });

  it("sends a desk with screens to its first one, other desks to their own page, and Admin to the admin area", async () => {
    await renderWithRouter(<DeskBar desks={desks} isAdmin={true} activeKey={undefined} />);

    expect(screen.getByRole("link", { name: "Natural Gas" })).toHaveAttribute("href", "/natgas");
    expect(screen.getByRole("link", { name: "Coal" })).toHaveAttribute("href", "/coal/api2");
    expect(screen.getByRole("link", { name: "Power" })).toHaveAttribute("href", "/desks/power");
    expect(screen.getByRole("link", { name: "Admin" })).toHaveAttribute("href", "/admin");
  });

  it("lights the active desk and only that one", async () => {
    await renderWithRouter(<DeskBar desks={desks} isAdmin={false} activeKey="natural-gas" />);

    expect(screen.getByRole("link", { name: "Natural Gas" })).toHaveStyle({ backgroundColor: "rgb(43, 58, 78)" });
    expect(screen.getByRole("link", { name: "Coal" })).not.toHaveStyle({ backgroundColor: "rgb(43, 58, 78)" });
  });
});
