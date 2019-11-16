package team.gotohel.howwasyourday.api

import android.util.Log
import io.reactivex.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import team.gotohel.howwasyourday.BuildConfig
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * fields -> @Field (메소드 앞에 @FormUrlEncoded 를 붙여야 한다.)
 * parameters -> @Query
 * path -> @Path
 */

class SampleApiClient() {

    companion object {
        private val BASE_URL_API_SERVER_DEFAULT = "https://sample.gotohel.team/"

        private var apiClient: SampleApiClient? = null
        fun getInstance(): SampleApiClient {
            if (apiClient == null) {
                apiClient = SampleApiClient()
            }
            return apiClient!!
        }
    }

    interface APIService {
        @GET("search/something")
        fun searchSomething(@Query("key") key: String): Single<String>
    }

    var retrofit: Retrofit
    val call: APIService
    val okHttpClient : OkHttpClient.Builder

    init {
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(AuthenticationInterceptor())

        val logging = HttpLoggingInterceptor()
        if (BuildConfig.DEBUG) { // development build
            logging.level = HttpLoggingInterceptor.Level.BODY
        } else { // production build
            logging.level = HttpLoggingInterceptor.Level.BASIC
        }

        okHttpClient.addInterceptor(logging)

        //set normal rest adapter
        retrofit = Retrofit.Builder()
            .client(okHttpClient.build())
            .baseUrl(BASE_URL_API_SERVER_DEFAULT)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        call = retrofit.create(APIService::class.java)
    }

    internal class AuthenticationInterceptor : Interceptor {

        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            try {
                val raw = chain.request()
                val authorized = raw.newBuilder()
                    .addHeader("Sample-Host", "sample_host")
                    .addHeader("Sample-Key", "sample_key")
                    .addHeader("Accept", "application/json")
                    .build()

                val response = chain.proceed(authorized)

                when (response.code()) {
                    401 -> {
//                    Log.d("인증 에러", "authKey : $authKey")
                    }
                    404 -> {
                        // do something
                    }
                }

                return response

            } catch (e: Exception) {

                //공통적인 오류 상황에 대한 안내 처리를 이곳에서 해준다.

                if (e is ConnectException || e is SocketTimeoutException) {
                    //기기가 네트워크에 연결되지 않거나, 서버가 완전히 죽어버린 상황
                    e.printStackTrace()
                    Log.e("APIClient", "Connection Problem")

                } else {
                    e.printStackTrace()
                }

                throw e
            }
        }
    }
}