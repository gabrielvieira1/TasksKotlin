package com.android.taskskotlin.viewmodel

import android.app.Application
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
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val personRepository = PersonRepository(application.applicationContext)
    private val priorityRepository = PriorityRepository(application.applicationContext)
    private val securityPreferences = SecurityPreferences(application.applicationContext)

    private val _login = MutableLiveData<ValidationModel>()
    val login: LiveData<ValidationModel> = _login

    private val _biometrics = MutableLiveData<Boolean>()
    val biometrics: LiveData<Boolean> = _biometrics

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

}