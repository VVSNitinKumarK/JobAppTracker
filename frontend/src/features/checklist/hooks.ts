import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    getChecklistByDate,
    setChecklistCompleted,
    submitChecklist,
} from "./api";
import type { ChecklistItem } from "./types";

export const checklistKeys = {
    all: ["checklist"] as const,
    byDate: (date: string) => ["checklist", "date", date] as const,
};

export function useChecklist(date: string) {
    return useQuery({
        queryKey: checklistKeys.byDate(date),
        queryFn: () => getChecklistByDate(date),
    });
}

export function useToggleChecklistItem(date: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (vars: { companyId: string; completed: boolean }) => {
            await setChecklistCompleted({ date, ...vars });
        },

        onMutate: async (vars) => {
            await queryClient.cancelQueries({
                queryKey: checklistKeys.byDate(date),
            });

            const previousData = queryClient.getQueryData<ChecklistItem[]>(
                checklistKeys.byDate(date)
            );

            queryClient.setQueryData<ChecklistItem[]>(
                checklistKeys.byDate(date),
                (old) => {
                    if (!old) {
                        return old;
                    }

                    const idx = old.findIndex(
                        (x) => x.companyId === vars.companyId
                    );

                    if (idx > 0) {
                        return old.map((item) =>
                            item.companyId === vars.companyId
                                ? { ...item, completed: vars.completed }
                                : item
                        );
                    }

                    return old;
                }
            );

            return { previousData };
        },

        onError: (_error, _vars, context) => {
            if (context?.previousData) {
                queryClient.setQueryData(
                    checklistKeys.byDate(date),
                    context.previousData
                );
            }
        },

        onSettled: async () => {
            await queryClient.invalidateQueries({
                queryKey: checklistKeys.byDate(date),
            });
        },
    });
}

export function useSubmitChecklist(date: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async () => {
            await submitChecklist(date);
        },
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: checklistKeys.byDate(date),
            });

            await queryClient.invalidateQueries({
                queryKey: ["companies"],
            });
        },
    });
}
