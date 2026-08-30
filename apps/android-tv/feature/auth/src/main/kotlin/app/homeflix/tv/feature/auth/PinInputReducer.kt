package app.homeflix.tv.feature.auth

private const val PIN_LENGTH = 4
private const val MAX_PIN_DIGIT = 9

data class PinInputState(
    val digits: String = "",
    val hasError: Boolean = false,
)

data class PinInputResult(
    val state: PinInputState,
    val shouldSubmit: Boolean,
    val pin: String?,
)

object PinInputReducer {
    fun append(
        state: PinInputState,
        digit: Int,
    ): PinInputResult {
        require(digit in 0..MAX_PIN_DIGIT)
        val digits =
            if (state.digits.length < PIN_LENGTH) {
                state.digits + digit
            } else {
                state.digits
            }
        val shouldSubmit = digits.length == PIN_LENGTH
        return PinInputResult(
            state = PinInputState(digits = digits),
            shouldSubmit = shouldSubmit,
            pin = digits.takeIf { shouldSubmit },
        )
    }

    fun backspace(state: PinInputState): PinInputState =
        state.copy(
            digits = state.digits.dropLast(1),
            hasError = false,
        )

    fun authenticationFailed(state: PinInputState): PinInputState =
        state.copy(
            digits = "",
            hasError = true,
        )
}
