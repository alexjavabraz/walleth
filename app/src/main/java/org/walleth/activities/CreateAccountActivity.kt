package org.walleth.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_account_create.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.crypto.createEthereumKeyPair
import org.kethereum.crypto.toAddress
import org.kethereum.erc681.parseERC681
import org.kethereum.erc831.isEthereumURLString
import org.kethereum.functions.isValid
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.koin.android.ext.android.inject
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.trezor.getAddressResult
import org.walleth.activities.trezor.getPATHResult
import org.walleth.activities.trezor.hasAddressResult
import org.walleth.data.*
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.util.hasText

private const val HEX_INTENT_EXTRA_KEY = "HEX"

fun Context.startCreateAccountActivity(hex: String) {
    startActivity(Intent(this, CreateAccountActivity::class.java).apply {
        putExtra(HEX_INTENT_EXTRA_KEY, hex)
    })
}

class CreateAccountActivity : BaseSubActivity() {

    private val keyStore: KeyStore by inject()
    private val appDatabase: AppDatabase by inject()
    private val currentAddressProvider: CurrentAddressProvider by inject()

    private var trezorPath: String? = null

    private var currentType: String? = null
    private var currentAddress: Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_create)

        supportActionBar?.subtitle = getString(R.string.create_account_subtitle)

        if (currentType == null) {
            startActivityForResult(Intent(this, NewAccountTypeSelectActivity::class.java), REQUEST_CODE_PICK_ACCOUNT_TYPE)
        }

        intent.getStringExtra(HEX_INTENT_EXTRA_KEY)?.let {
            //hexInput.setText(it)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(currentAddressProvider.getCurrent() != null)

        type_select_button.setOnClickListener {
            startActivityForResult(Intent(this, NewAccountTypeSelectActivity::class.java), REQUEST_CODE_PICK_ACCOUNT_TYPE)
        }

        fab.setOnClickListener {
            if (!nameInput.hasText()) {
                alert(title = R.string.alert_problem_title, message = R.string.please_enter_name)
                return@setOnClickListener
            }
            when (currentType) {
                ACCOUNT_TYPE_BURNER -> {
                    val key = createEthereumKeyPair()
                    keyStore.addKey(key, DEFAULT_PASSWORD, true)

                    GlobalScope.launch(Dispatchers.Main) {
                        val address = key.toAddress()
                        withContext(Dispatchers.Default) {
                            appDatabase.addressBook.upsert(AddressBookEntry(
                                    name = nameInput.text.toString(),
                                    address = address,
                                    note = noteInput.text.toString(),
                                    linkedKeyURI = null,
                                    isNotificationWanted = notify_checkbox.isChecked)
                            )
                        }
                        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_ADDRESS, address.hex))
                        finish()
                    }

                }

                ACCOUNT_TYPE_NFC -> {

                    GlobalScope.launch(Dispatchers.Main) {
                        withContext(Dispatchers.Default) {
                            appDatabase.addressBook.upsert(AddressBookEntry(
                                    name = nameInput.text.toString(),
                                    address = currentAddress!!,
                                    note = noteInput.text.toString(),
                                    linkedKeyURI = "$ACCOUNT_TYPE_NFC:",
                                    isNotificationWanted = notify_checkbox.isChecked)
                            )
                        }
                        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_ADDRESS, currentAddress!!.hex))
                        finish()
                    }
                    /*GlobalScope.launch(Dispatchers.Main) {
                        withContext(Dispatchers.Default) {
                            appDatabase.addressBook.upsert(AddressBookEntry(
                                    name = nameInput.text.toString(),
                                    address = address,
                                    note = noteInput.text.toString(),
                                    linkedKeyURI = null,
                                    isNotificationWanted = notify_checkbox.isChecked)
                            )
                        }
                        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_ADDRESS, address.hex))
                        finish()
                    }*/

                }
            }
            /*
            val hex = ""

            if (!Address(hex).isValid()) {
                alert(title = alert_problem_title, message = address_not_valid)
            } else else {

            }
 */
        }

        applyViewModel()
/*
        add_trezor.setVisibility(packageManager.hasSystemFeature(FEATURE_USB_HOST))
        add_trezor.setOnClickListener {
            startActivityForResult(Intent(this, NFCGetAddressActivity::class.java), REQUEST_CODE_NFC)
        }

        new_address_button.setOnClickListener {

            lastCreatedAddress = createEthereumKeyPair()
            lastCreatedAddress?.toAddress()?.let {
                setAddressFromExternalApplyingChecksum(it)
            }

            notify_checkbox.isChecked = true
        }

        add_nfc.setOnClickListener {
            startActivityForResult(Intent(this, NFCGetAddressActivity::class.java), REQUEST_CODE_TREZOR)
        }

        camera_button.setOnClickListener {
            startScanActivityForResult(this)
        }
*/
    }


    private fun applyViewModel() {

        (currentType ?: currentType.apply {
            type_image.setVisibility(false)
            type_select_button.text = "select"
        }).let { currentType ->
            type_image.setVisibility(true)
            val accountType = accountTypeMap[currentType]
            type_image.setImageResource(accountType?.drawable ?: R.drawable.ic_warning_black_24dp)

            type_select_button.text = "switch"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        data?.run {
            when (requestCode) {
                REQUEST_CODE_PICK_ACCOUNT_TYPE -> {
                    when (data.getStringExtra(EXTRA_KEY_ACCOUNT_TYPE)) {
                        ACCOUNT_TYPE_WATCH_ONLY -> {
                            currentAddressProvider.setCurrent(Address(data.getStringExtra(EXTRA_KEY_ADDRESS)))
                            startActivityFromClass(MainActivity::class.java)
                            finish()
                        }
                        ACCOUNT_TYPE_BURNER -> {
                            if (nameInput.text?.isBlank() == true) {
                                nameInput.setText(accountTypeMap[ACCOUNT_TYPE_BURNER]?.name)
                            }
                            currentType = ACCOUNT_TYPE_BURNER
                            applyViewModel()
                        }

                        ACCOUNT_TYPE_NFC -> {
                            currentAddress = data.getStringExtra(EXTRA_KEY_ADDRESS)?.let { Address(it) }
                            currentType = ACCOUNT_TYPE_NFC
                            applyViewModel()
                        }
                    }
                }
                else -> {
                    getStringExtra("SCAN_RESULT")?.let { stringExtra ->
                        val address = if (stringExtra.isEthereumURLString()) {
                            parseERC681(stringExtra).address
                        } else {
                            stringExtra
                        }
                        if (address != null) {
                            setAddressFromExternalApplyingChecksum(Address(address))
                        }
                    }
                    if (hasAddressResult()) {
                        trezorPath = getPATHResult()
                        setAddressFromExternalApplyingChecksum(Address(getAddressResult()))
                    }

                }
            }

        }
    }

    private fun setAddressFromExternalApplyingChecksum(addressHex: Address) {
        if (addressHex.isValid()) {
            //hexInput.setText(addressHex.withERC55Checksum().hex)
        } else {
            alert(getString(R.string.warning_not_a_valid_address, addressHex), getString(R.string.title_invalid_address_alert))
        }
    }
}
