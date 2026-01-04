import { useMemo, useState } from "react";
import { format } from "date-fns";
import {
    type ColumnDef,
    flexRender,
    getCoreRowModel,
    useReactTable,
} from "@tanstack/react-table";
import { ExternalLink, Plus } from "lucide-react";

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

import { useCompanies } from "./hooks";
import type { CompanyRow } from "./types";
import { AddCompanyDialog } from "./AddCompanyDialog";

function formatYmdOrDash(v: string | null) {
    if (!v) {
        return "-";
    }

    return format(new Date(`${v}T00:00:00`), "yyyy-MM-dd");
}

// eslint-disable-next-line @typescript-eslint/no-unused-expressions
("use no memo");

export function CompaniesTable() {
    const [query, setQuery] = useState("");
    const page = 0;
    const size = 50;

    const { data, isLoading, isError } = useCompanies({ page, size, q: query });

    const items = data?.items ?? [];

    const columns = useMemo<ColumnDef<CompanyRow>[]>(
        () => [
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
                            {tags.map((tag) => (
                                <Badge
                                    key={tag}
                                    variant="secondary"
                                    className="rounded-full"
                                >
                                    {tag}
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
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() =>
                            window.open(
                                row.original.careersUrl,
                                "_blank",
                                "noopener,nonreferrer"
                            )
                        }
                        title="Open careers page"
                        className="gap-2"
                    >
                        <ExternalLink className="h-4 w-4" />
                        Open
                    </Button>
                ),
            },
        ],
        []
    );

    // eslint-disable-next-line react-hooks/incompatible-library
    const table = useReactTable({
        data: items,
        columns,
        getCoreRowModel: getCoreRowModel(),
    });

    const [addOpen, setAddOpen] = useState(false);
    const [seedName, setSeedName] = useState<string>("");

    return (
        <section className="h-full w-full rounded-lg border bg-background p-4 flex flex-col overflow-hidden">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h2 className="text-base font-semibold">Companies</h2>
                    <div className="mt-1 text-xs text-muted-foreground">
                        Showing {items.length}
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
                </div>
            </div>

            {!isLoading && query.trim().length > 0 && items.length === 0 ? (
                <button
                    type="button"
                    onClick={() => {
                        setSeedName(query.trim());
                        setAddOpen(true);
                    }}
                    className="rounded-md border bg-blue-500/10 px-3 py-2 text-left text-sm hover:bg-blue-500/15"
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
                                <TableRow key={headerGroup.id}>
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
                                    <TableRow key={row.id}>
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

            <AddCompanyDialog
                open={addOpen}
                onOpenChange={setAddOpen}
                initialName={seedName}
            />
        </section>
    );
}
