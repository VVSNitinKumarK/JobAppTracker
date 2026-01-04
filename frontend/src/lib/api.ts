import { APP_CONFIG } from "./config";
import { http } from "./http";

export function apiURL(path: string) {
    return `${APP_CONFIG.API_BASE_URL}${path}`;
}

const jsonHeaders = {
    "Content-Type": "application/json",
} as const;

export const api = {
    get: <T>(path: string) => http<T>(apiURL(path)),

    post: <T>(path: string, body: unknown) =>
        http<T>(apiURL(path), {
            method: "POST",
            headers: jsonHeaders,
            body: JSON.stringify(body),
        }),

    put: <T>(path: string, body: unknown) =>
        http<T>(apiURL(path), {
            method: "PUT",
            headers: jsonHeaders,
            body: JSON.stringify(body),
        }),
};
