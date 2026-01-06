import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    getCompanies,
    createCompany,
    updateCompany,
    deleteComnpany,
    getTags,
    type CreateCompanyRequest,
    type UpdateCompanyRequest,
    type DueFilter,
} from "./api";

export type CompanyFilters = {
    page: number;
    size: number;
    q: string;
    tags?: string[];
    due?: DueFilter;
    date?: string;
    lastVisitedOn?: string;
};

export const companyKeys = {
    all: ["companies"] as const,
    list: (filters: CompanyFilters) =>
        ["companies", "list", filters] as const,
};

export function useCompanies(params: CompanyFilters) {
    return useQuery({
        queryKey: companyKeys.list(params),
        queryFn: () => getCompanies(params),
        staleTime: 30_000,
    });
}

export function useCreateCompany() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: CreateCompanyRequest) => createCompany(payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: companyKeys.all });
        },
    });
}

export function useUpdateCompany() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (args: {
            companyId: string;
            payload: UpdateCompanyRequest;
        }) => updateCompany(args.companyId, args.payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: companyKeys.all });
        },
    });
}

export function useDeleteCompanies() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (companyIds: string[]) => {
            await Promise.all(companyIds.map((id) => deleteComnpany(id)));
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: companyKeys.all });
        },
    });
}

export const tagKeys = {
    all: ["tags"] as const,
    list: () => ["tags", "list"] as const,
};

export function useGetTags() {
    return useQuery({
        queryKey: tagKeys.list(),
        queryFn: getTags,
        staleTime: 5 * 60_000,
    });
}
