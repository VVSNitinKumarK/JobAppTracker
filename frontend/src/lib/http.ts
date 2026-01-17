export class HttpError extends Error {
    status: number;
    body: unknown;

    constructor(message: string, status: number, body: unknown) {
        super(message);
        this.name = "HttpError";
        this.status = status;
        this.body = body;
    }
}

type Json =
    | Record<string, unknown>
    | unknown[]
    | string
    | number
    | boolean
    | null;

async function readJsonSafely(response: Response): Promise<Json | null> {
    const text = await response.text();

    if (!text) {
        return null;
    }

    try {
        return JSON.parse(text) as Json;
    } catch {
        return text;
    }
}

export async function http<T = void>(
    input: RequestInfo | URL,
    init?: RequestInit
): Promise<T | undefined> {
    const method = init?.method?.toUpperCase() ?? "GET";
    const hasBody = method !== "GET" && method !== "HEAD";

    const response = await fetch(input, {
        ...init,
        headers: {
            ...(hasBody ? { "Content-Type": "application/json" } : {}),
            ...(init?.headers ?? {}),
        },
    });

    if (!response.ok) {
        const body = await readJsonSafely(response);
        throw new HttpError(`HTTP ${response.status}`, response.status, body);
    }

    const text = await response.text();
    if (!text) {
        return undefined;
    }

    return JSON.parse(text) as T;
}
