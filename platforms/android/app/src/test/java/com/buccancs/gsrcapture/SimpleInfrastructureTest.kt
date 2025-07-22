package com.buccancs.gsrcapture

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Simple test to verify that the test infrastructure is working correctly.
 * This test doesn't depend on any application classes that might have compilation issues.
 */
@RunWith(RobolectricTestRunner::class)
class SimpleInfrastructureTest {

    @Test
    fun `test infrastructure should be working`() {
        // Simple test to verify Truth assertions work
        assertThat(true).isTrue()
        assertThat("hello").isEqualTo("hello")
        assertThat(listOf(1, 2, 3)).hasSize(3)
    }

    @Test
    fun `basic math operations should work`() {
        val result = 2 + 2
        assertThat(result).isEqualTo(4)
    }

    @Test
    fun `string operations should work`() {
        val text = "Hello, World!"
        assertThat(text).contains("World")
        assertThat(text).startsWith("Hello")
        assertThat(text).endsWith("!")
    }

    @Test
    fun `collections should work`() {
        val numbers = listOf(1, 2, 3, 4, 5)
        assertThat(numbers).contains(3)
        assertThat(numbers).containsExactly(1, 2, 3, 4, 5).inOrder()
    }

    @Test
    fun `null handling should work`() {
        val nullValue: String? = null
        val nonNullValue: String? = "test"
        
        assertThat(nullValue).isNull()
        assertThat(nonNullValue).isNotNull()
        assertThat(nonNullValue).isEqualTo("test")
    }
}