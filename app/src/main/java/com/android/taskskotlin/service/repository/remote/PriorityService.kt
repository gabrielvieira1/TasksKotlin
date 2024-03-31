package com.android.taskskotlin.service.repository.remote

import com.android.taskskotlin.service.model.PriorityModel
import retrofit2.Call
import retrofit2.http.GET

interface PriorityService {

    @GET("Priority")
    fun list(): Call<List<PriorityModel>>

}