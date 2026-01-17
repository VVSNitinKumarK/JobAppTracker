export const STALE_TIME = {
    COMPANIES: 30_000,
    CHECKLIST: 30_000,
    TAGS: 5 * 60_000,
    META: 60_000,
} as const;

export const UI = {
    PAGE_SIZE: 50,
    CHECKLIST_MAX_HEIGHT: 260,
} as const;