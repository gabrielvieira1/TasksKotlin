package com.android.taskskotlin.service.repository

import android.content.Context
import com.android.taskskotlin.R
import com.android.taskskotlin.service.listener.APIListener
import com.android.taskskotlin.service.model.PersonModel
import com.android.taskskotlin.service.repository.remote.PersonService
import com.android.taskskotlin.service.repository.remote.RetrofitClient

class PersonRepository(context: Context) : BaseRepository(context) {

    private val remote = RetrofitClient.getService(PersonService::class.java)

    fun login(email: String, password: String, listener: APIListener<PersonModel>) {
        if (!isConnectionAvailable()) {
            listener.onFailure(context.getString(R.string.ERROR_INTERNET_CONNECTION))
            return
        }

        executeCall(remote.login(email, password), listener)
    }

    fun create(name: String, email: String, password: String, listener: APIListener<PersonModel>) {
        if (!isConnectionAvailable()) {
            listener.onFailure(context.getString(R.string.ERROR_INTERNET_CONNECTION))
            return
        }

        executeCall(remote.create(name, email, password), listener)
    }

}