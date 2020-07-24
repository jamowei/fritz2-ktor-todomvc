package app.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ToDoTests {
    @Test
    fun testToDoValidator() {
        val validator = ToDoValidator()

        assertTrue(
            validator.validate(ToDo(text = " start with space"), Unit).size == 1,
            "validation for \"starting with space\" failed"
        )

        assertTrue(
            validator.validate(ToDo(text = "a"), Unit).size == 1,
            "validation for \"too short text\" failed"
        )

        assertTrue(
            validator.validate(ToDo(text = " a"), Unit).size == 2,
            "validation for \"two validations\" failed"
        )

        assertTrue(
            validator.validate(ToDo(text = "a".repeat(101)), Unit).size == 1,
            "validation for \"max length\" failed"
        )

    }
}