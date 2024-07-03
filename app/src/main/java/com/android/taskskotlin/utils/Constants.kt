package com.android.taskskotlin.utils

object Constants {

    const val SAMPLE_RATE_IN_HZ = 16000
    const val SHARE_PREF_NAME = "VoiceRecordingPrefs"
    const val SAVING_AUDIO_DIRECTORY_NAME = "VoiceRecordings"
    const val CURRENT_PHRASE_INDEX = "currentPhraseIndex"
    const val AUDIO_PLAYED_PREFIX = "audio_played_"
    const val GOOGLE_WEB_CLIENT = "236612577548-spl0oc5rkpdhjbpdag3g53h0kr6vm9gp.apps.googleusercontent.com"

    enum class LoadingStatus {
        SUCCESS, FAILURE, LOADING
    }

    enum class GoogleSignInStatus {
        SUCCESS, FAILURE
    }
}
