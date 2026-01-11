import { format } from "date-fns";
import { Check, Plus, X } from "lucide-react";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";

import { getCompanies } from "../companies/api";
import type { CompanyRow } from "../companies/types";
import { type PickerCompany } from "./types";
import { cn } from "../../lib/utils";
import {
    useChecklist,
    useSubmitChecklist,
    useToggleChecklistItem,
} from "./hooks";
import type { ChecklistItem } from "./types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

type Properties = {
    selectedDate: Date;
};

function normalizePrefix(s: string) {
    return s.trim();
}

function toYmd(d: Date) {
    return format(d, "yyyy-MM-dd");
}

function groupItems(items: ChecklistItem[], selectedDateYmd: string) {
    const today: ChecklistItem[] = [];
    const overdue: ChecklistItem[] = [];
    const added: ChecklistItem[] = [];

    for (const it of items) {
        // "Due" logic
        const due = it.nextVisitOn != null && it.nextVisitOn <= selectedDateYmd;

        if (due) {
            if (it.nextVisitOn === selectedDateYmd) today.push(it);
            else overdue.push(it);
            continue;
        }

        // Not due, but manually added to this date
        if (it.inChecklist) {
            added.push(it);
        }
    }

    return { today, overdue, added };
}

function ChecklistPill(props: {
    item: ChecklistItem;
    tint: "today" | "overdue";
    onToggle: (next: boolean) => void;
    toggling?: boolean;
}) {
    const { item, tint, onToggle, toggling } = props;

    return (
        <div className="flex items-center gap-2">
            {/* Checkbox (NOT struck) */}
            <button
                type="button"
                aria-label={
                    item.completed ? "Mark incomplete" : "Mark complete"
                }
                disabled={toggling}
                onClick={() => onToggle(!item.completed)}
                className={cn(
                    "grid h-5 w-5 place-items-center rounded-[5px] border transition shrink-0",
                    item.completed
                        ? "bg-foreground text-background border-foreground"
                        : "bg-background border-foreground/40 hover:border-foreground/70",
                    toggling && "cursor-not-allowed opacity-60"
                )}
            >
                {item.completed ? <Check className="h-4 w-4" /> : null}
            </button>

            <div className="relative inline-block">
                <a
                    href={item.careersUrl}
                    target="_blank"
                    rel="noreferrer"
                    className={cn(
                        "relative inline-flex items-center rounded-full px-3 py-1.5 text-sm leading-none border whitespace-nowrap",
                        tint === "today"
                            ? "bg-muted/60 border-border hover:bg-muted/80"
                            : "bg-red-500/10 border-red-500/20 hover:bg-red-500/15",
                        item.completed ? "opacity-50" : "opacity-100",
                        "transition-opacity",
                        item.completed
                            ? "line-through decoration-foreground/70"
                            : "no-underline",
                        "hover:underline underline-offset-2"
                    )}
                >
                    {item.companyName}
                </a>
            </div>
        </div>
    );
}

export function TodayChecklist({ selectedDate }: Properties) {
    const dateYmd = toYmd(selectedDate);

    const { data, isLoading, isError } = useChecklist(dateYmd);
    const toggle = useToggleChecklistItem(dateYmd);

    const submit = useSubmitChecklist(dateYmd);

    const items = useMemo(() => data ?? [], [data]);
    const { today, overdue, added } = groupItems(items, dateYmd);

    const [addingOpen, setAddingOpen] = useState(false);
    const [search, setSearch] = useState("");

    const existingIds = useMemo(
        () => new Set(items.map((x) => x.companyId)),
        [items]
    );

    const searchTerm = normalizePrefix(search);

    const companySearch = useQuery({
        queryKey: ["companies", "picker", searchTerm],
        enabled: addingOpen && searchTerm.length > 0,
        queryFn: async () => {
            const response = await getCompanies({
                q: searchTerm,
                page: 0,
                size: 10,
            });

            return response.items
                .map(
                    (x: CompanyRow): PickerCompany => ({
                        companyId: x.companyId,
                        companyName: x.companyName,
                        careersUrl: x.careersUrl,
                    })
                )
                .filter(
                    (company) =>
                        company.companyId &&
                        company.companyName &&
                        company.careersUrl
                );
        },
    });

    const pickerOptions = useMemo(() => {
        const all = companySearch.data ?? [];

        return all.filter((c) => !existingIds.has(c.companyId));
    }, [companySearch.data, existingIds]);

    const completedCount = items.filter((x) => x.completed).length;
    const canSubmit = completedCount > 0 && !isLoading && !isError;

    return (
        <section className="h-full w-full rounded-lg border bg-background p-4 flex flex-col">
            <div className="flex items-start justify-between gap-3">
                <div>
                    <h2 className="text-base font-semibold">Check Today</h2>
                    <div className="mt-1 text-xs text-muted-foreground">
                        {dateYmd}
                    </div>
                    {completedCount > 0 ? (
                        <div className="mt-1 text-xs text-muted-foreground">
                            {completedCount} marked complete
                        </div>
                    ) : null}
                </div>

                <div className="flex items-center gap-2">
                    <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={() => {
                            setAddingOpen((v) => !v);
                            setSearch("");
                        }}
                    >
                        {addingOpen ? (
                            <X className="h-4 w-4" />
                        ) : (
                            <Plus className="h-4 w-4" />
                        )}
                    </Button>
                </div>
            </div>

            {/* Add company picker (fixed space, dropdown overlays) */}
            {addingOpen ? (
                <div className="mt-3">
                    <div className="relative">
                        <Input
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Add company to today's checklist..."
                            className="h-8"
                        />

                        {/* Dropdown overlay */}
                        {searchTerm.length > 0 ? (
                            <div
                                className={cn(
                                    "absolute left-0 right-0 mt-2 z-50",
                                    "rounded-md border bg-background shadow-md",
                                    "max-h-40 overflow-y-auto" // keeps it from growing forever
                                )}
                            >
                                {companySearch.isLoading ? (
                                    <div className="text-xs text-muted-foreground px-3 py-2">
                                        Searching...
                                    </div>
                                ) : pickerOptions.length === 0 ? (
                                    <div className="text-xs text-muted-foreground px-3 py-2">
                                        No companies found (or already in
                                        today's list).
                                    </div>
                                ) : (
                                    <div className="flex flex-col">
                                        {pickerOptions.map(
                                            (c: PickerCompany) => (
                                                <button
                                                    key={c.companyId}
                                                    type="button"
                                                    disabled={toggle.isPending}
                                                    className={cn(
                                                        "text-left px-3 py-2 hover:bg-muted text-sm",
                                                        toggle.isPending &&
                                                            "cursor-not-allowed opacity-50"
                                                    )}
                                                    onClick={async () => {
                                                        await toggle.mutateAsync(
                                                            {
                                                                companyId:
                                                                    c.companyId,
                                                                completed:
                                                                    false,
                                                            }
                                                        );

                                                        setSearch("");
                                                        setAddingOpen(false);
                                                    }}
                                                >
                                                    <div className="font-medium">
                                                        {c.companyName}
                                                    </div>
                                                </button>
                                            )
                                        )}
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="mt-2 text-xs text-muted-foreground px-1">
                                Type to search companies...
                            </div>
                        )}
                    </div>
                </div>
            ) : null}

            {/* Scroll area wrapper takes remaining height */}
            <div className="mt-4 flex-1">
                <div className="space-y-5 max-h-[260px] overflow-y-auto pr-1">
                    {isLoading ? (
                        <div className="text-sm text-muted-foreground">
                            Loading checklist…
                        </div>
                    ) : null}

                    {isError ? (
                        <div className="text-sm text-red-600">
                            Failed to load checklist.
                        </div>
                    ) : null}

                    {!isLoading && !isError ? (
                        <>
                            <div>
                                <div className="mb-2 text-sm font-semibold text-red-700">
                                    Overdue
                                </div>
                                {overdue.length === 0 ? (
                                    <div className="text-sm text-muted-foreground">
                                        No overdue items.
                                    </div>
                                ) : (
                                    <div className="flex flex-col gap-2">
                                        {overdue.map((it) => (
                                            <ChecklistPill
                                                key={it.companyId}
                                                item={it}
                                                tint="overdue"
                                                toggling={toggle.isPending}
                                                onToggle={(next) =>
                                                    toggle.mutate({
                                                        companyId: it.companyId,
                                                        completed: next,
                                                    })
                                                }
                                            />
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div>
                                <div className="mb-2 text-sm font-semibold">
                                    Today
                                </div>
                                {today.length === 0 ? (
                                    <div className="text-sm text-muted-foreground">
                                        No items due today.
                                    </div>
                                ) : (
                                    <div className="flex flex-col gap-2">
                                        {today.map((it) => (
                                            <ChecklistPill
                                                key={it.companyId}
                                                item={it}
                                                tint="today"
                                                toggling={toggle.isPending}
                                                onToggle={(next) =>
                                                    toggle.mutate({
                                                        companyId: it.companyId,
                                                        completed: next,
                                                    })
                                                }
                                            />
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div>
                                <div className="mb-2 text-sm font-semibold">
                                    Added
                                </div>
                                {added.length === 0 ? (
                                    <div className="text-sm text-muted-foreground">
                                        No manually added items.
                                    </div>
                                ) : (
                                    <div className="flex flex-col gap-2">
                                        {added.map((item) => (
                                            <ChecklistPill
                                                key={item.companyId}
                                                item={item}
                                                tint="today"
                                                toggling={toggle.isPending}
                                                onToggle={(next) =>
                                                    toggle.mutate({
                                                        companyId:
                                                            item.companyId,
                                                        completed: next,
                                                    })
                                                }
                                            />
                                        ))}
                                    </div>
                                )}
                            </div>
                        </>
                    ) : null}
                </div>
            </div>

            {/* Footer pinned to bottom */}
            <div className="mt-auto pt-3 flex justify-end border-t">
                <Button
                    type="button"
                    size="sm"
                    disabled={!canSubmit || submit.isPending}
                    onClick={async () => {
                        await submit.mutateAsync();
                    }}
                >
                    {submit.isPending ? "Submitting..." : "Submit"}
                </Button>
            </div>
        </section>
    );
}
