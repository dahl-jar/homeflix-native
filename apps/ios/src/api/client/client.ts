const MAX_ERROR_DETAIL_LENGTH = 500;

type QueryParams = Record<string, string | number | boolean>;
type RequestOptions = { signal?: AbortSignal };
type FetchResponse = Pick<Response, 'ok' | 'status' | 'json' | 'text'>;
type FetchFn = (url: string, options?: RequestInit) => Promise<FetchResponse>;

export type ApiClient = {
    baseUrl: string;
    get<T>(path: string, params?: QueryParams, options?: RequestOptions): Promise<T>;
    getText(path: string, params?: QueryParams): Promise<string>;
    mediaHeaders: Readonly<Record<string, string>>;
    post<T>(path: string, body: unknown, params?: QueryParams): Promise<T>;
    postNoContent(path: string, body: unknown, params?: QueryParams): Promise<void>;
};

export class ApiError extends Error {
    readonly status: number;
    readonly path: string;
    readonly detail: string | null;

    constructor(status: number, path: string, detail: string | null = null) {
        super(`request to ${path} failed with status ${status}${detail ? `: ${detail}` : ''}`);
        this.name = 'ApiError';
        this.status = status;
        this.path = path;
        this.detail = detail;
    }
}

function identityValue(value: string, field: string): string {
    const normalized = value.trim();
    if (!normalized || normalized.includes('"')) {
        throw new TypeError(`${field} must be a non-empty MediaBrowser identity value`);
    }
    return normalized;
}

function clientIdentity(deviceId: string, version: string): string {
    const safeDeviceId = identityValue(deviceId, 'deviceId');
    const safeVersion = identityValue(version, 'version');
    return `MediaBrowser Client="Homeflix", Device="iPhone", DeviceId="${safeDeviceId}", Version="${safeVersion}"`;
}

function jsonErrorDetail(value: unknown): string | null {
    if (typeof value === 'string') return value;
    if (!value || typeof value !== 'object') return null;
    const body = value as Record<string, unknown>;
    const detail = body.Message ?? body.message ?? body.ErrorMessage ?? body.error ?? body.detail;
    return typeof detail === 'string' ? detail : null;
}

function normalizeErrorDetail(body: string): string | null {
    const normalized = body.trim().replace(/\s+/g, ' ');
    if (!normalized || normalized.startsWith('<')) return null;
    try {
        const detail = jsonErrorDetail(JSON.parse(normalized));
        return detail?.slice(0, MAX_ERROR_DETAIL_LENGTH) ?? null;
    } catch {
        return normalized.slice(0, MAX_ERROR_DETAIL_LENGTH);
    }
}

async function errorDetail(response: FetchResponse): Promise<string | null> {
    try {
        return normalizeErrorDetail(await response.text());
    } catch {
        return null;
    }
}

export function createClient({
    baseUrl,
    token,
    deviceId,
    version,
    fetchFn = fetch
}: {
    baseUrl: string;
    token: string;
    deviceId: string;
    version: string;
    fetchFn?: FetchFn;
}): ApiClient {
    const identity = clientIdentity(deviceId, version);
    const authorization = token ? `${identity}, Token="${token}"` : identity;
    const headers = {
        'Content-Type': 'application/json',
        Authorization: authorization
    };
    const mediaHeaders = Object.freeze({ Authorization: authorization });

    const buildUrl = (path: string, params?: QueryParams) => {
        if (!params) return `${baseUrl}${path}`;
        const query = new URLSearchParams(
            Object.entries(params).map(([key, value]) => [key, String(value)])
        );
        return `${baseUrl}${path}?${query}`;
    };

    const request = async (
        path: string,
        {
            method,
            body,
            params,
            signal
        }: {
            method?: 'POST';
            body?: unknown;
            params?: QueryParams;
            signal?: AbortSignal;
        } = {}
    ) => {
        const response = await fetchFn(buildUrl(path, params), {
            headers,
            signal,
            ...(method ? { method } : {}),
            ...(body === undefined ? {} : { body: JSON.stringify(body) })
        });
        if (!response.ok) throw new ApiError(response.status, path, await errorDetail(response));
        return response;
    };

    return {
        baseUrl,
        async get<T>(path: string, params?: QueryParams, { signal }: RequestOptions = {}) {
            const response = await request(path, { params, signal });
            return response.json() as Promise<T>;
        },
        async getText(path: string, params?: QueryParams) {
            const response = await request(path, { params });
            return response.text();
        },
        mediaHeaders,
        async post<T>(path: string, body: unknown, params?: QueryParams) {
            const response = await request(path, { method: 'POST', body, params });
            return response.json() as Promise<T>;
        },
        async postNoContent(path: string, body: unknown, params?: QueryParams) {
            await request(path, { method: 'POST', body, params });
        }
    };
}
