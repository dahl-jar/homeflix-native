const QUALITY = 90;

/** Tagged image urls get year-long cache headers from the server. */
export function primaryUrl(baseUrl, item, maxWidth) {
    const tag = item.ImageTags?.Primary;
    if (!tag) return null;
    return `${baseUrl}/Items/${item.Id}/Images/Primary?tag=${tag}&maxWidth=${maxWidth}&quality=${QUALITY}`;
}

export function backdropUrl(baseUrl, item, maxWidth) {
    const tag = item.BackdropImageTags?.[0];
    if (!tag) return primaryUrl(baseUrl, item, maxWidth);
    return `${baseUrl}/Items/${item.Id}/Images/Backdrop/0?tag=${tag}&maxWidth=${maxWidth}&quality=${QUALITY}`;
}

export function userImageUrl(baseUrl, user) {
    if (!user.PrimaryImageTag) return null;
    return `${baseUrl}/Users/${user.Id}/Images/Primary?tag=${user.PrimaryImageTag}&quality=${QUALITY}`;
}
