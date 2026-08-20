export const SERVER_CANDIDATES = ['http://homeflix.invalid:8096', 'http://homeflix.invalid:8096'];

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

/** Probes all candidates concurrently; the lowest reachable index wins. */
export async function resolveServer(candidates = SERVER_CANDIDATES, probe = defaultProbe) {
    const results = await Promise.all(candidates.map((url) => probe(url)));
    const index = results.findIndex(Boolean);
    return index === -1 ? null : candidates[index];
}
