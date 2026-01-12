import { api } from "../../lib/api";
import type { CompanyRow } from "./types";
import type { TagDto } from "@/types/api";

export type CompaniesResponse = {
    items: CompanyRow[];
    page: number;
    size: number;
    total: number;
};

export type DueFilter = "today" | "overdue" | "upcoming";

export async function getCompanies(params?: {
    page?: number;
    size?: number;
    q?: string;
    tags?: string[]; // Array of tag keys to filter by (OR logic)
    due?: DueFilter; // Filter by due status
    date?: string; // Filter by specific date (yyyy-MM-dd)
    lastVisitedOn?: string; // Filter by last visited date (yyyy-MM-dd)
}): Promise<CompaniesResponse> {
    const page = params?.page ?? 0;
    const size = params?.size ?? 50;
    const q = params?.q;
    const tags = params?.tags;
    const due = params?.due;
    const date = params?.date;
    const lastVisitedOn = params?.lastVisitedOn;

    const query = new URLSearchParams();
    query.set("page", String(page));
    query.set("size", String(size));
    if (q && q.trim().length > 0) query.set("q", q.trim());
    if (tags && tags.length > 0) query.set("tags", tags.join(","));
    if (due) query.set("due", due);
    if (date) query.set("date", date);
    if (lastVisitedOn) query.set("lastVisitedOn", lastVisitedOn);

    return (await api.get(
        `/companies?${query.toString()}`
    )) as CompaniesResponse;
}

export type CreateCompanyRequest = {
    companyName: string;
    careersUrl: string;
    lastVisitedOn?: string | null;
    revisitAfterDays?: number;
    tags?: string[]; // tag keys
};

export async function createCompany(
    payload: CreateCompanyRequest
): Promise<CompanyRow> {
    return (await api.post(`/companies`, payload)) as CompanyRow;
}

export type UpdateCompanyRequest = {
    companyName?: string;
    careersUrl?: string;
    lastVisitedOn?: string | null;
    revisitAfterDays?: number;
    tags?: string[]; // tag keys
};

export async function updateCompany(
    companyId: string,
    payload: UpdateCompanyRequest
): Promise<CompanyRow> {
    return (await api.put(`/companies/${companyId}`, payload)) as CompanyRow;
}

export async function deleteCompany(companyId: string): Promise<void> {
    await api.delete(`/companies/${companyId}`);
}

export async function getTags(): Promise<TagDto[]> {
    return (await api.get(`/tags`)) as TagDto[];
}
