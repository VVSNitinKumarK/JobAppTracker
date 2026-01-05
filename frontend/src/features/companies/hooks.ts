import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    getCompanies,
    createCompany,
    updateCompany,
    deleteComnpany,
    type CreateCompanyRequest,
    type UpdateCompanyRequest,
} from "./api";

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

export function useCreateCompany() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (payload: CreateCompanyRequest) => createCompany(payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["companies"] });
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
            queryClient.invalidateQueries({ queryKey: ["companies"] });
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
            queryClient.invalidateQueries({ queryKey: ["companies"] });
        },
    });
}
