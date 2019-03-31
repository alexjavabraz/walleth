package org.walleth.activities.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_nfc_enter_credentials.*
import org.walleth.R
import org.walleth.activities.BaseSubActivity
import org.walleth.data.EXTRA_KEY_NFC_CREDENTIALS

class NFCEnterCredentials : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nfc_enter_credentials)

        input_pin.setText("000000")
        input_puk.setText("000000000000")
        input_pairingpwd.setText("foo")

        fab.setOnClickListener {
            val nfcCredentials = NFCCredentials(
                    isNewCard = radio_new_card.isChecked,
                    pin = input_pin.text.toString(),
                    puk =  input_puk.text.toString(),
                    pairingPassword = input_pairingpwd.text.toString()
            )
            val resultIntent = Intent().putExtra(EXTRA_KEY_NFC_CREDENTIALS, nfcCredentials)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
