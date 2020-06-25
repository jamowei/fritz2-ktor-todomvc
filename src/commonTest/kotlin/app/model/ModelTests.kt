package app.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ModelTests {
    @Test
    fun testMe() {
        assertTrue(Sample().checkMe() > 0)
    }
}