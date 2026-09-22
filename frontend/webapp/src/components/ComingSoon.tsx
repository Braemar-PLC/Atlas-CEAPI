/**
 * Stand-in for a screen that has a tab but is not built yet.
 * It says so plainly rather than showing an empty or made-up grid.
 */
export function ComingSoon({ title, needs }: { title: string; needs: string }) {
  return (
    <div style={{ padding: "24px 16px", color: "#d0d0d0", fontFamily: "Arial, Helvetica, sans-serif" }}>
      <h2 style={{ margin: "0 0 8px", fontSize: 18, fontWeight: 400, color: "#ffffff" }}>{title}</h2>
      <p style={{ margin: 0, fontSize: 14 }}>Not built yet. {needs}</p>
    </div>
  );
}
