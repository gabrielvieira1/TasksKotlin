package com.android.taskskotlin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.taskskotlin.service.constants.TaskConstants
import com.android.taskskotlin.service.listener.APIListener
import com.android.taskskotlin.service.model.TaskModel
import com.android.taskskotlin.service.model.ValidationModel
import com.android.taskskotlin.service.repository.PriorityRepository
import com.android.taskskotlin.service.repository.TaskRepository

class TaskListViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application.applicationContext)
    private val priorityRepository = PriorityRepository(application.applicationContext)
    private var taskFilter = 0

    private val _tasks = MutableLiveData<List<TaskModel>>()
    val tasks: LiveData<List<TaskModel>> = _tasks

    private val _delete = MutableLiveData<ValidationModel>()
    val delete: LiveData<ValidationModel> = _delete

    private val _status = MutableLiveData<ValidationModel>()
    val status: LiveData<ValidationModel> = _status

    fun list(filter: Int) {
        taskFilter = filter
        val listener = object : APIListener<List<TaskModel>> {
            override fun onSuccess(result: List<TaskModel>) {
                result.forEach {
                    it.priorityDescription = priorityRepository.getDescription(it.priorityId)
                }
                _tasks.value = result
            }
            override fun onFailure(message: String) {}
        }

        when (filter) {
            TaskConstants.FILTER.ALL -> taskRepository.list(listener)
            TaskConstants.FILTER.NEXT -> taskRepository.listNext(listener)
            else -> taskRepository.listOverdue(listener)
        }
    }

    fun delete(id: Int) {
        taskRepository.delete(id, object : APIListener<Boolean> {
            override fun onSuccess(result: Boolean) {
                list(taskFilter)
            }

            override fun onFailure(message: String) {
                _delete.value = ValidationModel(message)
            }
        })
    }

    fun status(id: Int, complete: Boolean) {
        val listener = object : APIListener<Boolean> {
            override fun onSuccess(result: Boolean) {
                list(taskFilter)
            }

            override fun onFailure(message: String) {
                _status.value = ValidationModel(message)
            }
        }

        if (complete) {
            taskRepository.complete(id, listener)
        } else {
            taskRepository.undo(id, listener)
        }
    }

}