import { type ColumnDef } from "@tanstack/react-table";
import { ExternalLink } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { IndeterminateCheckbox } from "@/components/table/IndeterminateCheckbox";
import { cn } from "@/lib/utils";
import { formatYmdOrDash } from "@/lib/dateUtils";

import type { CompanyRow } from "./types";

export function createCompanyColumns(anySelected: boolean): ColumnDef<CompanyRow>[] {
    return [
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
    ];
}
