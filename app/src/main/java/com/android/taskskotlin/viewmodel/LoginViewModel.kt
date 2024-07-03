package com.android.taskskotlin.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.taskskotlin.service.constants.TaskConstants
import com.android.taskskotlin.service.helper.BiometricHelper
import com.android.taskskotlin.service.listener.APIListener
import com.android.taskskotlin.service.model.PersonModel
import com.android.taskskotlin.service.model.PriorityModel
import com.android.taskskotlin.service.model.ValidationModel
import com.android.taskskotlin.service.repository.PersonRepository
import com.android.taskskotlin.service.repository.PriorityRepository
import com.android.taskskotlin.service.repository.SecurityPreferences
import com.android.taskskotlin.service.repository.remote.RetrofitClient
import com.android.taskskotlin.utils.Constants
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.GoogleAuthProvider
import com.android.taskskotlin.utils.logITag
import com.android.taskskotlin.utils.logETag


class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val personRepository = PersonRepository(application.applicationContext)
    private val priorityRepository = PriorityRepository(application.applicationContext)
    private val securityPreferences = SecurityPreferences(application.applicationContext)

    private val _login = MutableLiveData<ValidationModel>()
    val login: LiveData<ValidationModel> = _login

    private val _biometrics = MutableLiveData<Boolean>()
    val biometrics: LiveData<Boolean> = _biometrics

    private val auth = Firebase.auth

    private val _signInWithGoogleStatus = MutableLiveData<Constants.GoogleSignInStatus>()
    val signInWithGoogleStatus: LiveData<Constants.GoogleSignInStatus> get() = _signInWithGoogleStatus

    /**
     * Faz login usando API
     */
    fun doLogin(email: String, password: String) {
        Firebase.analytics.logEvent("log_button_click", null)
        personRepository.login(email, password, object : APIListener<PersonModel> {
            override fun onSuccess(result: PersonModel) {
                securityPreferences.store(TaskConstants.SHARED.TOKEN_KEY, result.token)
                securityPreferences.store(TaskConstants.SHARED.PERSON_KEY, result.personKey)
                securityPreferences.store(TaskConstants.SHARED.PERSON_NAME, result.name)

                RetrofitClient.addHeaders(result.token, result.personKey)

                _login.value = ValidationModel()
            }

            override fun onFailure(message: String) {
                _login.value = ValidationModel(message)
            }
        })
    }

    /**
     * Verifica se usuário está logado e
     */
    fun verifyAuthentication() {
        val token = securityPreferences.get(TaskConstants.SHARED.TOKEN_KEY)
        val person = securityPreferences.get(TaskConstants.SHARED.PERSON_KEY)

        RetrofitClient.addHeaders(token, person)

        // Se token e person key forem diferentes de vazio, usuário está logado
        val logged = (token != "" && person != "")

        // Se usuário não estiver logado, aplicação vai atualizar os dados
        if (!logged) {
            priorityRepository.list(object : APIListener<List<PriorityModel>> {
                override fun onSuccess(result: List<PriorityModel>) {
                    priorityRepository.save(result)
                }

                override fun onFailure(message: String) {
                }
            })
        }

        // Usuário está logado E possui autenticação biométrica
        _biometrics.value = logged && BiometricHelper.isBiometricAvailable(getApplication())
    }

    fun doLoginWithGoogle(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        when (val credential = result.credential) {
            // GoogleIdToken credential
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                            .let {
                                auth.signInWithCredential(it).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // Sign in success, update UI with the signed-in user's information
                                        logITag("signInWithCredential:success")
                                        _signInWithGoogleStatus.postValue(Constants.GoogleSignInStatus.SUCCESS)
                                        auth.currentUser?.getIdToken(true)
                                            ?.addOnCompleteListener { firebaseTask ->
                                                if (firebaseTask.isSuccessful) {
                                                    logITag("Firebase Token: ${firebaseTask.result.token}")
                                                }
                                            }
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        logETag("Received an invalid google id token response:")
                                        _signInWithGoogleStatus.postValue(Constants.GoogleSignInStatus.FAILURE)
                                    }
                                }
                            }
                    } catch (e: GoogleIdTokenParsingException) {
                        _signInWithGoogleStatus.postValue(Constants.GoogleSignInStatus.FAILURE)
                        logETag("Received an invalid google id token response: $e")
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    _signInWithGoogleStatus.postValue(Constants.GoogleSignInStatus.FAILURE)
                    logETag("Unexpected type of credential")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                _signInWithGoogleStatus.postValue(Constants.GoogleSignInStatus.FAILURE)
                logETag("Unexpected type of credential")
            }
        }
    }
}