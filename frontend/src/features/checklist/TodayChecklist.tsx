import { format } from "date-fns";
import { Check } from "lucide-react";
import { cn } from "../../lib/utils";
import { useChecklist, useToggleChecklistItem } from "./hooks";
import type { ChecklistItem } from "./types";

type Props = {
    selectedDate: Date;
};

function toYmd(d: Date) {
    return format(d, "yyyy-MM-dd");
}

function groupItems(items: ChecklistItem[], selectedDateYmd: string) {
    const today: ChecklistItem[] = [];
    const overdue: ChecklistItem[] = [];

    for (const it of items) {
        if (it.nextVisitOn < selectedDateYmd) overdue.push(it);
        else if (it.nextVisitOn === selectedDateYmd) today.push(it);
    }

    return { today, overdue };
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

            {/* Pill ONLY for text; strike applies ONLY here */}
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

export function TodayChecklist({ selectedDate }: Props) {
    const dateYmd = toYmd(selectedDate);

    const { data, isLoading, isError } = useChecklist(dateYmd);
    const toggle = useToggleChecklistItem(dateYmd);

    const items = data ?? [];
    const { today, overdue } = groupItems(items, dateYmd);

    return (
        <section className="h-full w-full rounded-lg border bg-background p-4">
            <div className="flex items-start justify-between gap-3">
                <div>
                    <h2 className="text-base font-semibold">Check Today</h2>
                    <div className="mt-1 text-xs text-muted-foreground">
                        {dateYmd}
                    </div>
                </div>
            </div>

            <div className="mt-4 space-y-5 max-h-[260px] overflow-y-auto pr-1">
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
                    </>
                ) : null}
            </div>
        </section>
    );
}
