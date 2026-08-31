package app.homeflix.tv.feature.auth

internal fun authProfileFixtures(): List<AuthProfile> =
    listOf(
        AuthProfile(id = "one", name = "Darrow", hasPassword = true),
        AuthProfile(id = "two", name = "Mustang", hasPassword = false),
        AuthProfile(id = "three", name = "Goblin", hasPassword = false),
    )
