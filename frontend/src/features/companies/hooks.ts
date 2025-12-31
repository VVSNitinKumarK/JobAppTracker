import { useQuery } from "@tanstack/react-query";
import { getCompanies } from "./api";

export const companyKeys = {
    all: ["companies"] as const,
    list: (params: { page: number; size: number; q: string }) =>
        ["companies", "list", params] as const,
};

export function useCompanies(params: {
    page: number;
    size: number;
    q: string;
}) {
    return useQuery({
        queryKey: companyKeys.list(params),
        queryFn: () => getCompanies(params),
        staleTime: 30_000,
    });
}
