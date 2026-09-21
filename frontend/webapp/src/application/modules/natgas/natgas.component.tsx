import { AgGridReact } from "ag-grid-react";
import { useRef } from "react";


/**
 * Dumb UI only:
 * data + colDefs → DataGrid
 */
export function NatGasComponent({
    data,
    colDefs
}: {
    data: any[];
    colDefs: any[];
}) {
    const gridRef = useRef<AgGridReact>(null);

    return (
        <div className="ag-theme-alpine" style={{ height: "70vh" }}>
            <AgGridReact
                ref={gridRef}
                rowData={data}
                columnDefs={colDefs}
            />
        </div>
    );

}