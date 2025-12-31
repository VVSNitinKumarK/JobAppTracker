import { useQuery } from "@tanstack/react-query";
import { getMaxNextVisitOn } from "./meta";

export const metaKeys = {
    all: ["meta"] as const,
    maxNextVisitOn: () => ["meta", "maxNextVisitOn"] as const,
};

export function useMaxNextVisitOn() {
    return useQuery({
        queryKey: metaKeys.maxNextVisitOn(),
        queryFn: getMaxNextVisitOn,
        staleTime: 60_000,
    });
}
