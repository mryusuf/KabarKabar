package io.mryusuf.kabarkabar

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform