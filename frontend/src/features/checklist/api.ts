import { api } from "../../lib/api";
import type { ChecklistItem } from "./types";
import type { CompanyRow } from "../companies/types";

export function getChecklistByDate(date: string) {
    return api.get<ChecklistItem[]>(
        `/checklist?date=${encodeURIComponent(date)}`
    );
}

export function setChecklistCompleted(params: {
    date: string;
    companyId: string;
    completed: boolean;
}) {
    const { date, companyId, completed } = params;
    return api.put<void>(
        `/checklist/${encodeURIComponent(date)}/companies/${encodeURIComponent(
            companyId
        )}`,
        { completed }
    );
}

export function submitChecklist(date: string) {
    return api.post<CompanyRow[]>(
        `/checklist/${encodeURIComponent(date)}/submit`,
        {}
    );
}
