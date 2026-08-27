export function dropStreamRows(items) {
    return items.filter((item) => !item.Path?.startsWith('http'));
}
