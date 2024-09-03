package com.android.taskskotlin.viewmodel

import android.app.Application
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
import com.google.firebase.auth.auth
import com.google.firebase.auth.GoogleAuthProvider
import com.android.taskskotlin.utils.logITag
import com.android.taskskotlin.utils.logETag
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser


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

    fun doLoginWithAPI(email: String, password: String) {
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

    fun doLoginWithGoogle(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    handleGoogleIdTokenCredential(credential)
                } else {
                    handleUnexpectedCredentialType()
                }
            }
            else -> handleUnexpectedCredentialType()
        }
    }

    private fun handleGoogleIdTokenCredential(credential: CustomCredential) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            signInWithFirebase(firebaseCredential)
        } catch (e: GoogleIdTokenParsingException) {
            handleCredentialParsingError(e)
        }
    }

    private fun signInWithFirebase(firebaseCredential: AuthCredential) {
        auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                logITag("signInWithCredential:success")
                val user = auth.currentUser
                if (user != null) {
                    handleSuccessfulSignIn(user)
                } else {
                    handleSignInFailure("User is null after successful sign-in.")
                }
            } else {
                handleSignInFailure("Received an invalid google id token response:")
            }
        }
    }

    private fun handleSuccessfulSignIn(user: FirebaseUser) {
        val displayName = user.displayName ?: ""
        val email = user.email ?: ""

        user.getIdToken(true).addOnCompleteListener { firebaseTask ->
            if (firebaseTask.isSuccessful) {
                val token = firebaseTask.result.token ?: ""
                storeUserInformation(token, displayName, email)
                _login.value = ValidationModel()
            } else {
                handleSignInFailure("Failed to retrieve Firebase token.")
            }
        }
    }

    private fun storeUserInformation(token: String, displayName: String, email: String) {
        securityPreferences.store(TaskConstants.SHARED.TOKEN_KEY, token)
        securityPreferences.store(TaskConstants.SHARED.PERSON_NAME, displayName)
        securityPreferences.store(TaskConstants.SHARED.PERSON_EMAIL, email)

        RetrofitClient.addHeaders(token, null)
    }

    private fun handleCredentialParsingError(e: GoogleIdTokenParsingException) {
        _signInWithGoogleStatus.postValue(Constants.GoogleSignInStatus.FAILURE)
        logETag("Received an invalid google id token response: $e")
    }

    private fun handleUnexpectedCredentialType() {
        _signInWithGoogleStatus.postValue(Constants.GoogleSignInStatus.FAILURE)
        logETag("Unexpected type of credential")
    }

    private fun handleSignInFailure(errorMessage: String) {
        _signInWithGoogleStatus.postValue(Constants.GoogleSignInStatus.FAILURE)
        logETag(errorMessage)
    }

    fun verifyAuthentication() {
        val token = securityPreferences.get(TaskConstants.SHARED.TOKEN_KEY)
        val person = securityPreferences.get(TaskConstants.SHARED.PERSON_KEY)

        RetrofitClient.addHeaders(token, person)

        val loggedWithAPI = (token.isNotEmpty() && person.isNotEmpty())

        val loggedWithFirebase = token.isNotEmpty() && person.isEmpty()

        val logged = loggedWithAPI || loggedWithFirebase

        if (!logged) {
            priorityRepository.list(object : APIListener<List<PriorityModel>> {
                override fun onSuccess(result: List<PriorityModel>) {
                    priorityRepository.save(result)
                }

                override fun onFailure(message: String) {
                }
            })
        }

        _biometrics.value = logged && BiometricHelper.isBiometricAvailable(getApplication())
    }
}