import type { Plugin } from "vite";

/** The person the laptop is signed in as. Keep it the same person as Auth:DevelopmentUser in the API's appsettings.Development.json. */
export type DevUser = { name: string; email: string; roles: string[] };

type Request = { method?: string; url?: string };
type Response = { statusCode: number; setHeader(name: string, value: string): unknown; end(body?: string): unknown };
type Next = () => void;

/**
 * Stands in, on the laptop, for what Azure App Service Authentication does in front of the site (set to allow
 * unauthenticated requests): answers /.auth/me with the signed-in user, or 401 while signed out; signs out on
 * /.auth/logout and back in on /.auth/login/aad; while signed out refuses the API (401) but still serves the app,
 * which shows its own sign-in page. POST /.auth/dev/sign-out flips to signed out without a browser, so the app's
 * reload-on-expired-session can be exercised.
 */
export function devSignInMiddleware(user: DevUser) {
  let signedIn = true;

  const redirect = (res: Response, to: string) => {
    res.statusCode = 302;
    res.setHeader("location", to);
    res.end();
  };

  return (req: Request, res: Response, next: Next): void => {
    const url = new URL(req.url ?? "/", "http://localhost");
    const path = url.pathname;

    if (path === "/.auth/me") {
      if (!signedIn) {
        res.statusCode = 401;
        res.end();
        return;
      }
      res.statusCode = 200;
      res.setHeader("content-type", "application/json; charset=utf-8");
      res.end(JSON.stringify([{
        provider_name: "aad",
        user_id: user.email,
        user_claims: [
          { typ: "name", val: user.name },
          { typ: "preferred_username", val: user.email },
          ...user.roles.map(role => ({ typ: "roles", val: role })),
        ],
      }]));
      return;
    }
    if (path === "/.auth/login/aad") {
      signedIn = true;
      redirect(res, url.searchParams.get("post_login_redirect_uri") ?? "/");
      return;
    }
    if (path === "/.auth/logout") {
      signedIn = false;
      redirect(res, url.searchParams.get("post_logout_redirect_uri") ?? "/");
      return;
    }
    if (path === "/.auth/dev/sign-out" && req.method === "POST") {
      signedIn = false;
      res.statusCode = 204;
      res.end();
      return;
    }

    if (!signedIn && path.startsWith("/api/")) {
      res.statusCode = 401;
      res.end();
      return;
    }
    next();
  };
}

/** The Vite plugin: only for the dev server, never in a build. */
export function devSignIn(user: DevUser): Plugin {
  return {
    name: "atlas-dev-sign-in",
    apply: "serve",
    configureServer(server) {
      server.middlewares.use(devSignInMiddleware(user));
    },
  };
}
