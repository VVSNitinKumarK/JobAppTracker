import { format, isValid, parse } from "date-fns";

export function parseMMddyyyyToYmd(input?: string): string | null {
    const trimmed = (input ?? "").trim();
    if (!trimmed) {
        return null;
    }

    const date = parse(trimmed, "MM/dd/yyyy", new Date());
    if (!isValid(date)) {
        return null;
    }

    return format(date, "yyyy-MM-dd");
}

export function ymdToMmddyyyy(ymd: string): string {
    const date = new Date(`${ymd}T00:00:00`);
    return isValid(date) ? format(date, "MM/dd/yyyy") : "";
}

export function dateToYmd(date: Date): string {
    return format(date, "yyyy-MM-dd");
}

export function formatYmdOrDash(ymd: string | null): string {
    if (!ymd) {
        return "-";
    }

    return format(new Date(`${ymd}T00:00:00`), "MM/dd/yyyy");
}
