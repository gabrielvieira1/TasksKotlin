package com.android.taskskotlin.service.repository.remote

import com.android.taskskotlin.service.constants.TaskConstants
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient private constructor() {

    companion object {
        private lateinit var INSTANCE: Retrofit
        private var token: String? = null
        private var personKey: String? = null
        private val remoteConfig: FirebaseRemoteConfig by lazy {
            FirebaseRemoteConfig.getInstance()
        }

        // Inicializa e busca as configurações do Firebase Remote Config
        private fun initializeRemoteConfig() {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Intervalo mínimo para buscar novas configs
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            // Definir valores padrão locais (opcional)
            val defaults: Map<String, Any> = mapOf(
                "api_token" to "default_token_value",
                "person_key" to "default_person_key_value"
            )
            remoteConfig.setDefaultsAsync(defaults)

            // Busca valores atualizados do Remote Config
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Valores obtidos com sucesso do Firebase Remote Config
                        token = remoteConfig.getString("api_token")
                        personKey = remoteConfig.getString("person_key")
                    } else {
                        // Lida com falha na obtenção dos valores
                        token = defaults["api_token"] as String
                        personKey = defaults["person_key"] as String
                    }
                }
        }

        private fun getRetrofitInstance(): Retrofit {
            val httpClient = OkHttpClient.Builder()

            httpClient.addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val request = chain.request()
                        .newBuilder()
                        .addHeader(TaskConstants.HEADER.TOKEN_KEY, token)
                        .addHeader(TaskConstants.HEADER.PERSON_KEY, personKey)
                        .build()
                    return chain.proceed(request)
                }
            })

            if (!Companion::INSTANCE.isInitialized) {
                synchronized(RetrofitClient::class) {
                    INSTANCE = Retrofit.Builder()
                        .baseUrl("http://devmasterteam.com/CursoAndroidAPI/")
                        .client(httpClient.build())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
            return INSTANCE
        }

        fun <T> getService(serviceClass: Class<T>): T {
            return getRetrofitInstance().create(serviceClass)
        }

        fun addHeaders(tokenValue: String, personKeyValue: String?) {
            token = tokenValue
            if (personKeyValue != null) {
                personKey = personKeyValue
            }
        }
    }
}