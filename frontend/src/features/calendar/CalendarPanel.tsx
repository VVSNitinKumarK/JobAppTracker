import { format, isBefore, startOfMonth, subMonths } from "date-fns";
import { Calendar } from "../../components/ui/calendar";
import { cn } from "../../lib/utils";
import { useChecklist } from "../checklist/hooks";
import { useMaxNextVisitOn } from "./hooks";

type Properties = {
    selectedDate: Date;
    onSelectDate: (date: Date) => void;
};

function toYmd(date: Date) {
    return format(date, "yyyy-MM-dd");
}

function startOfMonthSafe(date: Date) {
    return startOfMonth(date);
}

export function CalendarDayPanel({ selectedDate, onSelectDate }: Properties) {
    const selectedYmd = toYmd(selectedDate);

    const maxMeta = useMaxNextVisitOn();
    const maxNextVisitOn = maxMeta.data?.maxNextVisitOn ?? null;

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const fromMonth = startOfMonthSafe(subMonths(today, 1));
    const toMonth = maxNextVisitOn
        ? startOfMonthSafe(new Date(`${maxNextVisitOn}T00:00:00`))
        : startOfMonthSafe(today);

    const checklist = useChecklist(selectedYmd);
    const items = checklist.data ?? [];

    const isPast = isBefore(selectedDate, today);

    const filtered = items.filter((item) =>
        isPast ? item.completed === true : item.completed === false
    );

    const heading = isPast ? "Completed" : "To Do";

    return (
        <section className="w-full rounded-lg border bg-background p-4">
            <div className="grid grid-cols-2 gap-4">
                <div className="flex flex-col">
                    <div className="mb-1 text-sm font-semibold">Calendar</div>

                    <div className="mb-2 text-xs text-muted-foreground">
                        {maxMeta.isLoading ? (
                            "loading range..."
                        ) : (
                            <>
                                Range: {format(fromMonth, "MMM yyyy")} -{" "}
                                {format(toMonth, "MMM yyyy")}
                            </>
                        )}
                    </div>

                    <div className="flex w-full items-center justify-center rounded-md border p-2">
                        <Calendar
                            mode="single"
                            selected={selectedDate}
                            onSelect={(date) => {
                                if (!date) {
                                    return;
                                }

                                const next = new Date(date);
                                next.setHours(0, 0, 0, 0);
                                onSelectDate(next);
                            }}
                            defaultMonth={startOfMonthSafe(selectedDate)}
                            fromMonth={fromMonth}
                            toMonth={toMonth}
                            className="p-0"
                        />
                    </div>
                </div>

                <div className="flex flex-col">
                    <div className="flex items-start justify-between gap-3">
                        <div>
                            <div className="text-sm font-semibold">Day</div>
                            <div className="mt-1 text-xs text-muted-foreground">
                                {selectedYmd}
                            </div>
                        </div>
                    </div>

                    <div className="mt-3 flex-1 rounded-md border p-3">
                        <div
                            className={cn(
                                "text-sm font-semibold",
                                isPast ? "text-foreground" : "text-foreground"
                            )}
                        >
                            {heading}
                        </div>

                        <div className="mt-2">
                            {checklist.isLoading ? (
                                <div className="text-sm text-muted-foreground">
                                    Loading...
                                </div>
                            ) : checklist.isError ? (
                                <div className="text-sm text-red-600">
                                    Failed to load day list.
                                </div>
                            ) : filtered.length === 0 ? (
                                <div className="text-sm text-muted-foreground">
                                    No items.
                                </div>
                            ) : (
                                <ul className="list-disc space-y-1 pl-5 text-sm">
                                    {filtered.map(
                                        (
                                            item // Changed: Added parentheses for implicit return
                                        ) => (
                                            <li key={item.companyId}>
                                                <a
                                                    href={item.careersUrl}
                                                    target="_blank"
                                                    rel="noreferrer"
                                                    className="hover:underline underline-offset-2"
                                                >
                                                    {item.companyName}
                                                </a>
                                            </li>
                                        )
                                    )}
                                </ul>
                            )}
                        </div>

                        <div className="mt-3 text-xs text-muted-foreground">
                            {isPast
                                ? "Showing completed items for a past date."
                                : "Showing remaining items for today/future."}
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}
