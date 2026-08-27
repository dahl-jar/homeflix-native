export function fetchRecommendations(client) {
    return client.get('/HomeFlix/Recommendations');
}
