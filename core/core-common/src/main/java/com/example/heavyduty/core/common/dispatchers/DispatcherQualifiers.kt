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

/**
 * A [kotlinx.coroutines.CoroutineScope] that outlives any single screen/ViewModel, for work that
 * must complete even if the component that triggered it is destroyed (e.g. one-shot DB seeding).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
