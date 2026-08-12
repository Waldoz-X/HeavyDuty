package com.example.heavyduty.core.common.dispatchers

import javax.inject.Qualifier

/**
 * Qualifiers for Hilt-provided coroutine dispatchers.
 * Enables testability by allowing dispatcher injection instead of hardcoding Dispatchers.IO etc.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
