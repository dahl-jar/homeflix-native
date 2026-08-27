const LOAD_MORE_THRESHOLD = 20;

export function createPager({ pageSize = 100 } = {}) {
    const items = [];
    let total = 0;

    return {
        items,
        pageSize,
        get total() {
            return total;
        },
        applyPage(result) {
            items.push(...result.Items);
            total = result.TotalRecordCount;
        },
        nextStartIndex() {
            return items.length;
        },
        shouldLoadMore(index) {
            if (items.length >= total) return false;
            return index >= items.length - LOAD_MORE_THRESHOLD;
        }
    };
}
