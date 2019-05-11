package artista.labs.appcrawler

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiProvider {

    @FormUrlEncoded
    @POST("https://buyhatke.com/cricket/dumpData.php")
    fun dumpData(@Field("data") data: String): Call<String>

}