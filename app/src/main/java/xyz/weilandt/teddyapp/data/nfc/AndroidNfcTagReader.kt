package xyz.weilandt.teddyapp.data.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.os.Bundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xyz.weilandt.teddyapp.domain.model.TagIds
import xyz.weilandt.teddyapp.domain.repository.TonieTagReader

/**
 * Liest Tonie-Figuren (ISO 15693 / NFC-V) per Reader-Mode, solange eine Activity im
 * Vordergrund ist. Gelesen wird nur die UID – keine geschützten Daten vom Chip.
 */
class AndroidNfcTagReader(context: Context) : TonieTagReader {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    private val _tagIds = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Gelesene UIDs als Hex (Byte-Reihenfolge wie von Android geliefert). */
    override val tagIds: Flow<String> = _tagIds.asSharedFlow()

    fun enable(activity: Activity) {
        adapter?.enableReaderMode(
            activity,
            { tag -> _tagIds.tryEmit(TagIds.toHex(tag.id)) },
            NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            Bundle(),
        )
    }

    fun disable(activity: Activity) {
        adapter?.disableReaderMode(activity)
    }
}
