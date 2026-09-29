// The look of a button in the black top bar, shared by the desk bar and a desk's page tabs.
export const barButtonStyle = {
  padding: "8px 14px",
  borderRadius: 4,
  color: "#e6e6e6",
  fontSize: 16,
  fontWeight: 400,
  whiteSpace: "nowrap",
} as const;

export const barButtonActiveStyle = { ...barButtonStyle, backgroundColor: "#2b3a4e", color: "#ffffff" } as const;
