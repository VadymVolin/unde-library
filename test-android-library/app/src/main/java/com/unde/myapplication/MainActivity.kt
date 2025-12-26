package com.unde.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.unde.library.UndeLibrary
import com.unde.library.external.network.interceptor.okhttp.UndeHttpInterceptor as OkHttpInterceptor
import com.unde.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

import retrofit2.Call
import retrofit2.http.GET
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val undeLibrary = UndeLibrary()
        undeLibrary.initialize()
        lifecycleScope.launch {
            while (isActive) {
                delay(10.seconds)
                withContext(Dispatchers.Default) {
                    val getCall = RetrofitClient.instance.getPost()
                    getCall.enqueue(object : Callback<Post> {
                        override fun onResponse(call: Call<Post>, response: Response<Post>) {
                            if (response.isSuccessful) {
                                val post = response.body()
                                Log.d("MainActivity", "Get request: $post")
                            } else {
                                Log.e("MainActivity", "Error: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<Post>, t: Throwable) {
                            Log.e("MainActivity", "Failure: ${t.message}")
                        }
                    })
                    val postCall = RetrofitClient.instance.sendPost()
                    postCall.enqueue(object : Callback<Post> {
                        override fun onResponse(call: Call<Post>, response: Response<Post>) {
                            if (response.isSuccessful) {
                                val post = response.body()
                                Log.d("MainActivity", "Post request: $post")
                            } else {
                                Log.e("MainActivity", "Error: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<Post>, t: Throwable) {
                            Log.e("MainActivity", "Failure: ${t.message}")
                        }
                    })
                }
            }
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}


// ================== TEST ======================
data class Post(
    val id: Int,
    val title: String
)


interface ApiService {
    @GET("posts/1")
    fun getPost(): Call<Post>

    @POST("create/post/mock")
    fun sendPost(@Body body: Post = Post(13, "Mock")): Call<Post>
}

object RetrofitClient {

    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(OkHttpInterceptor())
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        retrofit.create(ApiService::class.java)
    }
}
