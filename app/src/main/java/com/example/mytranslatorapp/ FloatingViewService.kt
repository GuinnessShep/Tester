import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingViewService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var translatedText: TextView
    private lateinit var translationService: TranslationService
    private lateinit var speechToTextService: SpeechToTextService

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_view, null)
        translatedText = floatingView.findViewById(R.id.translatedText)
        translationService = TranslationService()
        speechToTextService = SpeechToTextService(this)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 100

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, layoutParams)

        speechToTextService.startStreamingRecognize { result ->
            CoroutineScope(Dispatchers.IO).launch {
                val translated = translationService.translateText(result, "en", "es")
                withContext(Dispatchers.Main) {
                    updateTranslatedText(translated)
                }
            }
        }
    }

    private fun updateTranslatedText(text: String) {
        translatedText.text = text
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingView)
        speechToTextService.stopStreamingRecognize()
    }
}
