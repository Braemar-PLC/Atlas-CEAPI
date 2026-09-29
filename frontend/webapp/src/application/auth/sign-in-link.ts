/** The platform's sign-in address (Azure App Service Authentication, Entra ID): Microsoft's page, then back here. */
export const SignInEndpoint = "/.auth/login/aad";

/**
 * Where the arrow on the sign-in page sends the browser: the platform's sign-in, then back to `returnTo` (a path on
 * this site, e.g. "/natgas"). A username, when given, goes along as a hint so it need not be typed twice at
 * Microsoft's page; whether the platform passes the hint on is not documented, so the page must work without it.
 */
export function signInHref(returnTo: string, username = ""): string {
  const params = new URLSearchParams({ post_login_redirect_uri: returnTo });
  const hint = username.trim();
  if (hint) {
    params.set("login_hint", hint);
  }
  return `${SignInEndpoint}?${params}`;
}
