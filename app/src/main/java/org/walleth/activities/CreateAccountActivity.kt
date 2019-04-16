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
import org.kethereum.erc55.withERC55Checksum
import org.kethereum.erc681.parseERC681
import org.kethereum.erc831.isEthereumURLString
import org.kethereum.functions.isValid
import org.kethereum.keystore.api.KeyStore
import org.kethereum.model.Address
import org.kethereum.model.ECKeyPair
import org.kethereum.model.PrivateKey
import org.kethereum.model.PublicKey
import org.koin.android.ext.android.inject
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromClass
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.activities.trezor.getAddressResult
import org.walleth.activities.trezor.hasAddressResult
import org.walleth.data.*
import org.walleth.data.addressbook.AccountKeySpec
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.data.addressbook.toJSON
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

    private var currentSpec: AccountKeySpec = AccountKeySpec(ACCOUNT_TYPE_NONE)
    private var currentAddress: Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_create)

        supportActionBar?.subtitle = getString(R.string.create_account_subtitle)

        intent.getStringExtra(HEX_INTENT_EXTRA_KEY)?.let {
            currentSpec = AccountKeySpec(ACCOUNT_TYPE_WATCH_ONLY)
            setAddressFromExternalApplyingChecksum(Address(it))
        }

        if (currentSpec.type == ACCOUNT_TYPE_NONE) {
            startActivityForResult(Intent(this, NewAccountTypeSelectActivity::class.java), REQUEST_CODE_PICK_ACCOUNT_TYPE)
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
            when (currentSpec.type) {
                ACCOUNT_TYPE_IMPORT -> {
                    val split = currentSpec.initPayload!!.split("/")
                    val key = ECKeyPair(PrivateKey(split.first()), PublicKey(split.last()))
                    keyStore.addKey(key, DEFAULT_PASSWORD, true)

                    createAccountAndFinish(key.toAddress(), currentSpec.copy(initPayload = null))
                }

                ACCOUNT_TYPE_BURNER -> {
                    val key = createEthereumKeyPair()
                    keyStore.addKey(key, DEFAULT_PASSWORD, true)

                    createAccountAndFinish(key.toAddress(), currentSpec)

                }

                ACCOUNT_TYPE_WATCH_ONLY -> {

                }

                ACCOUNT_TYPE_PIN_PROTECTED, ACCOUNT_TYPE_PASSWORD_PROTECTED -> {
                    val key = createEthereumKeyPair()
                    keyStore.addKey(key, currentSpec.pwd!!, true)

                    createAccountAndFinish(key.toAddress(), currentSpec.copy(pwd = null))
                }
                ACCOUNT_TYPE_NFC -> {
                    createAccountAndFinish(currentAddress!!, currentSpec)
                }
            }


        }

        applyViewModel()
/*
        add_trezor.setVisibility(packageManager.hasSystemFeature(FEATURE_USB_HOST))
        add_trezor.setOnClickListener {
            startActivityForResult(Intent(this, NFCGetAddressActivity::class.java), REQUEST_CODE_NFC)
        }


        add_nfc.setOnClickListener {
            startActivityForResult(Intent(this, NFCGetAddressActivity::class.java), REQUEST_CODE_TREZOR)
        }

        camera_button.setOnClickListener {
            startScanActivityForResult(this)
        }
*/
    }

    private fun createAccountAndFinish(address: Address, keySpec: AccountKeySpec) {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                appDatabase.addressBook.upsert(AddressBookEntry(
                        name = nameInput.text.toString(),
                        address = address,
                        note = noteInput.text.toString(),
                        keySpec = keySpec.toJSON(),
                        isNotificationWanted = notify_checkbox.isChecked)
                )
            }
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_ADDRESS, address.hex))
            finish()
        }
    }


    private fun applyViewModel() {
        val noneSelected = currentSpec.type == ACCOUNT_TYPE_NONE
        type_image.setVisibility(!noneSelected)
        type_select_button.text = if (noneSelected) "select" else "switch"

        val accountType = accountTypeMap[currentSpec.type]
        type_image.setImageResource(accountType?.drawable ?: R.drawable.ic_warning_black_24dp)

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        data?.run {
            when (requestCode) {
                REQUEST_CODE_PICK_ACCOUNT_TYPE -> {
                    val spec = data.getParcelableExtra<AccountKeySpec>(EXTRA_KEY_ACCOUNTSPEC)
                    if (spec.type == ACCOUNT_TYPE_WATCH_ONLY) {
                        currentAddressProvider.setCurrent(Address(data.getStringExtra(EXTRA_KEY_ADDRESS)))
                        startActivityFromClass(MainActivity::class.java)
                        finish()
                    } else {
                        currentSpec = spec
                        applyViewModel()
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
                        //trezorPath = getPATHResult()
                        setAddressFromExternalApplyingChecksum(Address(getAddressResult()))
                    }

                }
            }

        }
    }

    private fun setAddressFromExternalApplyingChecksum(address: Address) {
        if (address.isValid()) {
            currentAddress = address.withERC55Checksum()
        } else {
            alert(getString(R.string.warning_not_a_valid_address, address), getString(R.string.title_invalid_address_alert))
        }
    }
}
