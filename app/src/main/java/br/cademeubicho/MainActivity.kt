package br.cademeubicho

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import br.cademeubicho.ui.tutorial.TutorialActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit


@Suppress("DEPRECATION", "UNREACHABLE_CODE")
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var verificationInProgress = false
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    @SuppressLint("PackageManagerGetSignatures")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

       /* val cm = baseContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo*/

        val connectivityManager = baseContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

        if(!isConnected){
            //Você está conectado à internet
            if(activeNetwork?.type == ConnectivityManager.TYPE_WIFI){
                Toast.makeText(baseContext, "Conectado via rede Wi-Fi\n", Toast.LENGTH_SHORT).show()
            }
            if(activeNetwork?.type == ConnectivityManager.TYPE_MOBILE){
                Toast.makeText(baseContext, "Conectado via rede Movel\n", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(baseContext, "Não possui conecxão com a Internet\n", Toast.LENGTH_SHORT).show()
                this.finish()
            }

        }
        if (toShowIntro()) {
            startActivityForResult(Intent(this, TutorialActivity::class.java), 1114)
        }

        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_meus_pets,
                R.id.nav_mensagens,
                R.id.nav_minha_conta,
                /*R.id.nav_configuracao,*/
                R.id.nav_cadastro_animal
            ), drawer_layout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        nav_view.setupWithNavController(navController)

        try {
            val info = packageManager.getPackageInfo(
                "br.cademeubicho",
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }


    }


    @SuppressLint("RtlHardcoded")
    override fun onResume() {
        super.onResume()
        val view: View = nav_view.getHeaderView(0)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            view.ivSair.visibility = VISIBLE
            view.tvEmail.visibility = VISIBLE

            view.tvName.text = user.displayName
            view.tvEmail.text = user.email

        } else {
            view.ivSair.visibility = INVISIBLE
            view.tvEmail.visibility = INVISIBLE
            view.tvName.text = getString(R.string.app_name)

        }

        view.ivSair.setOnClickListener {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    Toast.makeText(this, R.string.saiu, Toast.LENGTH_SHORT).show()
                    onResume()
                    drawer_layout.closeDrawer(Gravity.LEFT)
                }


        }

    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber, // Phone number to verify
            60, // Timeout duration
            TimeUnit.SECONDS, // Unit of timeout
            this, // Activity (for callback binding)
            callbacks
        ) // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]

        verificationInProgress = true
    }

    private fun toShowIntro(): Boolean {
        val prefs =
            PreferenceManager.getDefaultSharedPreferences(baseContext)
        return prefs.getBoolean("isAlreadyShown", true)
    }

    private fun makeIntroNotRunAgain() {
        val prefs =
            PreferenceManager.getDefaultSharedPreferences(baseContext)
        val previouslyStarted = prefs.getBoolean("isAlreadyShown", false)
        if (!previouslyStarted) {
            val edit = prefs.edit()
            edit.putBoolean("isAlreadyShown", false)
            edit.apply()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1114) {
            makeIntroNotRunAgain()
        }
    }

}
