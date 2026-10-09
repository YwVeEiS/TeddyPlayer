package xyz.weilandt.teddyapp.domain.model

/**
 * Umrechnung zwischen NFC-UID einer Tonie-Figur und der TeddyCloud-Tag-ID (`ruid`).
 *
 * Die UID `E0:04:03:50:9D:D8:CC:4A` steht in TeddyCloud byteweise umgedreht als
 * `4accd89d500304e0`. Android liefert die Bytes je nach Gerät in der einen oder
 * anderen Reihenfolge, daher probieren wir beide.
 */
object TagIds {

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    /** Mögliche ruids für eine gelesene UID (Hex), wie geliefert zuerst. */
    fun candidates(uidHex: String): List<String> {
        val hex = uidHex.lowercase()
        val reversed = hex.chunked(2).reversed().joinToString("")
        return listOf(hex, reversed).distinct()
    }
}
