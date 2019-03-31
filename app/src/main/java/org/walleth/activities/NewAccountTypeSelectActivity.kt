package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.init_choice.*
import kotlinx.android.synthetic.main.item_account_type.view.*
import org.koin.android.ext.android.inject
import org.walleth.R
import org.walleth.activities.nfc.NFCGetAddressActivity
import org.walleth.data.*
import org.walleth.data.networks.CurrentAddressProvider

const val REQUEST_CODE_PICK_WATCH_ONLY = 1
const val REQUEST_CODE_PICK_NFC = 2

data class AccountType(
        val accountType: String?,
        val name: String,
        val action: String,
        val description: String,
        @DrawableRes val drawable: Int,
        val enabled: Boolean = true,
        val callback: (context: Activity) -> Unit = {})

val optionList = listOf(

        AccountType(ACCOUNT_TYPE_BURNER,
                "Burner",
                "Create Burner",
                "Easy to get you started but weak security.",
                R.drawable.ic_whatshot_black_24dp) {
            it.setResult(Activity.RESULT_OK,
                    Intent().putExtra(EXTRA_KEY_ACCOUNT_TYPE, ACCOUNT_TYPE_BURNER)
            )
            it.finish()
        },
        AccountType(

                ACCOUNT_TYPE_TREZOR,
                "TREZOR wallet",
                "Connect TREZOR",
                "Very reasonable security but you need to have a device that costs more than 50DAI and plug it in the phone",
                R.drawable.trezor_icon_black,
                enabled = false
        ),
        AccountType(
                null, "",
                "Scan with camera",
                "Often you encounter QR codes in this context - e.g. with your private key.",
                R.drawable.ic_image_camera_alt
        ),

        AccountType(
                null,
                "Imported Key",
                "Import Key",
                "Import a key - e.g. your backup",
                R.drawable.ic_import) {
            it.startActivityForResult(Intent(it, ImportKeyActivity::class.java), TO_ADDRESS_REQUEST_CODE)
        },
        AccountType(ACCOUNT_TYPE_WATCH_ONLY,
                "Watch only account",
                "Watch Only",
                "No transactions possible then - just monitor or interact with this account",
                R.drawable.ic_watch) {
            it.startActivityForResult(Intent(it, AccountPickActivity::class.java), REQUEST_CODE_PICK_WATCH_ONLY)
        },
        AccountType(ACCOUNT_TYPE_NFC,
                "NFC account",
                "Connect via NFC",
                "Contact-less connection to e.g. the keycard (a java card)",
                R.drawable.ic_nfc_black) {
            it.startActivityForResult(Intent(it,NFCGetAddressActivity::class.java), REQUEST_CODE_PICK_NFC)
        },
        AccountType(null,
                "Contract wallet",
                "Contract wallet",
                "Coming soon",
                R.drawable.ic_account_balance_wallet_black_24dp
        )
)

val accountTypeMap by lazy {
    mutableMapOf<String, AccountType>().apply {
        optionList.forEach {
            it.accountType?.let { accountType -> put(accountType, it) }
        }
    }
}

class AccountTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: AccountType) {
        itemView.bitmap_type.isEnabled = false
        itemView.bitmap_type.setImageResource(item.drawable)
        itemView.account_type_label.text = item.action
        itemView.account_type_description.text = item.description
        itemView.setOnClickListener {
            item.callback.invoke(itemView.context as Activity)
        }
    }
}

class AccountTypeAdapter : RecyclerView.Adapter<AccountTypeViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = AccountTypeViewHolder(LayoutInflater.from(p0.context).inflate(R.layout.item_account_type, p0, false))


    override fun getItemCount() = optionList.size

    override fun onBindViewHolder(viewHolder: AccountTypeViewHolder, p1: Int) {
        viewHolder.bind(optionList[p1])
    }
}

class NewAccountTypeSelectActivity : BaseSubActivity() {
    val currentAddressProvider: CurrentAddressProvider by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.init_choice)

        supportActionBar?.subtitle = "New account"

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AccountTypeAdapter()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_PICK_WATCH_ONLY -> {
                    setResult(Activity.RESULT_OK,
                            data.apply {
                                putExtra(EXTRA_KEY_ACCOUNT_TYPE, ACCOUNT_TYPE_WATCH_ONLY)
                            }
                    )
                    finish()
                }

                REQUEST_CODE_PICK_NFC -> {
                    setResult(Activity.RESULT_OK,
                            data.apply {
                                putExtra(EXTRA_KEY_ACCOUNT_TYPE, ACCOUNT_TYPE_NFC)
                            }
                    )
                    finish()
                }
            }
        }
    }
}