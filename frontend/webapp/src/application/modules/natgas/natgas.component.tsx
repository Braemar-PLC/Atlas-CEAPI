import { AgGridReact } from "ag-grid-react";
import type {
    ColDef, ColumnResizedEvent, GetRowIdParams, GridApi, GridReadyEvent, RowSelectionOptions,
} from "ag-grid-community";
import { useRef, useState } from "react";
import { iceScreenTheme } from "./natgas.theme";
import { clearColumnWidths, loadColumnWidths, saveColumnWidths } from "./natgas.column-widths";
import type { ColumnWidths } from "./natgas.column-widths";
import "./natgas.css";

// Grid settings that never change are built ONCE, out here - not inside the component.
// The component re-renders on every price tick and every second (the clock). Settings written inline in the JSX
// would be brand-new objects each time, and AG Grid takes a new `defaultColDef` to mean "the columns have
// changed": it re-read the column definitions and threw away any width the user had just dragged
// (bug found by Sean, 21 Sep 2026). It was also needless work several times a second.

// Display-only, like the ICE screen: no sorting, column menus or dragging columns about.
// Columns CAN be resized (AG Grid's default) - see the note on NatGasComponent.
// No cell flash here: in defaultColDef it would reach the bars — see natgas.col-defs.ts.
const defaultColDef: ColDef = {
    sortable: false,
    suppressMovable: true,
    suppressHeaderMenuButton: true,
    minWidth: 40,
};

const rowSelection: RowSelectionOptions = { mode: "singleRow", checkboxes: false, enableClickSelection: true };

// A stable id per row lets the grid update changed cells in place instead of rebuilding every row on each tick.
const rowId = (p: GetRowIdParams) => `${p.data.symbol}|${p.data.tenor}`;


/**
 * Dumb UI only:
 * title + data + colDefs → DataGrid styled as the ICE screen
 *
 * Column widths (the desk's ask, 21 Sep 2026 - "column width should be a slider"): drag the line between two
 * headers to resize a column, double-click it to fit the column to its contents. Widths are remembered per
 * screen in this browser (natgas.column-widths.ts); "Reset columns" in the title bar puts them back.
 */
export function NatGasComponent({
    screenKey,
    title,
    data,
    colDefs,
    asOf,
    onGridReady
}: {
    screenKey: string;
    title: string;
    data: any[];
    colDefs: any[];
    asOf: string;
    /** Optional: told once the grid is up, with the grid's API. Tests use it to look at the columns. */
    onGridReady?: (api: GridApi) => void;
}) {
    const gridRef = useRef<AgGridReact>(null);
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
        gridRef.current?.api.resetColumnState(); // back to the widths in natgas.col-defs.ts
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
                {/* The grid fills the height left over (see natgas.css) and scrolls its rows inside itself,
                    so the title bar and the column headers stay in view — "freeze panes". */}
                <AgGridReact
                    ref={gridRef}
                    theme={iceScreenTheme}
                    rowData={data}
                    columnDefs={colDefs}
                    rowHeight={30}
                    headerHeight={34}
                    // A stable id per row lets the grid update changed cells in place
                    // instead of rebuilding every row on each price tick.
                    getRowId={rowId}
                    // Last flashes yellow for 0.3s when it changes, then cuts straight back (no fade).
                    // Which column flashes is set in natgas.col-defs.ts; the colour in natgas.theme.ts.
                    cellFlashDuration={300}
                    cellFadeDuration={0}
                    defaultColDef={defaultColDef}
                    onGridReady={applyWidths}
                    onColumnResized={rememberWidths}
                    rowSelection={rowSelection}
                    suppressCellFocus
                />
            </div>
        </div>
    );

}
