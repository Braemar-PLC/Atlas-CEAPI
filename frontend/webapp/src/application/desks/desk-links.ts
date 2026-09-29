import { AdminKey, DeskPages, pagesOf } from "@/application/registries/nav-tabs";

/** Where a desk's button in the bar goes: a desk with screens to its first one, any other desk to its own page. */
export function deskHome(key: string): string {
  return pagesOf(key)[0]?.to ?? `/desks/${key}`;
}

/** Which desk bar button to light for an address; undefined on the chooser and on pages that belong to no desk. */
export function activeDeskKey(pathname: string): string | undefined {
  for (const key of Object.keys(DeskPages)) {
    if (pagesOf(key).some(page => page.to === pathname)) {
      return key;
    }
  }
  if (pathname === "/admin") {
    return AdminKey;
  }
  const desk = /^\/desks\/([^/]+)/.exec(pathname);
  return desk?.[1];
}
