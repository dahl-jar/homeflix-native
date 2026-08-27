/**
 * Remote stream rows leak into Latest with raw http stream urls as Path
 * and epoch-zero created dates; real items carry library:// stubs or file
 * paths. Server-side exclusion is the durable fix; this guards the row.
 */
export function dropStreamRows(items) {
    return items.filter((item) => !item.Path?.startsWith('http'));
}
