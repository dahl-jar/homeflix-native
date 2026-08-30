package app.homeflix.tv.feature.auth

data class AuthProfile(
    val id: String,
    val name: String,
    val hasPassword: Boolean,
    val avatarUrl: String? = null,
)

sealed interface ProfileSelectionAction {
    data class RequestPin(
        val profile: AuthProfile,
    ) : ProfileSelectionAction

    data class Authenticate(
        val profile: AuthProfile,
        val pin: String,
    ) : ProfileSelectionAction
}

object ProfileSelection {
    fun select(profile: AuthProfile): ProfileSelectionAction =
        if (profile.hasPassword) {
            ProfileSelectionAction.RequestPin(profile)
        } else {
            ProfileSelectionAction.Authenticate(profile, pin = "")
        }
}
