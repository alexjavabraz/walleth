package org.walleth.data.addressbook

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import org.kethereum.model.Address
import org.walleth.data.ACCOUNT_TYPE_NFC
import org.walleth.data.ACCOUNT_TYPE_TREZOR

fun AddressBookEntry?.isTrezor() = this?.linkedKeyURI?.startsWith("$ACCOUNT_TYPE_TREZOR:") == true
fun AddressBookEntry.getTrezorDerivationPath() = when {
    linkedKeyURI?.startsWith("$ACCOUNT_TYPE_TREZOR:") == true -> linkedKeyURI?.removePrefix("$ACCOUNT_TYPE_TREZOR:")
    linkedKeyURI?.startsWith("m/") == true -> linkedKeyURI.also{
        linkedKeyURI = "$ACCOUNT_TYPE_TREZOR:$linkedKeyURI"
    }
    else -> null
}

fun AddressBookEntry?.isNFC() = this?.linkedKeyURI?.startsWith("$ACCOUNT_TYPE_NFC:") == true
fun AddressBookEntry.getNFCDerivationPath() = when {
    linkedKeyURI?.startsWith("$ACCOUNT_TYPE_TREZOR:") == true -> linkedKeyURI?.removePrefix("$ACCOUNT_TYPE_TREZOR:")
    linkedKeyURI?.startsWith("m/") == true -> linkedKeyURI.also{
        linkedKeyURI = "$ACCOUNT_TYPE_TREZOR:$linkedKeyURI"
    }
    else -> null
}


@Entity(tableName = "addressbook")
data class AddressBookEntry(

        @PrimaryKey
        var address: Address,

        var name: String,

        var note: String? = null,

        @ColumnInfo(name = "is_notification_wanted")
        var isNotificationWanted: Boolean = false,

        @ColumnInfo(name = "trezor_derivation_path") // TODO with the next migration we should rename the column
        var linkedKeyURI: String? = null,

        var starred: Boolean = false,

        var deleted: Boolean = false,

        var fromUser: Boolean = false,

        var order: Int = 0
)