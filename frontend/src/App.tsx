import { useState } from "react";
import { addDays } from "date-fns";
import { CalendarDayPanel } from "./features/calendar/CalendarPanel";
import { TodayChecklist } from "./features/checklist/TodayChecklist";

function Header() {
    return <header className="h-12 border-b bg-background" />;
}

function Footer() {
    return <footer className="h-10 border-t bg-background" />;
}

function CompaniesTablePlaceHolder() {
    return (
        <section className="h-full w-full rounded-lg border bg-background p-4">
            <h2 className="text-base font-semibold">Companies Table</h2>
            <div className="mt-3 text-sm text-muted-foreground">
                Placeholder for Companies Table
            </div>
        </section>
    );
}

function McpSuggestionsPlaceholder() {
    return (
        <section className="h-full w-full rounded-lg border bg-background p-4">
            <h2 className="text-base font-semibold">MCP Suggestions</h2>
            <div className="mt-3 text-sm text-muted-foreground">
                Placeholder for MCP ideas / quick actions.
            </div>
        </section>
    );
}

export default function App() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const tomorrow = addDays(today, 1);
    const [selectedDate, setSelectedDate] = useState<Date>(tomorrow);

    return (
        <div className="min-h-screen bg-muted/20 text-foreground">
            <Header />

            <main className="w-full px-6 py-6">
                {/* Layout: 1/4 top, 3/4 bottom */}
                <div className="grid h-[calc(100vh-48px-40px-48px)] grid-rows-[1fr_3fr] gap-4">
                    {/* TOP ROW - 1/4 height, divided into 2 equal halves */}
                    <div className="grid grid-cols-2 gap-4">
                        {/* Left half: 1/3 Today Checklist, 2/3 Calendar Panel */}
                        <div className="grid grid-cols-[1fr_2fr] gap-4">
                            <div>
                                <TodayChecklist selectedDate={today} />
                            </div>

                            <div>
                                <CalendarDayPanel
                                    selectedDate={selectedDate}
                                    onSelectDate={setSelectedDate}
                                />
                            </div>
                        </div>

                        {/* Right half: MCP & LangChain */}
                        <div>
                            <McpSuggestionsPlaceholder />
                        </div>
                    </div>

                    {/* BOTTOM ROW - 3/4 height for table */}
                    <div>
                        <CompaniesTablePlaceHolder />
                    </div>
                </div>
            </main>

            <Footer />
        </div>
    );
}
