package org.walleth.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.activity_enter_password.*
import org.walleth.R
import org.walleth.data.EXTRA_KEY_PWD

class RequestPasswordActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_enter_password)

        input_pwd.imeOptions = EditorInfo.IME_ACTION_DONE
        input_pwd.setOnEditorActionListener { _, _, _ -> true.also { deliverResult() } }
        fab.setOnClickListener {
            deliverResult()
        }
    }

    private fun deliverResult() {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_PWD, input_pwd.text.toString()))
        finish()
    }


}
