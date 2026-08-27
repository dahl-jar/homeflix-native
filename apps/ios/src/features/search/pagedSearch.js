import { createPager } from '../../api/paging.js';

import { createSearchController } from './debounce.js';

const SEARCH_STATUS = Object.freeze({
    DEBOUNCING: 'debouncing',
    ERROR: 'error',
    EXTERNAL: 'external',
    IDLE: 'idle',
    LOADING: 'loading',
    PAGING: 'paging',
    READY: 'ready'
});

const createNextPageRequest = ({
    pager,
    mergedReady,
    pagePending,
    signal,
    term
}) => {
    if (
        !pager
        || !mergedReady
        || pagePending
        || pager.items.length >= pager.total
        || !signal
        || signal.aborted
    ) {
        return null;
    }

    return {
        term,
        startIndex: pager.nextStartIndex(),
        limit: pager.pageSize,
        signal
    };
};

export function createPagedSearchController({
    delayMs = 350,
    pageSize = 18,
    loadLocalPage,
    loadMergedPage,
    onReset,
    onResults,
    onStatus
}) {
    let activeQuery = '';
    let activeSignal = null;
    let pager = null;
    let mergedReady = false;
    let pagePending = false;

    const publishMerged = (resultPager) => {
        onResults({
            items: [...resultPager.items],
            total: resultPager.total,
            source: 'merged'
        });
    };

    const runInitialSearch = async (term, signal) => {
        let mergedFailed = false;
        activeQuery = term;
        activeSignal = signal;
        pager = createPager({ pageSize });
        mergedReady = false;
        pagePending = false;
        onStatus(SEARCH_STATUS.LOADING);

        const request = { term, startIndex: 0, limit: pageSize, signal };
        const localTask = loadLocalPage(request)
            .then((result) => {
                if (signal.aborted || mergedReady) return;
                onResults({
                    items: result.Items,
                    total: result.TotalRecordCount,
                    source: 'local'
                });
                onStatus(mergedFailed ? SEARCH_STATUS.ERROR : SEARCH_STATUS.EXTERNAL);
            })
            .catch(() => undefined);
        const mergedTask = loadMergedPage(request)
            .then((result) => {
                if (signal.aborted) return;
                mergedReady = true;
                pager.applyPage(result);
                publishMerged(pager);
                onStatus(SEARCH_STATUS.READY);
            })
            .catch(() => {
                if (signal.aborted) return;
                mergedFailed = true;
                onStatus(SEARCH_STATUS.ERROR);
            });

        await Promise.all([localTask, mergedTask]);
    };

    const debouncedSearch = createSearchController({
        delayMs,
        run: runInitialSearch
    });

    return {
        onQuery(rawQuery) {
            const query = rawQuery.trim();
            debouncedSearch.onQuery(rawQuery);
            pager = null;
            mergedReady = false;
            pagePending = false;
            onReset();
            onStatus(query ? SEARCH_STATUS.DEBOUNCING : SEARCH_STATUS.IDLE);
        },
        async loadMore() {
            const request = createNextPageRequest({
                pager,
                mergedReady,
                pagePending,
                signal: activeSignal,
                term: activeQuery
            });
            if (!request) return;

            pagePending = true;
            onStatus(SEARCH_STATUS.PAGING);
            const pagePager = pager;
            const pageSignal = request.signal;
            try {
                const result = await loadMergedPage(request);
                if (pageSignal.aborted || pager !== pagePager) return;
                pagePager.applyPage(result);
                publishMerged(pagePager);
                onStatus(SEARCH_STATUS.READY);
            } catch {
                if (!pageSignal.aborted && pager === pagePager) {
                    onStatus(SEARCH_STATUS.ERROR);
                }
            } finally {
                if (pager === pagePager) pagePending = false;
            }
        },
        dispose() {
            debouncedSearch.dispose();
        }
    };
}
