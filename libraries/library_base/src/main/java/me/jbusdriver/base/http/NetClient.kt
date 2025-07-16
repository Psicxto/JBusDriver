/**
 * ==================================================================================================
 * <p>
 * 设计目的 (Why)
 * <p>
 * NetClient 是一个专门用于管理和配置网络请求的单例对象。在现代 Android 应用中，网络通信是核心功能之一，
 * 通常需要一个统一的地方来配置和创建网络客户端实例（如 OkHttpClient 和 Retrofit），以确保整个应用的网络
 * 请求行为一致，例如统一的超时设置、User-Agent、Cookie 管理、日志记录和数据转换等。通过将这些通用配置
 * 集中在 NetClient 中，可以避免在代码的多个地方重复配置，提高了代码的可维护性和复用性。
 * <p>
 * ==================================================================================================
 * <p>
 * 功效作用 (What)
 * <p>
 * 1.  **OkHttpClient 实例管理**：提供一个全局共享的 OkHttpClient 实例，配置了连接、读取和写入超时时间。
 * 2.  **Retrofit 实例工厂**：提供一个 getRetrofit 方法，用于根据传入的 baseUrl 创建 Retrofit 实例，并可选择性地处理 JSON 响应。
 * 3.  **请求拦截器 (Interceptor)**：
 *     -   `EXIST_MAGNET_INTERCEPTOR`: 为请求添加固定的 User-Agent 和 Cookie，用于模拟浏览器行为和身份验证。
 *     -   `PROGRESS_INTERCEPTOR`: 用于拦截响应，包装 ResponseBody 以实现下载进度的监听。
 *     -   `LoggerInterceptor`: 在 Debug 模式下，添加日志拦截器，用于打印网络请求和响应的详细信息。
 * 4.  **响应转换器 (Converter)**：
 *     -   `strConv`: 一个将 ResponseBody 直接转换为 String 的转换器。
 *     -   `jsonConv`: 一个将 ResponseBody 转换为 JsonObject 的转换器，并对返回的 JSON 数据进行初步校验（检查 code 是否为 200）。
 * 5.  **Cookie 管理**：实现了一个简单的内存 CookieJar，用于在请求之间保持 Cookie。
 * 6.  **网络状态检查**：提供 isNetAvailable 方法，用于检查设备当前是否有可用的网络连接。
 * 7.  **适配器工厂**：提供了 RxJava2CallAdapterFactory，使得 Retrofit 可以与 RxJava 2 协同工作。
 * <p>
 * ==================================================================================================
 * <p>
 * 核心技术 (How)
 * <p>
 * 1.  **单例模式 (Singleton)**：使用 Kotlin 的 `object` 关键字，创建了一个线程安全的单例，确保全局只有一个 NetClient 实例。
 * 2.  **懒加载 (Lazy Initialization)**：通过 `by lazy` 委托属性，延迟了 `okHttpClient`、`EXIST_MAGNET_INTERCEPTOR` 和 `PROGRESS_INTERCEPTOR`
 *     等重量级对象的初始化，直到它们第一次被访问时才创建，提高了应用的启动性能。
 * 3.  **Retrofit & OkHttp**：利用了 Square 公司强大的网络库 Retrofit 和 OkHttp。OkHttp 作为 HTTP 客户端，负责底层的网络通信；
 *     Retrofit 作为一个类型安全的 HTTP 客户端，将 HTTP API 转换为 Java/Kotlin 接口。
 * 4.  **拦截器链 (Interceptor Chain)**：通过向 OkHttpClient 添加多个拦截器，形成一个处理链。每个拦截器都可以对请求或响应进行检查、
 *     修改或增强，实现了对网络请求流程的灵活控制。
 * 5.  **自定义 Converter.Factory**：通过继承 `Converter.Factory` 并重写 `responseBodyConverter` 方法，实现了自定义的响应体转换逻辑，
 *     可以根据业务需求对原始响应数据进行预处理。
 * 6.  **反射与注解**：Retrofit 内部通过反射和注解来解析接口定义，并将它们转换为实际的 HTTP 请求。NetClient 中的转换器工厂也利用了
 *     `type` 和 `annotations` 等参数，这些参数是 Retrofit 通过反射获取的。
 * <p>
 * ==================================================================================================
 */
package me.jbusdriver.base.http

import android.content.Context
import android.net.ConnectivityManager
import android.text.TextUtils
import com.google.gson.JsonObject
import me.jbusdriver.base.BuildConfig
import me.jbusdriver.base.GSON
import me.jbusdriver.base.KLog
import okhttp3.*
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 网络请求Client
 */
object NetClient {
    private const val TAG = "NetClient"
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.67 Safari/537.36"
    // private val gsonConverterFactory = GsonConverterFactory.create(GSON)

    //默认通用的拦截器
    private val EXIST_MAGNET_INTERCEPTOR by lazy {
        Interceptor { chain ->
            var request = chain.request()
            val builder = request.newBuilder().header("User-Agent", USER_AGENT)
            val sb = buildString {
                append(if (!TextUtils.isEmpty(request.header("existmag"))){
                    "existmag=all"
                }else{
                    "existmag=mag"
                } )
                append(";")
                append("bus_auth=4b85UbbfIo1f9unsrObLRtu0aYAe8VOgu7OjJJBPE95b9jKg0Jqj7xGmCEzb9VJOGoJO")
            }
            builder.header("Cookie",sb)
            request = builder.build()
            chain.proceed(request)
        }
    }
    val RxJavaCallAdapterFactory: CallAdapter.Factory = RxJava2CallAdapterFactory.create()
    val PROGRESS_INTERCEPTOR by lazy {
        Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            return@Interceptor response.newBuilder()
                .body(ProgressResponseBody(request.url().toString(), response.body(), GlobalProgressListener))
                .build()
        }
    }

    private val strConv = object : Converter.Factory() {

        override fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<Annotation>,
            methodAnnotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<*, RequestBody>? {
            KLog.d("requestBodyConverter", type, parameterAnnotations, methodAnnotations)
            for (parameterAnnotation in parameterAnnotations) {
                KLog.d("parameterAnnotation", parameterAnnotation)
            }
            return super.requestBodyConverter(
                type,
                parameterAnnotations,
                methodAnnotations,
                retrofit
            )
        }

        override fun responseBodyConverter(
            type: Type?,
            annotations: Array<out Annotation>?,
            retrofit: Retrofit?
        ): Converter<ResponseBody, *> =
            Converter<ResponseBody, String> { it.string() }
    }

    private val jsonConv = object : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type?,
            annotations: Array<out Annotation>?,
            retrofit: Retrofit?
        ): Converter<ResponseBody, *> =
            Converter<ResponseBody, JsonObject> {
                val s = it.string()
                val json = GSON.fromJson(s, JsonObject::class.java)
                if (json == null || json.isJsonNull || json.entrySet().isEmpty()) {
                    error("json is null")
                }
                if (json.get("code")?.asInt == 200) {
                    return@Converter json
                } else {
                    error(json.get("message")?.asString ?: "未知错误")
                }
            }
    }

    fun getRetrofit(
        baseUrl: String = "https://raw.githubusercontent.com/",
        handleJson: Boolean = false,
        client: OkHttpClient = okHttpClient
    ): Retrofit =
        Retrofit.Builder().client(client).apply {
            if (baseUrl.isNotEmpty()) this.baseUrl(baseUrl)
        }.addConverterFactory(if (handleJson) jsonConv else strConv)
            .addCallAdapterFactory(RxJavaCallAdapterFactory)
            .build()

    //endregion

    private val okHttpClient by lazy {
        //设置缓存路径

        // .addNetworkInterceptor(StethoInterceptor())
        val client = OkHttpClient.Builder()
            .writeTimeout((30 * 1000).toLong(), TimeUnit.MILLISECONDS)
            .readTimeout((20 * 1000).toLong(), TimeUnit.MILLISECONDS)
            .connectTimeout((15 * 1000).toLong(), TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(EXIST_MAGNET_INTERCEPTOR)
            .cookieJar(object : CookieJar {
                private val cookieStore = HashMap<String, List<Cookie>>()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieStore[url.host()] = cookies
                }

                override fun loadForRequest(url: HttpUrl) = cookieStore[url.host()] ?: emptyList()
            })
        if (BuildConfig.DEBUG) {
            client.addInterceptor(LoggerInterceptor("OK_HTTP"))
        }
        client.build()
    }

    val glideOkHttpClient: OkHttpClient  by lazy { okHttpClient }

    /**
     * 判断是否有网络可用

     * @param context
     * *
     * @return
     */
    fun isNetAvailable(context: Context): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetworkInfo?.isAvailable ?: false
    } catch (e: Exception) {
        false
    }
}
