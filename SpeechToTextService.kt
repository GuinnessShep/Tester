import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class SpeechToTextService {
    private val googleApiKey = "your-google-api-key" // Replace with your Google API key
    private val microsoftApiKey = "your-microsoft-api-key" // Replace with your Microsoft API key
    private val microsoftRegion = "your-microsoft-region" // Replace with your Microsoft region
    private val httpClient = OkHttpClient()

    // Transcribes the given audio file using Google Cloud Speech-to-Text API
    fun transcribeWithGoogle(audioFile: File, languageCode: String, callback: (transcription: String?, error: String?) -> Unit) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", "audio.raw", audioFile.asRequestBody("audio/l16; rate=16000".toMediaType()))
            .addFormDataPart("key", googleApiKey)
            .addFormDataPart("config", JSONObject()
                .put("encoding", "LINEAR16")
                .put("sampleRateHertz", 16000)
                .put("languageCode", languageCode)
                .toString())
            .build()

        val request = Request.Builder()
            .url("https://speech.googleapis.com/v1p1beta1/speech:recognize")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                callback(null, "Failed to transcribe audio with Google API: ${response.message}")
                return@use
            }

            val jsonResponse = JSONObject(response.body!!.string())
            val results = jsonResponse.getJSONArray("results")
            val transcription = results.getJSONObject(0).getJSONArray("alternatives").getJSONObject(0).getString("transcript")
            callback(transcription, null)
        }
    }

    // Transcribes the given audio file using Microsoft's Speech Service
    fun transcribeWithMicrosoft(audioFile: File, languageCode: String, callback: (transcription: String?, error: String?) -> Unit) {
        val requestBody = audioFile.readBytes().toRequestBody("audio/wav".toMediaType())

        val request = Request.Builder()
            .url("https://${microsoftRegion}.stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=$languageCode")
            .addHeader("Content-Type", "audio/wav; codecs=audio/pcm; samplerate=16000")
            .addHeader("Ocp-Apim-Subscription-Key", microsoftApiKey)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                callback(null, "Failed to transcribe audio with Microsoft API: ${response.message}")
                return@use
            }

            val jsonResponse = JSONObject(response.body!!.string())
            val results = jsonResponse.getJSONArray("RecognitionStatus")
            if (results.getString(0) == "Success") {
                val transcription = jsonResponse.getJSONArray("DisplayText").getString(0)
                callback(transcription, null)
            } else {
                callback(null, "Failed to transcribe audio with Microsoft API")
            }
        }
    }

    // Add more speech recognition services as needed, following the same structure as above

}
