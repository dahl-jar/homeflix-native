package app.homeflix.tv.feature.home

interface HomeGateway {
    suspend fun fetchHome(userId: String): HomeContent
}
