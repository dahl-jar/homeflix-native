/**
 * Debounced search runner: trims input, drops whitespace-only queries,
 * and aborts the in-flight run when a newer query arrives.
 */
export function createSearchController({ delayMs = 350, run }) {
    let timer = null;
    let inFlight = null;

    return {
        onQuery(rawQuery) {
            const query = rawQuery.trim();
            if (timer) clearTimeout(timer);
            if (inFlight) {
                inFlight.abort();
                inFlight = null;
            }
            if (query === '') return;
            timer = setTimeout(() => {
                const controller = new AbortController();
                inFlight = controller;
                run(query, controller.signal);
            }, delayMs);
        }
    };
}
