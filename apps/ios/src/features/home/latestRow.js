export function dropHttpPathRows(items) {
    return items.filter((item) => !item.Path?.startsWith('http'));
}
