import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DeskEditor } from "@/components/DeskEditor";

const naturalGas = { key: "natural-gas", name: "Natural Gas", description: "TTF, NBP and JKM", members: ["sean.hays@braemar.com"] };

describe("DeskEditor", () => {
  it("edits an existing desk: the key is fixed, the rest can change, members are chips", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<DeskEditor desk={naturalGas} onSave={onSave} onCancel={() => {}} />);

    expect(screen.queryByLabelText("Key")).toBeNull();
    expect(screen.getByText("natural-gas")).toBeInTheDocument();
    expect(screen.getByText("sean.hays@braemar.com")).toBeInTheDocument();

    await user.clear(screen.getByLabelText("Name"));
    await user.type(screen.getByLabelText("Name"), "Nat Gas");
    await user.type(screen.getByLabelText("Add a member by email"), "marc.jarvis@braemar.com{Enter}");
    await user.click(screen.getByRole("button", { name: "Remove sean.hays@braemar.com" }));
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onSave).toHaveBeenCalledWith({ key: "natural-gas", name: "Nat Gas", description: "TTF, NBP and JKM", members: ["marc.jarvis@braemar.com"] });
  });

  it("creates a new desk: the key is typed too", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<DeskEditor onSave={onSave} onCancel={() => {}} />);

    await user.type(screen.getByLabelText("Key"), "lng");
    await user.type(screen.getByLabelText("Name"), "LNG");
    await user.type(screen.getByLabelText("Description"), "Cargoes");
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onSave).toHaveBeenCalledWith({ key: "lng", name: "LNG", description: "Cargoes", members: [] });
  });

  it("does not add the same email twice, and a comma adds a member like Enter does", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<DeskEditor desk={naturalGas} onSave={onSave} onCancel={() => {}} />);

    await user.type(screen.getByLabelText("Add a member by email"), "sean.hays@braemar.com,");
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onSave.mock.calls[0][0].members).toEqual(["sean.hays@braemar.com"]);
  });

  it("shows the reason when a save was refused, and Cancel gives up", async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(<DeskEditor desk={naturalGas} onSave={() => {}} onCancel={onCancel} error={"\"sean.hays\" is not an email address."} />);

    expect(screen.getByRole("alert")).toHaveTextContent("\"sean.hays\" is not an email address.");

    await user.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onCancel).toHaveBeenCalled();
  });
});
