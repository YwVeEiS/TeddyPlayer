package xyz.weilandt.teddyapp.domain.model

/**
 * Conversion between the NFC UID of a Tonie figure and the TeddyCloud tag ID (`ruid`).
 *
 * TeddyCloud stores the UID `E0:04:03:50:9D:D8:CC:4A` byte-reversed as
 * `4accd89d500304e0`. Depending on the device, Android delivers the bytes in either
 * order, so we try both.
 */
object TagIds {

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    /** Possible ruids for a read UID (hex), as-delivered order first. */
    fun candidates(uidHex: String): List<String> {
        val hex = uidHex.lowercase()
        val reversed = hex.chunked(2).reversed().joinToString("")
        return listOf(hex, reversed).distinct()
    }
}
