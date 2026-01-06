import { api } from "../../lib/api";
import type { CompanyRow, TagDto } from "./types";

export type CompaniesResponse = {
    items: CompanyRow[];
    page: number;
    size: number;
    total: number;
};

type ApiResponse<T> = T | { data: T };

function isObject(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null;
}

function unwrap<T>(res: ApiResponse<T>): T {
    if (isObject(res) && "data" in res) {
        return (res as { data: T }).data;
    }
    return res as T;
}

type BackendObjectResponse = {
    items: CompanyRow[];
    page?: number;
    size?: number;
    total?: number;
};

type BackendSpringPageResponse = {
    content: CompanyRow[];
    totalElements?: number;
    number?: number;
    size?: number;
};

function isBackendObjectResponse(x: unknown): x is BackendObjectResponse {
    return (
        isObject(x) &&
        "items" in x &&
        Array.isArray((x as Record<string, unknown>).items)
    );
}

function isBackendSpringPageResponse(
    x: unknown
): x is BackendSpringPageResponse {
    return (
        isObject(x) &&
        "content" in x &&
        Array.isArray((x as Record<string, unknown>).content)
    );
}

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

    const res = (await api.get(
        `/companies?${query.toString()}`
    )) as ApiResponse<unknown>;

    const data = unwrap(res);

    if (Array.isArray(data)) {
        // assume CompanyRow[]
        const items = data as CompanyRow[];
        return { items, page, size, total: items.length };
    }

    if (isBackendObjectResponse(data)) {
        return {
            items: data.items,
            page: Number(data.page ?? page),
            size: Number(data.size ?? size),
            total: Number(data.total ?? data.items.length),
        };
    }

    if (isBackendSpringPageResponse(data)) {
        return {
            items: data.content,
            page: Number(data.number ?? page),
            size: Number(data.size ?? size),
            total: Number(data.totalElements ?? data.content.length),
        };
    }

    return { items: [], page, size, total: 0 };
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
    const res = (await api.post(
        `/companies`,
        payload
    )) as ApiResponse<CompanyRow>;
    return unwrap(res);
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
    const res = (await api.put(
        `/companies/${companyId}`,
        payload
    )) as ApiResponse<CompanyRow>;
    return unwrap(res);
}

export async function deleteCompany(companyId: string): Promise<void> {
    await api.delete(`/companies/${companyId}`);
}

// keep old name if already used elsewhere
export const deleteComnpany = deleteCompany;

export async function getTags(): Promise<TagDto[]> {
    const res = (await api.get(`/tags`)) as ApiResponse<TagDto[]>;
    return unwrap(res);
}
