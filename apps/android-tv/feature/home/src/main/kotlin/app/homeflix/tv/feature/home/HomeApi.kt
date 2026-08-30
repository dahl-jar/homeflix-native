package app.homeflix.tv.feature.home

import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class HomeApi(
    private val baseUrl: String,
    private val client: JsonApiClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : HomeGateway {
    override suspend fun fetchHome(userId: String): HomeContent =
        coroutineScope {
            val featuredRequest = async { optional { fetchFeatured(userId) }.orEmpty() }
            val resumeRequest = async { optional { fetchResume(userId) }.orEmpty() }
            val views = fetchViews(userId)
            val recentRequests = views.map { view -> async { optional { fetchRecent(userId, view) } } }

            val rails =
                buildList {
                    val resume = resumeRequest.await()
                    if (resume.isNotEmpty()) {
                        add(
                            HomeRail(
                                id = HomePolicy.CONTINUE_RAIL_ID,
                                title = "Continue watching",
                                items = resume,
                                variant = HomeRailVariant.Poster,
                            ),
                        )
                    }
                    addAll(recentRequests.awaitAll().filterNotNull())
                }

            HomeContent(
                featured = featuredRequest.await(),
                rails = HomePolicy.nonEmptyRails(rails),
            )
        }

    private suspend fun fetchFeatured(userId: String): List<HomeMediaItem> {
        val recommendations =
            HomeContract.recommendations(
                json,
                client.get("/HomeFlix/Recommendations"),
            )
        val ids =
            recommendations
                .sortedBy(HomeRecommendation::rank)
                .take(FEATURED_LIMIT)
                .map(HomeRecommendation::itemId)
        if (ids.isEmpty()) return emptyList()

        val items =
            HomeContract.items(
                json = json,
                baseUrl = baseUrl,
                payload =
                    client.get(
                        path = "/Items",
                        query =
                            mapOf(
                                "userId" to userId,
                                "ids" to ids.joinToString(","),
                                "enableImages" to "true",
                                "enableUserData" to "true",
                                "enableTotalRecordCount" to "false",
                                "fields" to "Overview",
                            ),
                    ),
            )
        return HomePolicy.rankFeatured(recommendations, items.associateBy(HomeMediaItem::id))
    }

    private suspend fun fetchResume(userId: String): List<HomeMediaItem> =
        HomeContract.items(
            json = json,
            baseUrl = baseUrl,
            payload =
                client.get(
                    path = "/UserItems/Resume",
                    query =
                        mapOf(
                            "userId" to userId,
                            "limit" to RESUME_LIMIT.toString(),
                            "mediaTypes" to "Video",
                            "fields" to "Overview",
                        ),
                ),
        )

    private suspend fun fetchViews(userId: String): List<HomeView> =
        HomeContract.views(
            json,
            client.get(
                path = "/UserViews",
                query = mapOf("userId" to userId),
            ),
        )

    private suspend fun fetchRecent(
        userId: String,
        view: HomeView,
    ): HomeRail {
        val items =
            if (view.collectionType == MOVIES_COLLECTION) {
                HomeContract.recentItems(
                    json = json,
                    baseUrl = baseUrl,
                    payload =
                        client.get(
                            path = "/Items",
                            query =
                                mapOf(
                                    "userId" to userId,
                                    "parentId" to view.id,
                                    "recursive" to "true",
                                    "includeItemTypes" to "Movie",
                                    "sortBy" to "DateCreated",
                                    "sortOrder" to "Descending",
                                    "limit" to MOVIE_LIMIT.toString(),
                                    "fields" to "Path",
                                ),
                        ),
                )
            } else {
                HomeContract.latestItems(
                    json = json,
                    baseUrl = baseUrl,
                    payload =
                        client.get(
                            path = "/Users/$userId/Items/Latest",
                            query =
                                mapOf(
                                    "parentId" to view.id,
                                    "limit" to LATEST_LIMIT.toString(),
                                    "groupItems" to "true",
                                    "fields" to "Path",
                                ),
                        ),
                )
            }

        return HomeRail(
            id = view.id,
            title = "Recently added in ${view.name}",
            items = items,
            variant = HomeRailVariant.Poster,
        )
    }

    private suspend fun <T> optional(block: suspend () -> T): T? =
        try {
            block()
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            null
        }

    private companion object {
        const val FEATURED_LIMIT = 8
        const val RESUME_LIMIT = 12
        const val MOVIE_LIMIT = 16
        const val LATEST_LIMIT = 24
        const val MOVIES_COLLECTION = "movies"
    }
}
