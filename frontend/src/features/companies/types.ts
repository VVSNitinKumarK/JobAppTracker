export type CompanyRow = {
    companyId: string;
    companyName: string;
    careersUrl: string;
    lastVisitedOn: string | null;
    nextVisitOn: string | null;
    revisitAfterDays: number;
    tags?: TagDto[];
    createdAt: string;
    updatedAt: string;
};

export type TagDto = {
    tagKey: string;
    tagName: string;
};
