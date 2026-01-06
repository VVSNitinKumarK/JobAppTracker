import { useEffect, useMemo, useRef, useState } from "react";
import { format } from "date-fns";
import {
    type ColumnDef,
    flexRender,
    getCoreRowModel,
    useReactTable,
    type RowSelectionState,
} from "@tanstack/react-table";
import { ExternalLink, Plus, Filter } from "lucide-react";

import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "../../components/ui/table";

import { useCompanies, useDeleteCompanies, useGetTags } from "./hooks";
import type { CompanyRow } from "./types";
import { AddCompanyDialog } from "./AddCompanyDialog";
import { UpdateCompanyDialog } from "./UpdateCompanyDialog";
import { DeleteCompaniesDialog } from "./DeleteCompaniesDialog";
import {
    CompaniesFilterDialog,
    type CompanyFilters as UICompanyFilters,
} from "./CompaniesFilterDialog";

import { cn } from "@/lib/utils";

function formatYmdOrDash(v: string | null) {
    if (!v) {
        return "-";
    }

    return format(new Date(`${v}T00:00:00`), "MM-dd-yyyy");
}

function IndeterminateCheckbox(properties: {
    checked: boolean;
    indeterminate: boolean;
    onChange: () => void;
    ariaLabel: string;
    className?: string;
    disabled?: boolean;
}) {
    const ref = useRef<HTMLInputElement | null>(null);

    useEffect(() => {
        if (!ref.current) {
            return;
        }

        ref.current.indeterminate = properties.indeterminate;
    }, [properties.indeterminate]);

    return (
        <input
            ref={ref}
            type="checkbox"
            aria-label={properties.ariaLabel}
            checked={properties.checked}
            onChange={properties.onChange}
            disabled={properties.disabled}
            className={
                properties.className ??
                "h-4 w-4 rounded border border-input bg-background align-middle"
            }
        />
    );
}

// eslint-disable-next-line @typescript-eslint/no-unused-expressions
("use no memo");

export function CompaniesTable() {
    const [query, setQuery] = useState("");
    const [page, setPage] = useState(0);
    const [size] = useState(50);

    const [uiFilters, setUiFilters] = useState<UICompanyFilters>({
        lastVisitedOnYmd: null,
        nextVisitOnYmd: null,
        tagsAny: [],
    });
    const [filterOpen, setFilterOpen] = useState(false);

    // Map UI filters to backend API parameters
    const apiFilters = useMemo(() => {
        return {
            page,
            size,
            q: query,
            // Use nextVisitOn date filter for backend 'date' parameter
            date: uiFilters.nextVisitOnYmd || undefined,
            // Use lastVisitedOn date filter for backend 'lastVisitedOn' parameter
            lastVisitedOn: uiFilters.lastVisitedOnYmd || undefined,
            // Use tags filter for backend 'tags' parameter
            tags: uiFilters.tagsAny && uiFilters.tagsAny.length > 0
                ? uiFilters.tagsAny
                : undefined,
        };
    }, [page, size, query, uiFilters]);

    const { data, isLoading, isError } = useCompanies(apiFilters);

    const items = useMemo(() => data?.items ?? [], [data]);
    const totalCount = data?.total ?? 0;

    // All filtering is now server-side!
    const filteredItems = items;

    const [rowSelection, setRowSelection] = useState<RowSelectionState>({});
    const anySelected = Object.keys(rowSelection).length > 0;

    const tagsQuery = useGetTags();
    const tagNameByKey = useMemo(() => {
        const map = new Map<string, string>();
        for (const t of tagsQuery.data ?? []) {
            if (!t.tagKey || !t.tagName) continue;
            map.set(t.tagKey, t.tagName);
        }
        return map;
    }, [tagsQuery.data]);

    useEffect(() => {
        setRowSelection({});
        setPage(0); // Reset to first page when query changes
    }, [query, uiFilters]);

    const columns = useMemo<ColumnDef<CompanyRow>[]>(
        () => [
            {
                id: "select",
                header: ({ table }) => {
                    const isAllPage = table.getIsAllPageRowsSelected();
                    const isSomePage = table.getIsSomePageRowsSelected();

                    return (
                        <div className="w-8 flex items-center justify-center">
                            <IndeterminateCheckbox
                                ariaLabel="Select all rows on this page"
                                checked={isAllPage}
                                indeterminate={!isAllPage && isSomePage}
                                onChange={() =>
                                    table.toggleAllPageRowsSelected()
                                }
                                disabled={table.getRowModel().rows.length === 0}
                                className={cn(
                                    "h-4 w-4 rounded border border-input bg-background",
                                    anySelected
                                        ? "visible"
                                        : "invisible group-hover:visible"
                                )}
                            />
                        </div>
                    );
                },
                cell: ({ row, table }) => {
                    const show =
                        Object.keys(table.getState().rowSelection).length > 0;

                    return (
                        <div className="w-8 flex items-center justify-center">
                            <input
                                type="checkbox"
                                aria-label={`Select ${row.original.companyName}`}
                                checked={row.getIsSelected()}
                                onChange={() => row.toggleSelected()}
                                className={cn(
                                    "h-4 w-4 rounded border border-input bg-background",
                                    show
                                        ? "visible"
                                        : "invisible group-hover:visible"
                                )}
                            />
                        </div>
                    );
                },
                enableSorting: false,
                enableHiding: false,
                size: 36,
            },
            {
                accessorKey: "companyName",
                header: "Company",
                cell: ({ row }) => (
                    <div className="font-medium">
                        {row.original.companyName}
                    </div>
                ),
            },
            {
                accessorKey: "lastVisitedOn",
                header: "Last Visited",
                cell: ({ row }) => (
                    <div className="tabular-nums">
                        {formatYmdOrDash(row.original.lastVisitedOn)}
                    </div>
                ),
            },
            {
                accessorKey: "nextVisitOn",
                header: "Next Visit",
                cell: ({ row }) => (
                    <div className="tabular-nums">
                        {formatYmdOrDash(row.original.nextVisitOn)}
                    </div>
                ),
            },
            {
                accessorKey: "tags",
                header: "Tags",
                cell: ({ row }) => {
                    const tags = row.original.tags ?? [];
                    if (tags.length === 0) {
                        return <span className="text-muted-foreground">-</span>;
                    }

                    return (
                        <div className="flex flex-wrap gap-1.5">
                            {tags.map((t) => (
                                <Badge
                                    key={t.tagKey}
                                    variant="secondary"
                                    className="rounded-full"
                                >
                                    {t.tagName}
                                </Badge>
                            ))}
                        </div>
                    );
                },
            },
            {
                id: "open",
                header: "",
                cell: ({ row }) => (
                    <Button variant="ghost" size="sm" asChild className="gap-2">
                        <a
                            href={row.original.careersUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            title="Open careers page"
                        >
                            <ExternalLink className="h-4 w-4" />
                            Open
                        </a>
                    </Button>
                ),
            },
        ],
        [anySelected]
    );

    // eslint-disable-next-line react-hooks/incompatible-library
    const table = useReactTable({
        data: filteredItems,
        columns,
        getCoreRowModel: getCoreRowModel(),
        enableRowSelection: true,
        getRowId: (row) => row.companyId,
        onRowSelectionChange: setRowSelection,
        state: { rowSelection },
    });

    const selectedRows = table.getSelectedRowModel().rows;
    const selectedCompanies = selectedRows.map((row) => row.original);
    const selectedCount = selectedCompanies.length;

    const [addOpen, setAddOpen] = useState(false);
    const [seedName, setSeedName] = useState<string>("");

    const [updateOpen, setUpdateOpen] = useState(false);
    const [deleteOpen, setDeleteOpen] = useState(false);

    const selectedForUpdate = selectedCount === 1 ? selectedCompanies[0] : null;

    const deleteMutation = useDeleteCompanies();

    const clearSelection = () => setRowSelection({});

    return (
        <section className="h-full w-full rounded-lg border bg-background p-4 flex flex-col overflow-hidden">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h2 className="text-base font-semibold">Companies</h2>
                    <div className="mt-1 text-xs text-muted-foreground">
                        Showing {filteredItems.length} of {totalCount} total
                    </div>
                </div>

                <div className="flex items-center gap-2 w-full">
                    <Input
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                        placeholder="Search company (starts with)..."
                        className="w-full"
                        onKeyDown={(event) => {
                            if (event.key === "Enter") {
                                const trimmed = query.trim();
                                if (
                                    trimmed.length > 0 &&
                                    items.length === 0 &&
                                    !isLoading
                                ) {
                                    event.preventDefault();
                                    setSeedName(trimmed);
                                    setAddOpen(true);
                                }
                            }
                        }}
                    />

                    <Button
                        type="button"
                        onClick={() => {
                            setSeedName(query.trim());
                            setAddOpen(true);
                        }}
                        className="h-9 w-9 p-0"
                        title="Add Company"
                    >
                        <Plus className="h-4 w-4" />
                    </Button>

                    <Button
                        type="button"
                        variant="outline"
                        onClick={() => setFilterOpen(true)}
                        className="h-9 w-9 p-0"
                        title="filters"
                    >
                        <Filter className="h-4 w-4" />
                    </Button>
                </div>
            </div>

            {selectedCount > 0 ? (
                <div className="mt-3 flex items-center justify-between rounded-md border bg-muted/30 px-3 py-2">
                    <div className="text-sm">
                        <span className="font-medium">{selectedCount}</span>{" "}
                        selected
                    </div>
                    <div className="flex items-center gap-2">
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            disabled={selectedCount !== 1}
                            onClick={() => setUpdateOpen(true)}
                        >
                            Update
                        </Button>
                        <Button
                            type="button"
                            variant="destructive"
                            size="sm"
                            disabled={selectedCount < 1}
                            onClick={() => setDeleteOpen(true)}
                        >
                            Delete
                        </Button>
                        <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={clearSelection}
                        >
                            Clear
                        </Button>
                    </div>
                </div>
            ) : null}

            {uiFilters.lastVisitedOnYmd ||
            uiFilters.nextVisitOnYmd ||
            (uiFilters.tagsAny?.length ?? 0) > 0 ? (
                <div className="mt-2 text-xs text-muted-foreground">
                    Filters:
                    {uiFilters.lastVisitedOnYmd
                        ? ` last=${uiFilters.lastVisitedOnYmd}`
                        : ""}
                    {uiFilters.nextVisitOnYmd
                        ? ` next=${uiFilters.nextVisitOnYmd}`
                        : ""}
                    {(uiFilters.tagsAny?.length ?? 0) > 0
                        ? ` tags=${uiFilters.tagsAny
                              .map((k) => tagNameByKey.get(k) ?? k)
                              .join(", ")}`
                        : ""}
                </div>
            ) : null}

            {!isLoading && query.trim().length > 0 && items.length === 0 ? (
                <button
                    type="button"
                    onClick={() => {
                        setSeedName(query.trim());
                        setAddOpen(true);
                    }}
                    className="mt-3 rounded-md border bg-blue-500/10 px-3 py-2 text-left text-sm hover:bg-blue-500/15"
                >
                    <span className="font-medium">Create new Company:</span>{" "}
                    <span className="font-mono">{query.trim()}</span>{" "}
                    <span className="text-xs text-muted-foreground">
                        (press Enter)
                    </span>
                </button>
            ) : null}

            <div className="mt-4 flex-1 overflow-auto rounded-md border">
                {isLoading ? (
                    <div className="p-4 text-sm text-muted-foreground">
                        Loading companies...
                    </div>
                ) : isError ? (
                    <div className="p-4 text-sm text-red-600">
                        Failed to load companies.
                    </div>
                ) : (
                    <Table>
                        <TableHeader>
                            {table.getHeaderGroups().map((headerGroup) => (
                                <TableRow
                                    key={headerGroup.id}
                                    className="group"
                                >
                                    {headerGroup.headers.map((header) => (
                                        <TableHead key={header.id}>
                                            {header.isPlaceholder
                                                ? null
                                                : flexRender(
                                                      header.column.columnDef
                                                          .header,
                                                      header.getContext()
                                                  )}
                                        </TableHead>
                                    ))}
                                </TableRow>
                            ))}
                        </TableHeader>

                        <TableBody>
                            {table.getRowModel().rows.length === 0 ? (
                                <TableRow>
                                    <TableCell
                                        colSpan={columns.length}
                                        className="h-24 text-center text-sm text-muted-foreground"
                                    >
                                        No results.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                table.getRowModel().rows.map((row) => (
                                    <TableRow key={row.id} className="group">
                                        {row.getVisibleCells().map((cell) => (
                                            <TableCell key={cell.id}>
                                                {flexRender(
                                                    cell.column.columnDef.cell,
                                                    cell.getContext()
                                                )}
                                            </TableCell>
                                        ))}
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                )}
            </div>

            {/* Pagination Controls */}
            {totalCount > size && (
                <div className="mt-3 flex items-center justify-between text-sm">
                    <div className="text-muted-foreground">
                        Page {page + 1} of {Math.ceil(totalCount / size)}
                    </div>
                    <div className="flex items-center gap-2">
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setPage((p) => Math.max(0, p - 1))}
                            disabled={page === 0 || isLoading}
                        >
                            Previous
                        </Button>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setPage((p) => p + 1)}
                            disabled={page >= Math.ceil(totalCount / size) - 1 || isLoading}
                        >
                            Next
                        </Button>
                    </div>
                </div>
            )}

            <AddCompanyDialog
                open={addOpen}
                onOpenChange={setAddOpen}
                initialName={seedName}
            />

            <UpdateCompanyDialog
                open={updateOpen}
                onOpenChange={(v) => {
                    setUpdateOpen(v);
                    if (!v) {
                        clearSelection();
                    }
                }}
                company={selectedForUpdate}
            />

            <DeleteCompaniesDialog
                open={deleteOpen}
                onOpenChange={setDeleteOpen}
                companyNames={selectedCompanies.map(
                    (company) => company.companyName
                )}
                isDeleting={deleteMutation.isPending}
                onConfirm={async () => {
                    const ids = selectedCompanies.map(
                        (company) => company.companyId
                    );
                    await deleteMutation.mutateAsync(ids);
                    setDeleteOpen(false);
                    clearSelection();
                }}
            />

            <CompaniesFilterDialog
                open={filterOpen}
                onOpenChange={setFilterOpen}
                value={uiFilters}
                onApply={(next) => {
                    setUiFilters(next);
                    setPage(0); // Reset to first page when filters change
                    clearSelection();
                }}
                onClear={() => {
                    setUiFilters({
                        lastVisitedOnYmd: null,
                        nextVisitOnYmd: null,
                        tagsAny: [],
                    });
                    setPage(0);
                    clearSelection();
                }}
            />
        </section>
    );
}
