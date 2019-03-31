package org.walleth.data

import okhttp3.MediaType
import java.math.BigInteger

val ETH_IN_WEI = BigInteger("1000000000000000000")

var DEFAULT_GAS_PRICE = BigInteger("20000000000")
var DEFAULT_GAS_LIMIT_ETH_TX = BigInteger("21000")
var DEFAULT_GAS_LIMIT_ERC_20_TX = BigInteger("73000")

const val DEFAULT_PASSWORD = "default"

val JSON_MEDIA_TYPE = MediaType.parse("application/json")

const val DEFAULT_ETHEREUM_BIP44_PATH = "m/44'/60'/0'/0/0"

const val KEY_TX_HASH = "TXHASH"


const val REQUEST_CODE_CREATE_ACCOUNT = 420
const val REQUEST_CODE_PICK_ACCOUNT_TYPE = REQUEST_CODE_CREATE_ACCOUNT + 1
const val REQUEST_CODE_SELECT_TOKEN = REQUEST_CODE_PICK_ACCOUNT_TYPE + 1
const val REQUEST_CODE_ENTER_NFC_CREDENTIALS = REQUEST_CODE_SELECT_TOKEN + 1


const val KEY_INTENT_EXTRA_TYPE = "TYPE"
const val KEY_INTENT_EXTRA_KEYCONTENT = "KEY"


const val EXTRA_KEY_ADDRESS = "addess"
const val EXTRA_KEY_ACCOUNT_TYPE = "account_type"
const val EXTRA_KEY_NFC_CREDENTIALS = "nfc_credentials"

const val ACCOUNT_TYPE_BURNER = "burner"
const val ACCOUNT_TYPE_TREZOR = "trezor"
const val ACCOUNT_TYPE_NFC = "nfc"
const val ACCOUNT_TYPE_WATCH_ONLY = "watchonly"
const val ACCOUNT_IMPORTEDBURNER = "importedburner"