package app.homeflix.tv.feature.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PinInputReducerTest {
    @Test
    fun `should submit pin on fourth digit`() {
        var state = PinInputState()

        state = PinInputReducer.append(state, 4).state
        state = PinInputReducer.append(state, 3).state
        state = PinInputReducer.append(state, 2).state
        val fourth = PinInputReducer.append(state, 1)

        assertEquals("4321", fourth.state.digits)
        assertTrue(fourth.shouldSubmit)
        assertEquals("4321", fourth.pin)
    }

    @Test
    fun `should ignore digits after fourth`() {
        val complete = PinInputState(digits = "1234")

        val result = PinInputReducer.append(complete, 5)

        assertEquals(complete, result.state)
        assertTrue(result.shouldSubmit)
        assertEquals("1234", result.pin)
    }

    @Test
    fun `should clear pin after authentication failure`() {
        val result = PinInputReducer.authenticationFailed(PinInputState(digits = "1234"))

        assertEquals("", result.digits)
        assertTrue(result.hasError)
    }

    @Test
    fun `should remove last digit`() {
        val result = PinInputReducer.backspace(PinInputState(digits = "123"))

        assertEquals("12", result.digits)
        assertFalse(result.hasError)
        assertNull(PinInputReducer.append(PinInputState(), 1).pin)
    }

    @Test
    fun `should reject invalid digits`() {
        assertThrows(IllegalArgumentException::class.java) {
            PinInputReducer.append(PinInputState(), -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PinInputReducer.append(PinInputState(), 10)
        }
    }
}
