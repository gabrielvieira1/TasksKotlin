package com.android.taskskotlin.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.taskskotlin.databinding.RowTaskListBinding
import com.android.taskskotlin.service.listener.TaskListener
import com.android.taskskotlin.service.model.TaskModel
import com.android.taskskotlin.view.viewholder.TaskViewHolder

class TaskAdapter : RecyclerView.Adapter<TaskViewHolder>() {

    private var listTasks: List<TaskModel> = arrayListOf()
    private lateinit var listener: TaskListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding = RowTaskListBinding.inflate(inflater, parent, false)
        return TaskViewHolder(itemBinding, listener)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bindData(listTasks[position])
    }

    override fun getItemCount(): Int {
        return listTasks.count()
    }

    fun updateTasks(list: List<TaskModel>) {
        listTasks = list
        notifyDataSetChanged()
    }

    fun attachListener(taskListener: TaskListener) {
        listener = taskListener
    }

}