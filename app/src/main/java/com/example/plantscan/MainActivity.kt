package com.example.plantscan

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.plantscan.ui.theme.PlantScanTheme
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val apiKey = "API-KEY"
    private val apiUrl = "https://my-api.plantnet.org/v2/identify/all?api-key=$apiKey"

    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var apiResult by mutableStateOf("Processing...") // State for the API result

        setContent {
            PlantScanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = apiResult,  // Display the API result
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                sendImageToApi(it) { result ->
                    apiResult = result // Update the state with the API result
                }
            }
        }

        takePictureLauncher.launch(null)

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Relaunch the camera instead of closing the app
                takePictureLauncher.launch(null)
            }
        })
    }

    private fun sendImageToApi(bitmap: Bitmap, onResult: (String) -> Unit) {
        val client = OkHttpClient()
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("images", "image.jpg",
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray))
            .addFormDataPart("organs", "leaf")  // Adjust the organ as needed
            .build()

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onResult("Failed to get response") // Handle failure
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val result = parseResponse(responseBody)
                    onResult(result ?: "No result found") // Update with result
                } else {
                    onResult("Failed to get a successful response")
                }
            }
        })
    }

    private fun parseResponse(response: String?): String? {
        val gson = Gson()
        val plantResponse = gson.fromJson(response, PlantResponse::class.java)
        return plantResponse?.results?.firstOrNull()?.species?.scientificName
    }

    data class PlantResponse(val results: List<PlantResult>)
    data class PlantResult(val species: Species)
    data class Species(val scientificName: String)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlantScanTheme {
        Greeting("Android")
    }
}
