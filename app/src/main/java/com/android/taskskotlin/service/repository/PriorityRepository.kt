package com.android.taskskotlin.service.repository

import android.content.Context
import com.android.taskskotlin.R
import com.android.taskskotlin.service.listener.APIListener
import com.android.taskskotlin.service.model.PriorityModel
import com.android.taskskotlin.service.repository.local.TaskDatabase
import com.android.taskskotlin.service.repository.remote.PriorityService
import com.android.taskskotlin.service.repository.remote.RetrofitClient

class PriorityRepository(context: Context) : BaseRepository(context) {

    private val remote = RetrofitClient.getService(PriorityService::class.java)
    private val database = TaskDatabase.getDatabase(context).priorityDAO()

    companion object {
        private val cache = mutableMapOf<Int, String>()
        fun getDescription(id: Int): String {
            return cache[id] ?: ""
        }
        fun setDescription(id: Int, str: String) {
            cache[id] = str
        }
    }

    /**
     * Obtém a descrição da tarefa, sempre buscando no cache antes do banco de dados
     * */
    fun getDescription(id: Int): String {
        val cached = Companion.getDescription(id)
        return if (cached == "") {
            val description = database.getDescription(id)
            setDescription(id, description)
            description
        } else {
            cached
        }
    }

    fun list(listener: APIListener<List<PriorityModel>>) {
        if (!isConnectionAvailable()) {
            listener.onFailure(context.getString(R.string.ERROR_INTERNET_CONNECTION))
            return
        }

        executeCall(remote.list(), listener)
    }

    fun list(): List<PriorityModel> {
        return database.list()
    }

    fun save(list: List<PriorityModel>) {
        database.clear()
        database.save(list)
    }

}