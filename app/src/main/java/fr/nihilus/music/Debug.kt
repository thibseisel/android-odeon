@file:JvmName("Debug")

package fr.nihilus.music

/**
 * Throws an AssertionError if the value is false and this build is a debug build.
 * This method is a replacement for the `assert` keyword or [kotlin.assert]
 * function that have no effect on Android.
 */
fun assert(value: Boolean) {
    assert(value) { "Assertion failed" }
}

/**
 * Throws an AssertionError calculated by [lazyMessage] if the value is false
 * and this build is a debug build.
 * This method is a replacement for the `assert` keyword or [kotlin.assert]
 * function that have no effect on Android.
 */
inline fun assert(value: Boolean, lazyMessage: () -> Any) {
    if (BuildConfig.DEBUG) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}