import { APP_CONFIG } from "./config";
import { http } from "./http";

export function apiURL(path: string) {
    return `${APP_CONFIG.API_BASE_URL}${path}`;
}

export const api = {
    get: <T>(path: string) => http<T>(apiURL(path)),
    put: <T>(path: string, body: unknown) =>
        http<T>(apiURL(path), { method: "PUT", body: JSON.stringify(body) }),
};
