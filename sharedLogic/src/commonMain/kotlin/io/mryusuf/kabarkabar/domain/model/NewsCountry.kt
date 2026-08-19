package io.mryusuf.kabarkabar.domain.model

/** The only countries supported by the product and the NewsAPI request boundary. */
enum class NewsCountry(val code: String) {
    US("us"),
    ID("id");

    companion object {
        fun fromCode(code: String): NewsCountry? =
            values().firstOrNull { it.code == code }
    }
}
