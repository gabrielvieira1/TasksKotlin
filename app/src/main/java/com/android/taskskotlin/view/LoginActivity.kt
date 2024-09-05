package com.android.taskskotlin.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModelProvider
import com.android.taskskotlin.R
import com.android.taskskotlin.databinding.ActivityLoginBinding
import com.android.taskskotlin.viewmodel.LoginViewModel
import com.android.taskskotlin.utils.Constants
import com.android.taskskotlin.utils.logException
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Variáveis da classe
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        binding = ActivityLoginBinding.inflate(layoutInflater)

        // Layout
        setContentView(binding.root)
        supportActionBar?.hide()

        // Inicializa o CredentialManager
        credentialManager = CredentialManager.create(this@LoginActivity)

        // Eventos
        binding.buttonLogin.setOnClickListener(this)
        binding.textRegister.setOnClickListener(this)
        binding.buttonGoogleSignIn.setOnClickListener(this)

        viewModel.verifyAuthentication()

        // Observadores
        observe()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_login -> handleLogin()
            R.id.text_register -> startActivity(Intent(this, RegisterActivity::class.java))
            R.id.button_google_sign_in -> signIn()
        }
    }

    private fun signIn() {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(Constants.GOOGLE_WEB_CLIENT)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity,
                )
                viewModel.doLoginWithGoogle(result)
                //Toast.makeText(applicationContext, "Successful login", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                logException(e)
            }
        }
    }

    private fun observe() {
        viewModel.login.observe(this) {
            if (it.status()) {
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(applicationContext, it.message(), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.biometrics.observe(this) {
            if (it) {
                val executor: Executor = ContextCompat.getMainExecutor(this)

                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            startActivity(Intent(applicationContext, MainActivity::class.java))
                            finish()
                        }
                    })

                // Informações apresentadas no momento da autenticação
                val info: BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.autenticacao_biometrica))
                    // .setSubtitle("Subtítulo")
                    // .setDescription("Descrição")
                    .setNegativeButtonText(getString(R.string.cancelar))
                    .build()

                // Exibe para o usuário
                biometricPrompt.authenticate(info)
            }
        }
    }

    private fun handleLogin() {
        val email = binding.editEmail.text.toString()
        val password = binding.editPassword.text.toString()

        viewModel.doLoginWithAPI(email, password)
    }
}