import { TodayChecklist } from "./features/checklist/TodayChecklist";

function Header() {
    return <header className="h-12 border-b bg-background" />;
}

function Footer() {
    return <footer className="h-10 border-t bg-background" />;
}

function CalendarDayPlaceHolder() {
    return (
        <section className="h-full w-full rounded-lg border bg-background p-4">
            <h2 className="text-base font-semibold">Calendar + Day List</h2>
            <div className="mt-3 text-sm text-muted-foreground">
                Placeholder for Calendar and Day List
            </div>
        </section>
    );
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

export default function App() {
    const selectedDate = new Date();

    return (
        <div className="min-h-screen bg-muted/20 test-foreground">
            <Header />

            <main className="mx-auto max-6-wxl px-6 py-6">
                <div className="flex h-[calc(100vh-48px-40px-32px)] flex-col gap-4">
                    <div className="flex basis-1/4 flex-row gap-4">
                        <div className="basis-1/3">
                            <TodayChecklist selectedDate={selectedDate} />
                        </div>

                        <div className="basis-2/3">
                            <CalendarDayPlaceHolder />
                        </div>
                    </div>

                    <div className="flex basis-3/4">
                        <CompaniesTablePlaceHolder />
                    </div>
                </div>
            </main>

            <Footer />
        </div>
    );
}
