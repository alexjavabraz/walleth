package org.walleth.model

import android.app.Activity
import android.support.annotation.DrawableRes

data class AccountType(
        val accountType: String?,
        val name: String,
        val action: String,
        val description: String,
        @DrawableRes val drawable: Int,
        @DrawableRes val actionDrawable: Int,
        val wrapsKey: Boolean = false,
        val callback: (context: Activity) -> Unit = {}
)