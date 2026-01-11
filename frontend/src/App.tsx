import { useState } from "react";
import { addDays } from "date-fns";
import { CalendarDayPanel } from "./features/calendar/CalendarPanel";
import { TodayChecklist } from "./features/checklist/TodayChecklist";
import { CompaniesTable } from "./features/companies/CompaniesTable";

function Header() {
    return <header className="h-12 border-b bg-background" />;
}

function Footer() {
    return <footer className="h-10 border-t bg-background" />;
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
                <div className="grid h-[calc(100vh-48px-40px-48px)] grid-rows-[1fr_3fr] gap-4">
                    <div className="grid grid-cols-2 gap-4">
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

                        <div>
                            <McpSuggestionsPlaceholder />
                        </div>
                    </div>

                    <div className="overflow-hidden">
                        <CompaniesTable />
                    </div>
                </div>
            </main>

            <Footer />
        </div>
    );
}
