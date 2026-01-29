package com.example.facedetection

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class CameraViewModel : ViewModel() {

    val bitmap = MutableLiveData<Bitmap?>()
    val mood = MutableLiveData<String>()
    val foodSuggestions = MutableLiveData<List<String>>()

    // Configure ML Kit face detector (fast + classification for smile probability)
    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // enables smilingProbability
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    /**
     * Process the given upright bitmap:
     * - run ML Kit face detection
     * - pick smilingProbability if available and map to mood
     * - if no face or no classifier available, use a simple brightness heuristic fallback
     */
    fun processBitmap(bmp: Bitmap) {
        bitmap.value = bmp // show the image immediately

        val image = InputImage.fromBitmap(bmp, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                // If ML Kit returns faces, use first face for mood
                if (faces.isNullOrEmpty()) {
                    // no face found -> fallback to brightness
                    val b = brightnessHeuristic(bmp)
                    val mapped = mapBrightnessToMood(b)
                    mood.postValue(mapped)
                    foodSuggestions.postValue(suggestionsForMood(mapped))
                    Log.d(TAG, "No faces detected. brightness=$b -> mood=$mapped")
                    return@addOnSuccessListener
                }

                // pick the most confident face (here: first)
                val face = chooseBestFace(faces)
                val smileProb = face.smilingProbability // nullable: may be null if not supported
                val leftEyeOpen = face.leftEyeOpenProbability
                val rightEyeOpen = face.rightEyeOpenProbability

                Log.d(TAG, "Face detected: smilingProb=$smileProb leftEye=$leftEyeOpen rightEye=$rightEyeOpen")

                val mappedMood = when {
                    smileProb != null -> mapSmileToMood(smileProb)
                    else -> {
                        // fallback to brightness if smile prob not provided
                        val b = brightnessHeuristic(bmp)
                        mapBrightnessToMood(b)
                    }
                }

                mood.postValue(mappedMood)
                foodSuggestions.postValue(suggestionsForMood(mappedMood))
            }
            .addOnFailureListener { e ->
                // detection failed -> fallback to brightness + log
                val b = brightnessHeuristic(bmp)
                val mapped = mapBrightnessToMood(b)
                mood.postValue(mapped)
                foodSuggestions.postValue(suggestionsForMood(mapped))
                Log.e(TAG, "Face detection failed: ${e.message}. brightness=$b -> mood=$mapped", e)
            }
    }

    private fun chooseBestFace(faces: List<Face>): Face {
        // Simple heuristic: choose largest bounding box area (likely the main face)
        return faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: faces[0]
    }

    // Translate ML Kit smilingProbability (0..1) to moods
    private fun mapSmileToMood(prob: Float): String {
        return when {
            prob >= 0.75f -> "Very Happy"
            prob >= 0.45f -> "Happy"
            prob >= 0.25f -> "Neutral"
            prob >= 0.10f -> "Sad"
            else -> "Very Sad"
        }
    }

    // Fallback: compute simple average brightness (0..255)
    private fun brightnessHeuristic(bmp: Bitmap): Int {
        // sample scaled-down bitmap to be quick
        val sample = Bitmap.createScaledBitmap(bmp, 40, 40, true)
        var sum = 0L
        val px = IntArray(sample.width * sample.height)
        sample.getPixels(px, 0, sample.width, 0, 0, sample.width, sample.height)
        for (c in px) {
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
            sum += lum
        }
        val avg = (sum / px.size).toInt()
        sample.recycle()
        return avg
    }

    private fun mapBrightnessToMood(brightness: Int): String {
        return when {
            brightness >= 180 -> "Very Happy"
            brightness >= 140 -> "Happy"
            brightness >= 100 -> "Neutral"
            brightness >= 60 -> "Sad"
            else -> "Very Sad"
        }
    }

    private fun suggestionsForMood(mood: String): List<String> {
        return when (mood) {
            "Very Happy" -> listOf("Celebrate with Pizza", "Sushi", "Chocolate cake")
            "Happy" -> listOf("Ice-cream", "Pasta", "Fresh fruit")
            "Neutral" -> listOf("Sandwich", "Salad", "Tea")
            "Sad" -> listOf("Warm soup", "Comfort food - Mac & Cheese", "Hot chocolate")
            "Very Sad" -> listOf("Chocolate", "Ice-cream", "Call a friend")
            else -> listOf("Water", "Fruit")
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            detector.close()
        } catch (ignore: Exception) {}
    }

    companion object {
        private const val TAG = "CameraViewModel"
    }
}
