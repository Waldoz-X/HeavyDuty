package com.example.heavyduty.core.domain.repository

/**
 * Whether the app's database was already on the device — false on the very first launch.
 * Answered from the file alone, without opening the database (which on a first launch means
 * waiting for the exercise catalog to be seeded).
 */
fun interface StoredDataCheck {
    fun databaseExists(): Boolean
}
