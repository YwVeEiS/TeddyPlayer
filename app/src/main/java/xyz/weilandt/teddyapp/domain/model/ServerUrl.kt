package xyz.weilandt.teddyapp.domain.model

object ServerUrl {
    const val DEFAULT = "http://tc"

    /**
     * Turns input like `192.168.1.50/web/` into a base URL `http://192.168.1.50`.
     * Returns `null` if the input is blank.
     */
    fun normalize(input: String): String? {
        var url = input.trim()
        if (url.isEmpty()) return null
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }
        url = url.trimEnd('/')
        if (url.endsWith("/web", ignoreCase = true)) url = url.dropLast(4)
        return url.trimEnd('/')
    }

    fun resolve(baseUrl: String, path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path
        else baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
