package xyz.weilandt.teddyapp.domain.model

object ServerUrl {
    const val DEFAULT = "http://192.168.1.50"

    /**
     * Macht aus Eingaben wie `192.168.1.50/web/` eine Basis-URL `http://192.168.1.50`.
     * Gibt `null` zurück, wenn die Eingabe leer ist.
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
