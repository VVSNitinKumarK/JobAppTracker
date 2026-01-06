export type ChecklistItem = {
    companyId: string;
    companyName: string;
    careersUrl: string;
    lastVisitedOn: string | null;
    revisitAfterDays: number;
    tags?: TagDto[];
    nextVisitOn: string;
    createdAt: string;
    updatedAt: string;
    completed: boolean;
};

export type TagDto = {
    tagKey: string;
    tagName: string;
};
