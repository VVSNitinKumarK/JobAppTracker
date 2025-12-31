import { api } from "../../lib/api";
import type { CompanyRow } from "./types";

export type CompaniesResponse = {
    items: CompanyRow[];
    page: number;
    size: number;
    total: number;
};

// Helper: your api wrapper may return either `data` directly or `{ data }`
function unwrap<T>(res: any): T {
    return res && typeof res === "object" && "data" in res
        ? (res.data as T)
        : (res as T);
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

    const res = await api.get(`/companies?${query.toString()}`);
    const data = unwrap<any>(res);

    // Case 1: backend returns plain array
    if (Array.isArray(data)) {
        return {
            items: data as CompanyRow[],
            page,
            size,
            total: data.length,
        } satisfies CompaniesResponse;
    }

    // Case 2: backend returns { items, page, size, total }
    if (data && Array.isArray(data.items)) {
        return {
            items: data.items as CompanyRow[],
            page: Number(data.page ?? page),
            size: Number(data.size ?? size),
            total: Number(data.total ?? data.items.length),
        } satisfies CompaniesResponse;
    }

    // Case 3: Spring Page style: { content: [], totalElements, number, size }
    if (data && Array.isArray(data.content)) {
        return {
            items: data.content as CompanyRow[],
            page: Number(data.number ?? page),
            size: Number(data.size ?? size),
            total: Number(data.totalElements ?? data.content.length),
        } satisfies CompaniesResponse;
    }

    // Fallback
    return {
        items: [],
        page,
        size,
        total: 0,
    } satisfies CompaniesResponse;
}
