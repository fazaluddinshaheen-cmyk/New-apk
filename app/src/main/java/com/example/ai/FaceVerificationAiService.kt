package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class FaceVerificationAiResult(
    val isLiveHumanFace: Boolean,
    val confidencePercent: Int,
    val reasonUrdu: String
)

object FaceVerificationAiService {
    private const val TAG = "FaceVerificationAi"

    private val faceSchema = Schema.obj(
        properties = mapOf(
            "isLiveHumanFace" to Schema.boolean(
                description = "True only if the image clearly shows a single real human face looking toward the camera suitable for identity face verification. False for blank screens, landscapes, objects, cartoons, ID cards/documents, blurred faces, or occluded faces."
            ),
            "confidencePercent" to Schema.integer(
                description = "Confidence score from 0 to 100 indicating how clearly a real human face is visible."
            ),
            "reasonUrdu" to Schema.string(
                description = "Short, clear 1-sentence explanation in Urdu explaining why the face selfie passed or failed."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = faceSchema
            }
        )
    }

    suspend fun verifyFaceBitmap(bitmap: Bitmap): Result<FaceVerificationAiResult> {
        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(
                            "You are an AI face verification security inspector for Crescent Wealth. " +
                                "Inspect the provided image and decide if it contains one clear, well-lit, genuine human face suitable for selfie face verification. " +
                                "Reject blank images, objects, scenery, printed documents/ID cards/CNICs, heavily obscured faces, or unreadable blur. " +
                                "Respond in JSON with isLiveHumanFace, confidencePercent (0-100), and reasonUrdu (concise Urdu feedback)."
                        )
                    }
                )
            }

            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("AI ماڈل سے کوئی جواب موصول نہیں ہوا۔"))

            val json = JSONObject(rawJson)
            val isHuman = json.optBoolean("isLiveHumanFace", false)
            val confidence = json.optInt("confidencePercent", 0).coerceIn(0, 100)
            val reasonUrdu = json.optString("reasonUrdu", "").trim().ifBlank {
                if (isHuman) "چہرے کی تصدیق AI کے ذریعے مکمل ہو گئی۔"
                else "واضح انسانی چہرہ نظر نہیں آیا، براہ کرم دوبارہ کوشش کریں۔"
            }

            Result.success(
                FaceVerificationAiResult(
                    isLiveHumanFace = isHuman && confidence >= 60,
                    confidencePercent = confidence,
                    reasonUrdu = reasonUrdu
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI face verification timed out", e)
            Result.failure(IllegalStateException("AI فیس ویریفکیشن کا وقت ختم ہو گیا۔ براہ کرم دوبارہ کوشش کریں۔", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI face verification failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "AI فیس ویریفکیشن ناکام ہو گئی۔ براہ کرم انٹرنیٹ چیک کریں۔",
                    e
                )
            )
        }
    }
}
