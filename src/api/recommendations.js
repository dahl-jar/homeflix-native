/** Latest nightly recommendation batch: rows of ItemId, Rank, SeedVotes, GenreScore, CommunityRating. */
export function fetchRecommendations(client) {
    return client.get('/HomeFlix/Recommendations');
}
