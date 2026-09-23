import { themeQuartz } from "ag-grid-community";

/**
 * The desk's ask (18 Sep 2026): replicate the ICE screen as closely as possible — clean black
 * background, white figures, light grey header, blue selected row. Colours were read off the
 * 17 Sep photo of the ICE "Nat Gas TTF Flat Price" screen.
 * The red/green bars and the tick square are per-cell styling, so they live in natgas.css.
 */
export const iceScreenTheme = themeQuartz.withParams({
  browserColorScheme: "dark",
  fontFamily: ["Arial", "Helvetica", "sans-serif"],
  // The desk's ask (Marc Jarvis, Harrison Lee, 21 Sep 2026): figures "slightly larger with less space around
  // them", filling the row better without crowding it. So 18px in the same 30px row (it was 15px).
  // Bold was tried the same day and the desk preferred regular weight, so the weight is 400 (set in natgas.css,
  // because the theme has no setting for it). Headers are smaller, so the figures are what the eye lands on.
  dataFontSize: 18,
  headerFontSize: 15,
  headerFontWeight: 400,

  backgroundColor: "#000000",
  foregroundColor: "#ffffff",
  // Every other row is dark grey, as on the ICE wall screens — it helps the eye read across (Sean, Sep 2026).
  oddRowBackgroundColor: "#242424",
  headerBackgroundColor: "#e6e9ed",
  headerTextColor: "#1b1f24",

  // Row lines are drawn per cell in natgas.css, so the red and green bars run unbroken.
  rowBorder: false,
  columnBorder: false,
  headerColumnBorder: { color: "#c3c8cf" },
  // The line between two headers is the handle for resizing a column ("column width should be a slider" -
  // Harrison Lee, 21 Sep 2026). Darker, thicker and full height, so it reads as something to grab.
  headerColumnResizeHandleColor: "#979ea8",
  headerColumnResizeHandleHeight: "100%",
  headerColumnResizeHandleWidth: 3,
  headerRowBorder: false,
  wrapperBorder: false,
  wrapperBorderRadius: 0,

  rowHoverColor: "#2b4a8c",
  selectedRowBackgroundColor: "#2b4a8c",
  // Highlighter yellow behind Last for the moment it changes. natgas.css turns the figure black meanwhile.
  valueChangeValueHighlightBackgroundColor: "#ffff00",
  // Tighter than the 8px it was: part of "less space around" the figures, and it buys the bigger digits room.
  cellHorizontalPadding: 5,
});
