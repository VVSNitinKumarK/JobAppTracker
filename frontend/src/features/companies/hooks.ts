import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { STALE_TIME } from "@/lib/constants";
import {
    getCompanies,
    createCompany,
    updateCompany,
    deleteCompaniesBatch,
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
    list: (filters: CompanyFilters) => ["companies", "list", filters] as const,
};

export function useCompanies(params: CompanyFilters) {
    return useQuery({
        queryKey: companyKeys.list(params),
        queryFn: () => getCompanies(params),
        staleTime: STALE_TIME.COMPANIES,
    });
}

export function useCreateCompany() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: CreateCompanyRequest) => createCompany(payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: companyKeys.all });
            toast.success("Company added");
        },
        onError: () => {
            toast.error("Failed to add company");
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
            toast.success("Company updated");
        },
        onError: () => {
            toast.error("Failed to update company");
        },
    });
}

export function useDeleteCompanies() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (companyIds: string[]) => deleteCompaniesBatch(companyIds),
        onSuccess: (result) => {
            queryClient.invalidateQueries({ queryKey: companyKeys.all });
            const count = result.deleted;
            toast.success(count === 1 ? "Company deleted" : `${count} companies deleted`);
        },
        onError: () => {
            toast.error("Failed to delete companies");
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
        staleTime: STALE_TIME.TAGS,
    });
}
