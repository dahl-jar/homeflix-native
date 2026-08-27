/** Maps a Jellyfin public user onto the shape the profile gate renders. */
export function toGateCard(user) {
    return {
        id: user.Id,
        name: user.Name,
        hasPassword: user.HasPassword === true,
        imageTag: user.PrimaryImageTag ?? null
    };
}

export function fetchPublicUsers(client) {
    return client.get('/Users/Public');
}

export function authenticate(client, username, password = '') {
    return client.post('/Users/AuthenticateByName', { Username: username, Pw: password });
}

export function fetchMe(client) {
    return client.get('/Users/Me');
}
