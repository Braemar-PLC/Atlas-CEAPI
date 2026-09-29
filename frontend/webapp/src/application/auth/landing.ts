import type { Me } from "@atlas/data";
import { deskHome } from "@/application/desks/desk-links";

export const ChooserPath = "/desks";

/** Where a signed-in user goes from "/": their desk if they have exactly one, otherwise the desk chooser. */
export function landingFor(me: Me): string {
  return me.homeDesk ? deskHome(me.homeDesk) : ChooserPath;
}
