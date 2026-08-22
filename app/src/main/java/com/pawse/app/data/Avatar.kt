package com.pawse.app.data

/**
 * The animal shown on an app's block screen. Stored on [AppLimit] as the enum's name
 * (plain String column, no Room TypeConverter needed) so [fromName] can fall back to
 * [TURTLE] for anything unrecognized — covers a future removed avatar cleanly.
 */
enum class Avatar {
    TURTLE,
    CAT,
    OWL,
    FOX,
    ;

    companion object {
        fun fromName(name: String): Avatar = entries.find { it.name == name } ?: TURTLE
    }
}
