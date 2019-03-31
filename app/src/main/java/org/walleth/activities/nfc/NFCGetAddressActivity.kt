package org.walleth.activities.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_nfc.*
import org.kethereum.crypto.toAddress
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.walleth.data.EXTRA_KEY_ADDRESS
import org.walleth.data.EXTRA_KEY_NFC_CREDENTIALS
import org.walleth.data.REQUEST_CODE_ENTER_NFC_CREDENTIALS
import org.walleth.khartwarewallet.KHardwareChannel

@Parcelize
data class NFCCredentials(var isNewCard: Boolean,
                          var pin: String,
                          var pairingPassword: String,
                          var puk: String? = null) : Parcelable

class NFCGetAddressActivity : BaseNFCActivity() {

    private val nfcCredentialStore: NFCCredentialStore by inject()

    var credentials: NFCCredentials? = null

    fun setText(value: String) {
        runOnUiThread {
            nfc_status_text.text = value

            nfc_status_text.parent.requestLayout()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (credentials == null) {
            startActivityForResult(Intent(this, NFCEnterCredentials::class.java), REQUEST_CODE_ENTER_NFC_CREDENTIALS)
        }

        supportActionBar?.subtitle = "NFC SmartCard interaction"
        /*
        showPINDialog(
                onPIN = { pin = it },
                onCancel = { finish() },
                labelButtons = true,
                pinPadMapping = KEY_MAP_TOUCH,
                maxLength = 6
        )
        */

        cardManager.onCardConnectedListener = { channel ->

            credentials?.let { credentials ->

                try {

                    val cardInfo = channel.getCardInfo()
                    if (credentials.isNewCard) {
                        if (!cardInfo.isInitializedCard) {
                            setText("Initializing new card ..")
                            val result = channel.commandSet.init(credentials.pin, credentials.puk!!, credentials.pairingPassword)
                            if (!result.checkOK().isOK) {
                                setText("Initializing failed")
                            } else {

                                channel.commandSet.select().checkOK()
                                setText("Selected")

                                pairAndStore(channel, credentials)

                                channel.commandSet.autoOpenSecureChannel()

                                setText("Secured channel")
                                channel.commandSet.verifyPIN(credentials.pin)

                                channel.commandSet.setNDEF((NdefMessage(NdefRecord.createApplicationRecord("org.walleth")).toByteArray()))

                                setText("NDEF set")
                                val wasKeyGenerated = channel.commandSet.generateKey().isOK


                                if (wasKeyGenerated) {
                                    finishWithAddress(channel.toPublicKey().toAddress())
                                } else {
                                    setText("Problem generating key")
                                }

                            }
                        } else runOnUiThread {
                            alert("This is not a new card.")
                        }
                    } else {

                        if (nfcCredentialStore.hasPairing(cardInfo.instanceUID)) {
                            channel.commandSet.pairing = nfcCredentialStore.getPairing(cardInfo.instanceUID)
                            setText("Paired (old)")
                        } else {
                            pairAndStore(channel, credentials)
                            setText("Paired (new)")
                        }

                        channel.commandSet.autoOpenSecureChannel()

                        setText("Secured channel")
                        channel.commandSet.verifyPIN(credentials.pin)

                        setText("PIN")

                        val wasKeyGenerated = channel.commandSet.generateKey().isOK

                        setText("genkey")

                        val address = channel.toPublicKey().toAddress()

                        setText("Address" + address)

                        finishWithAddress(channel.toPublicKey().toAddress())

                        /*
                        val resultIntent = Intent()
                        resultIntent.putExtra(ADDRESS_HEX_KEY, address.hex)
                        //resultIntent.putExtra(ADDRESS_PATH, BIP44(DEFAULT_ETHEREUM_BIP44_PATH))
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
*/
                        //channel.commandSet.autoPair(cardInfo.pairingPassword)
//                        channel.commandSet.pairing
                    }

/*

                    if (!channel.cardInfo.isInitializedCard) {
                        channel.init(cardInfo.pin, cardInfo.puk!!, cardInfo.pairingPassword)
                    } else {


                        setText("Paired")
                        channel.autoOpenSecureChannel()

                        setText("Secured channel")
                        channel.verifyPIN(cardInfo.pin)

                        setText("PIN")
                        val address = channel.toPublicKey().toAddress()

                        setText("Address")

                        channel.unpairOthers()
                        channel.autoUnpair()

                        setText("Unpaired")
                        val resultIntent = Intent()
                        resultIntent.putExtra(ADDRESS_HEX_KEY, address.hex)
                        //resultIntent.putExtra(ADDRESS_PATH, BIP44(DEFAULT_ETHEREUM_BIP44_PATH))
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                    */
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        alert(e.message!!)
                    }
                }

            }
        }

        cardManager.start()
    }

    private fun pairAndStore(channel: KHardwareChannel, credentials: NFCCredentials) {
        channel.commandSet.autoPair(credentials.pairingPassword)
        setText("Paired")
        val keyUID = channel.getCardInfo().instanceUID
        nfcCredentialStore.putPairing(keyUID, channel.commandSet.pairing)
    }

    private fun finishWithAddress(address: Address) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_ADDRESS, address.toString()))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        cardManager.onCardConnectedListener = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            credentials = data.getParcelableExtra(EXTRA_KEY_NFC_CREDENTIALS)
            setText(credentials.toString())
        } else {
            finish()
        }
    }
}
