import type { TagDto } from "@/types/api";

export type ChecklistItem = {
    companyId: string;
    companyName: string;
    careersUrl: string;
    lastVisitedOn: string | null;
    revisitAfterDays: number;
    tags?: TagDto[];
    nextVisitOn: string | null;
    createdAt: string;
    updatedAt: string;
    completed: boolean;
    inChecklist: boolean;
};

export type PickerCompany = {
    companyId: string;
    companyName: string;
    careersUrl: string;
};
