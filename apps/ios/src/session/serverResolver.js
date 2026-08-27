export function parseServerCandidates(value) {
    return (value ?? '')
        .split(',')
        .map((candidate) => candidate.trim().replace(/\/+$/, ''))
        .filter(Boolean);
}

export const SERVER_CANDIDATES = parseServerCandidates(
    process.env.EXPO_PUBLIC_HOMEFLIX_SERVER_URLS
);

const PROBE_PATH = '/System/Info/Public';
const PROBE_TIMEOUT_MS = 2000;

export async function defaultProbe(baseUrl) {
    try {
        const response = await fetch(`${baseUrl}${PROBE_PATH}`, {
            signal: AbortSignal.timeout(PROBE_TIMEOUT_MS)
        });
        return response.ok;
    } catch {
        return false;
    }
}

export async function resolveServer(candidates = SERVER_CANDIDATES, probe = defaultProbe) {
    const results = await Promise.all(candidates.map((url) => probe(url)));
    const index = results.findIndex(Boolean);
    return index === -1 ? null : candidates[index];
}
