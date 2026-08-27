export class ApiError extends Error {
    constructor(status, path) {
        super(`request to ${path} failed with status ${status}`);
        this.name = 'ApiError';
        this.status = status;
    }
}

const CLIENT_IDENTITY =
    'MediaBrowser Client="Homeflix", Device="iPhone", DeviceId="homeflix-native", Version="1.0.0"';

/**
 * Transport-only Jellyfin client: builds urls, attaches the MediaBrowser
 * identity (+ token) header, parses JSON, and translates non-2xx into
 * ApiError. Jellyfin rejects auth calls that lack the identity fields.
 */
export function createClient({ baseUrl, token, fetchFn = fetch }) {
    const authorization = token ? `${CLIENT_IDENTITY}, Token="${token}"` : CLIENT_IDENTITY;
    const headers = {
        'Content-Type': 'application/json',
        Authorization: authorization
    };
    const mediaHeaders = Object.freeze({ Authorization: authorization });

    const buildUrl = (path, params) => {
        if (!params) return `${baseUrl}${path}`;
        const query = new URLSearchParams(
            Object.entries(params).map(([key, value]) => [key, String(value)])
        );
        return `${baseUrl}${path}?${query}`;
    };

    const parse = async (response, path) => {
        if (!response.ok) throw new ApiError(response.status, path);
        return response.json();
    };

    const parseText = async (response, path) => {
        if (!response.ok) throw new ApiError(response.status, path);
        return response.text();
    };

    return {
        baseUrl,
        async get(path, params, { signal } = {}) {
            const response = await fetchFn(buildUrl(path, params), { headers, signal });
            return parse(response, path);
        },
        async getText(path, params) {
            const response = await fetchFn(buildUrl(path, params), { headers });
            return parseText(response, path);
        },
        mediaHeaders,
        async post(path, body, params) {
            const response = await fetchFn(buildUrl(path, params), {
                method: 'POST',
                headers,
                body: JSON.stringify(body)
            });
            return parse(response, path);
        },
        async postNoContent(path, body, params) {
            const response = await fetchFn(buildUrl(path, params), {
                method: 'POST',
                headers,
                body: JSON.stringify(body)
            });
            if (!response.ok) throw new ApiError(response.status, path);
        }
    };
}
