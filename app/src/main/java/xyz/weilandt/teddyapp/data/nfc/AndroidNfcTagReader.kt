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
 * Reads Tonie figures (ISO 15693 / NFC-V) in reader mode while an activity is in the
 * foreground. Only the UID is read – no protected data from the chip.
 */
class AndroidNfcTagReader(context: Context) : TonieTagReader {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    private val _tagIds = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Read UIDs as hex (byte order as delivered by Android). */
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
