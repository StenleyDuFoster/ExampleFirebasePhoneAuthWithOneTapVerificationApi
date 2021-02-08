package example.phoneauth.firebase.with.smsverificationapi

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.lang.Exception
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupClicks()
        startReceiver()
    }

    private fun setupClicks() {
        findViewById<Button>(R.id.sendButton).setOnClickListener {
            startPhoneAuthFirebase()
        }
    }

    private fun startPhoneAuthFirebase() {
        FirebaseAuth.getInstance().also {
            val options = PhoneAuthOptions.newBuilder(it)
                .setPhoneNumber(findViewById<EditText>(R.id.phoneEdit).text.toString())
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callBackFirebase)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    private fun startReceiver() {
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        registerReceiver(smsVerificationReceiver, intentFilter)
        SmsRetriever.getClient(this).startSmsUserConsent(null)
    }

    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {

                if (result.data != null) {
                    // Get SMS message content
                    val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                    // Extract one-time code from the message and complete verification
                    // `message` contains the entire text of the SMS message, so you will need
                    // to parse the string.
                    try {
                        message?.substring(0, 6)?.toInt()?.let {
                            findViewById<TextView>(R.id.codeText).setText(it.toString())
                        }
                    } catch (e: Exception) {

                    }
                } else {
                    // Consent denied. User can type OTC manually.
                }

            }
        }

    private val smsVerificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                val extras = intent.extras
                val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status?

                when (smsRetrieverStatus?.statusCode) {
                    CommonStatusCodes.SUCCESS -> {

                        try {
                            val consentIntent =
                                extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                            startForResult.launch(consentIntent)
                        } catch (e: Exception) {

                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {

                    }
                }
            }
        }
    }

    private val callBackFirebase = object :
        PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            findViewById<Button>(R.id.sendButton).visibility = View.GONE
            Toast.makeText(applicationContext, credential.toString(), Toast.LENGTH_SHORT).show()
        }

        override fun onVerificationFailed(ex: FirebaseException) {
            findViewById<EditText>(R.id.phoneEdit).setError(ex.message)
        }

    }

}