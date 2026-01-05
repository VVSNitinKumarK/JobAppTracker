export type CompanyRow = {
    companyId: string;
    companyName: string;
    careersUrl: string;
    lastVisitedOn: string | null;
    nextVisitOn: string | null;
    revisitAfterDays: number;
    tags: string[];
};
