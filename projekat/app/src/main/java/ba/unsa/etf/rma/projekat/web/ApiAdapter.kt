package ba.unsa.etf.rma.projekat.web

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiAdapter {
    val retrofit : Api = Retrofit.Builder()
        .baseUrl("http://trefle.io/api/v1/")    //privremeno umjesto https
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(Api::class.java)
}