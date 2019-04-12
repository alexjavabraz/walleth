package org.walleth.activities.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_nfc.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.koin.android.ext.android.inject
import org.ligi.kaxtui.alert
import org.ligi.kroom.inTransaction
import org.walleth.activities.trezor.TREZOR_REQUEST_CODE
import org.walleth.data.AppDatabase
import org.walleth.data.transactions.TransactionState
import org.walleth.data.transactions.toEntity
import org.walleth.kethereum.android.TransactionParcel
import org.walleth.khex.toHexString


fun Activity.startNFCSigningActivity(transactionParcel: TransactionParcel) {
    val trezorIntent = Intent(this, NFCSignTransactionActivity::class.java).putExtra("TX", transactionParcel)
    startActivityForResult(trezorIntent, TREZOR_REQUEST_CODE)
}

class NFCSignTransactionActivity : BaseNFCActivity() {

    private val transaction by lazy { intent.getParcelableExtra<TransactionParcel>("TX") }

    private val appDatabase: AppDatabase by inject()

    var pin = "000000"

    fun setText(value: String) {
        runOnUiThread {
            nfc_status_text.text = value

            nfc_status_text.parent.requestLayout()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.subtitle = "Sign via NFC"

        cardManager.onCardConnectedListener = { channel ->

            try {

                if (!nfcCredentialStore.hasPairing(channel.getCardInfo().instanceUID)) {
                    setText("no pairing")
                } else {
                    setText("pairing")

                    channel.commandSet.pairing = nfcCredentialStore.getPairing(channel.getCardInfo().instanceUID)

                    setText("open secure channel")
                    channel.commandSet.autoOpenSecureChannel()

                    setText("verify PIN")
                    channel.commandSet.verifyPIN(pin)

                    setText("signing")

                    val oldHash = transaction.transaction.txHash

                    val signedTransaction = channel.sign(transaction.transaction)

                    GlobalScope.launch (Dispatchers.Main) {
                        withContext(Dispatchers.Default) {
                            transaction.transaction.txHash = signedTransaction.encodeRLP().keccak().toHexString()
                            appDatabase.inTransaction {
                                oldHash?.let { transactions.deleteByHash(it) }
                                transactions.upsert(transaction.transaction.toEntity(signedTransaction.signatureData, TransactionState()))
                            }
                        }
                        setResult(RESULT_OK, Intent().apply { putExtra("TXHASH", transaction.transaction.txHash) })
                        finish()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let {
                    runOnUiThread {
                        alert(it)
                    }
                }
            }


        }

        cardManager.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        cardManager.onCardConnectedListener = null
    }
}
