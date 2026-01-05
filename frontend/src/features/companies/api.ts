import { api } from "../../lib/api";
import type { CompanyRow } from "./types";

export type CompaniesResponse = {
    items: CompanyRow[];
    page: number;
    size: number;
    total: number;
};

// Define possible response shapes from your API wrapper
type ApiResponse<T> = T | { data: T };

// Define possible backend response formats
type BackendArrayResponse = CompanyRow[];

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

type BackendResponse =
    | BackendArrayResponse
    | BackendObjectResponse
    | BackendSpringPageResponse;

// Helper: your api wrapper may return either `data` directly or `{ data }`
function unwrap<T>(res: ApiResponse<T>): T {
    return res && typeof res === "object" && "data" in res ? res.data : res;
}

export async function getCompanies(params?: {
    page?: number;
    size?: number;
    q?: string;
}) {
    const page = params?.page ?? 0;
    const size = params?.size ?? 50;
    const q = params?.q;

    const query = new URLSearchParams();
    query.set("page", String(page));
    query.set("size", String(size));
    if (q && q.trim().length > 0) query.set("q", q.trim());

    const res = (await api.get(
        `/companies?${query.toString()}`
    )) as ApiResponse<BackendResponse>;
    const data = unwrap<BackendResponse>(res);

    if (Array.isArray(data)) {
        return {
            items: data,
            page,
            size,
            total: data.length,
        } satisfies CompaniesResponse;
    }

    if ("items" in data && Array.isArray(data.items)) {
        return {
            items: data.items,
            page: Number(data.page ?? page),
            size: Number(data.size ?? size),
            total: Number(data.total ?? data.items.length),
        } satisfies CompaniesResponse;
    }

    if ("content" in data && Array.isArray(data.content)) {
        return {
            items: data.content,
            page: Number(data.number ?? page),
            size: Number(data.size ?? size),
            total: Number(data.totalElements ?? data.content.length),
        } satisfies CompaniesResponse;
    }

    return {
        items: [],
        page,
        size,
        total: 0,
    } satisfies CompaniesResponse;
}

export type CreateCompanyRequest = {
    companyName: string;
    careersUrl: string;
    lastVisitedOn?: string | null;
    revisitAfterDays?: number;
    tags?: string[];
};

export async function createCompany(
    payload: CreateCompanyRequest
): Promise<CompanyRow> {
    const response = await api.post<CompanyRow>(`/companies`, payload);
    return unwrap(response);
}

export type UpdateCompanyRequest = {
    companyName?: string;
    careersUrl?: string;
    lastVisitedOn?: string;
    revisitAfterDays?: number;
    tags?: string[];
};

export async function updateCompany(
    companyId: string,
    payload: UpdateCompanyRequest
): Promise<CompanyRow> {
    const response = await api.put<CompanyRow>(
        `/companies/${companyId}`,
        payload
    );
    return unwrap(response);
}

export async function deleteComnpany(companyId: string): Promise<void> {
    await api.delete(`/companies/${companyId}`);
}
