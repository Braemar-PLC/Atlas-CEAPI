import { AgGridReact } from "ag-grid-react";
import type {
    ColDef, ColumnResizedEvent, GetRowIdParams, GridApi, GridReadyEvent, RowClassRules, RowSelectionOptions,
} from "ag-grid-community";
import { useEffect, useRef, useState } from "react";
import { iceScreenTheme } from "./screen.theme";
import { clearColumnWidths, loadColumnWidths, saveColumnWidths } from "./screen.column-widths";
import type { ColumnWidths } from "./screen.column-widths";
import { keepUnchangedRows } from "./unchanged-rows";
import "./screen.css";

// Grid settings that never change are built ONCE, out here - not inside the component.
// The component re-renders on every price tick and every second (the clock). Settings written inline in the JSX
// would be brand-new objects each time, and AG Grid takes a new `defaultColDef` to mean "the columns have
// changed": it re-read the column definitions and threw away any width the user had just dragged
// (bug found by Sean, 21 Sep 2026). It was also needless work several times a second.

// Display-only, like the ICE screen: no sorting, column menus or dragging columns about.
// Columns CAN be resized (AG Grid's default) - see the note on ScreenComponent.
// No cell flash here: in defaultColDef it would reach the bars — see screen.col-defs.ts.
const defaultColDef: ColDef = {
    sortable: false,
    suppressMovable: true,
    suppressHeaderMenuButton: true,
    minWidth: 40,
};

// One row at a time; a click selects it and a second click on the same row clears it (without that option AG Grid
// only clears on Ctrl+click).
const rowSelection: RowSelectionOptions = {
  mode: "singleRow",
  checkboxes: false,
  enableClickSelection: true,
  enableSelectionWithoutKeys: true,
};

// A stable id per row lets the grid update changed cells in place instead of rebuilding every row on each tick.
const idOf = (row: { symbol: string; tenor: string }) => `${row.symbol}|${row.tenor}`;
const rowId = (p: GetRowIdParams) => idOf(p.data);

/**
 * How often each grid redraws its visible rows from scratch. A refresh that changes nothing on screen leaves React
 * holding a small record until that row or cell is next drawn (unchanged-rows.ts); a redraw lets React drop them,
 * so a screen left open for days does not keep growing. Selection, scroll position and column widths are kept.
 */
export const RowRedrawIntervalMs = 60_000;

/** `data`, with every row whose figures did not change replaced by the object the grid already has (unchanged-rows.ts). */
function useUnchangedRows<Row extends { symbol: string; tenor: string }>(data: Row[]): Row[] {
    // React's pattern for information from the previous render: the rows last handed over, and what they came from.
    const [kept, setKept] = useState({ from: data, rows: data });
    if (kept.from === data) return kept.rows;

    const next = { from: data, rows: keepUnchangedRows(kept.rows, data, idOf) };
    setKept(next);
    return next.rows;
}


/**
 * Dumb UI only:
 * title + data + colDefs → DataGrid styled as the ICE screen
 *
 * Column widths (the desk's ask, 21 Sep 2026 - "column width should be a slider"): drag the line between two
 * headers to resize a column, double-click it to fit the column to its contents. Widths are remembered per
 * screen in this browser (screen.column-widths.ts); "Reset columns" in the title bar puts them back.
 */
export function ScreenComponent({
    screenKey,
    title,
    data,
    colDefs,
    asOf,
    rowClassRules,
    pinnedTopRowData,
    headerHeight = 34,
    groupHeaderHeight,
    onGridReady,
    rowRedrawIntervalMs = RowRedrawIntervalMs
}: {
    screenKey: string;
    title: string;
    data: any[];
    colDefs: any[];
    asOf: string;
    /**
     * Optional: CSS classes to put on a row by what it holds (the option chains mark the future at the head of
     * each expiry). Must be a constant built outside the component - see the note at the top of this file.
     */
    rowClassRules?: RowClassRules;
    /** Optional: rows pinned above the scrolling ones. */
    pinnedTopRowData?: unknown[];
    /**
     * Optional: the height of the column header row, and of the column-group row above it where a screen has
     * groups. The option matrix sets the header row to 0 and keeps the group row, so each expiry block shows only
     * its "X26 71.691" heading, as on the desk's calculator.
     */
    headerHeight?: number;
    groupHeaderHeight?: number;
    /** Optional: told once the grid is up, with the grid's API. Tests use it to look at the columns. */
    onGridReady?: (api: GridApi) => void;
    /** Optional: how often the rows are redrawn (RowRedrawIntervalMs). Tests shorten it. */
    rowRedrawIntervalMs?: number;
}) {
    const gridRef = useRef<AgGridReact>(null);
    const rowData = useUnchangedRows(data);

    useEffect(() => {
        const timer = setInterval(() => gridRef.current?.api?.redrawRows(), rowRedrawIntervalMs);
        return () => clearInterval(timer);
    }, [rowRedrawIntervalMs]);
    // Only decides whether "Reset columns" is shown; the grid itself holds the widths.
    const [hasCustomWidths, setHasCustomWidths] = useState(() => Object.keys(loadColumnWidths(screenKey)).length > 0);

    // A remembered column gets a fixed width; `flex: null` stops it sharing the leftover space.
    // Every other column keeps flexing, so the grid still fills the window.
    const applyWidths = (event: GridReadyEvent) => {
        const widths: ColumnWidths = loadColumnWidths(screenKey);
        const state = Object.entries(widths).map(([colId, width]) => ({ colId, width, flex: null }));
        if (state.length) event.api.applyColumnState({ state });
        onGridReady?.(event.api);
    };

    const rememberWidths = (event: ColumnResizedEvent) => {
        // The grid also reports resizes it makes itself (flexing, applying saved widths). Only a person's
        // drag, or their double-click to auto-fit, is worth remembering - and only once the drag has ended.
        const byUser = event.source === "uiColumnResized" || event.source === "autosizeColumns";
        if (!event.finished || !byUser) return;

        const changed: ColumnWidths = {};
        for (const column of event.columns ?? []) {
            changed[column.getColId()] = column.getActualWidth();
        }
        if (Object.keys(changed).length === 0) return;

        saveColumnWidths(screenKey, changed);
        setHasCustomWidths(true);
    };

    const resetWidths = () => {
        clearColumnWidths(screenKey);
        gridRef.current?.api.resetColumnState(); // back to the widths in screen.col-defs.ts
        setHasCustomWidths(false);
    };

    return (
        <div className="ice-screen">
            <div className="ice-screen-title">
                <span>{title}</span>
                <span className="ice-screen-title-right">
                    {hasCustomWidths && (
                        <button type="button" className="ice-screen-reset" onClick={resetWidths}
                            title="Put every column back to its original width">
                            Reset columns
                        </button>
                    )}
                    <span className="ice-screen-asof">{asOf}</span>
                </span>
            </div>
            <div className="ice-screen-grid">
                {/* The grid fills the height left over (see screen.css) and scrolls its rows inside itself,
                    so the title bar and the column headers stay in view — "freeze panes". */}
                <AgGridReact
                    ref={gridRef}
                    theme={iceScreenTheme}
                    rowData={rowData}
                    columnDefs={colDefs}
                    rowHeight={30}
                    headerHeight={headerHeight}
                    groupHeaderHeight={groupHeaderHeight}
                    // A stable id per row lets the grid update changed cells in place
                    // instead of rebuilding every row on each price tick.
                    getRowId={rowId}
                    // Last flashes yellow for 0.3s when it changes, then cuts straight back (no fade).
                    // Which column flashes is set in screen.col-defs.ts; the colour in screen.theme.ts.
                    cellFlashDuration={300}
                    cellFadeDuration={0}
                    defaultColDef={defaultColDef}
                    onGridReady={applyWidths}
                    onColumnResized={rememberWidths}
                    rowSelection={rowSelection}
                    rowClassRules={rowClassRules}
                    pinnedTopRowData={pinnedTopRowData}
                    suppressCellFocus
                />
            </div>
        </div>
    );

}
