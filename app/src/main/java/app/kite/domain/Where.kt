package app.kite.domain

/** Where the mind was when the string was tugged. Names are stored, so only ever append. */
enum class Where {
    HERE, PAST, FUTURE, CARES;

    val away: Boolean get() = this != HERE

    companion object {
        fun of(name: String): Where = entries.firstOrNull { it.name == name } ?: HERE
        val drifts = listOf(PAST, FUTURE, CARES)
    }
}

/** What started a pull: the app's tug, or you catching yourself ("I drifted off"). */
enum class Source {
    TUG, SELF;

    companion object {
        fun of(name: String): Source = entries.firstOrNull { it.name == name } ?: TUG
    }
}

/** The three things you name while reeling in, one per turn of the spool. */
enum class Sense { SEE, HEAR, FEEL }
