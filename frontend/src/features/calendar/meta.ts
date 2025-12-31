import { api } from "../../lib/api";

export type MaxNextVisitOnResponse = {
    maxNextVisitOn: string | null;
};

export function getMaxNextVisitOn() {
    return api.get<MaxNextVisitOnResponse>("/meta/max-next-visit-on");
}
