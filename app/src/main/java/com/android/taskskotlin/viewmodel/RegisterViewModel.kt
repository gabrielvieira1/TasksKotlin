package com.android.taskskotlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.taskskotlin.service.constants.TaskConstants
import com.android.taskskotlin.service.listener.APIListener
import com.android.taskskotlin.service.model.PersonModel
import com.android.taskskotlin.service.model.ValidationModel
import com.android.taskskotlin.service.repository.PersonRepository
import com.android.taskskotlin.service.repository.SecurityPreferences
import com.android.taskskotlin.service.repository.remote.RetrofitClient

// Encripta as informações com o EncriptySharedPreferences e salva no SharedPreferences

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val personRepository = PersonRepository(application.applicationContext)
    private val securityPreferences = SecurityPreferences(application.applicationContext)
    // private val keystoreManager = KeystoreManager(application.applicationContext)

    private val _user = MutableLiveData<ValidationModel>()
    val user: LiveData<ValidationModel> = _user

    fun create(name: String, email: String, password: String) {
        personRepository.create(name, email, password, object : APIListener<PersonModel>{
            override fun onSuccess(result: PersonModel) {
                securityPreferences.store(TaskConstants.SHARED.TOKEN_KEY, result.token)
                securityPreferences.store(TaskConstants.SHARED.PERSON_KEY, result.personKey)
                securityPreferences.store(TaskConstants.SHARED.PERSON_NAME, result.name)

                RetrofitClient.addHeaders(result.token, result.personKey)

                _user.value = ValidationModel()
            }
            override fun onFailure(message: String) {
                _user.value = ValidationModel(message)
            }
        })
    }

}

// Encripta as informações e salva no SharedPreferences

//class RegisterViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val personRepository = PersonRepository(application.applicationContext)
//    private val securityPreferences = SecurityPreferences(application.applicationContext)
//    private val keystoreManager = KeystoreManager(application.applicationContext)
//
//    private val _user = MutableLiveData<ValidationModel>()
//    val user: LiveData<ValidationModel> = _user
//
//    fun create(name: String, email: String, password: String) {
//        personRepository.create(name, email, password, object : APIListener<PersonModel>{
//            override fun onSuccess(result: PersonModel) {
//                val encryptedToken = keystoreManager.encryptData(result.token)
//                val encryptedPersonKey = keystoreManager.encryptData(result.personKey)
//                val encryptedName = keystoreManager.encryptData(result.name)
//
//                // Armazene os dados criptografados
//                securityPreferences.store(TaskConstants.SHARED.TOKEN_KEY, encryptedToken)
//                securityPreferences.store(TaskConstants.SHARED.PERSON_KEY, encryptedPersonKey)
//                securityPreferences.store(TaskConstants.SHARED.PERSON_NAME, encryptedName)
//
//                RetrofitClient.addHeaders(result.token, result.personKey)
//
//                _user.value = ValidationModel()
//            }
//            override fun onFailure(message: String) {
//                _user.value = ValidationModel(message)
//            }
//        })
//    }
//
//}

// Sem encriptar e salva no SharedPreferences

//class RegisterViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val personRepository = PersonRepository(application.applicationContext)
//    private val securityPreferences = SecurityPreferences(application.applicationContext)
//
//    private val _user = MutableLiveData<ValidationModel>()
//    val user: LiveData<ValidationModel> = _user
//
//    fun create(name: String, email: String, password: String) {
//        personRepository.create(name, email, password, object : APIListener<PersonModel>{
//            override fun onSuccess(result: PersonModel) {
//                securityPreferences.store(TaskConstants.SHARED.TOKEN_KEY, result.token)
//                securityPreferences.store(TaskConstants.SHARED.PERSON_KEY, result.personKey)
//                securityPreferences.store(TaskConstants.SHARED.PERSON_NAME, result.name)
//
//                RetrofitClient.addHeaders(result.token, result.personKey)
//
//                _user.value = ValidationModel()
//            }
//            override fun onFailure(message: String) {
//                _user.value = ValidationModel(message)
//            }
//        })
//    }
//
//}