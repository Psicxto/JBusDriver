/**
 * @author J.query
 * @date 2019/4/15
 * @email j-query@foxmail.com
 * Why: 设计目的
 *   `Gobal.kt` 文件（命名上可能是 `Global.kt` 的拼写错误）旨在提供一系列全局可用的工具函数、常量和扩展属性，
 *   以简化 Android 开发中的常见任务，并确保这些功能在整个应用中的一致性。其设计目的包括：
 *   1.  **代码重用**：将通用的功能（如 Toast 显示、文件目录创建、JSON 解析、URL 处理）集中在一个地方，避免在多个类中重复编写相同的代码。
 *   2.  **全局访问**：通过顶层函数和属性的方式，使得这些工具无需创建类的实例即可在任何地方直接调用。
 *   3.  **配置中心化**：提供一个中心化的 `GSON` 实例，并对其进行预配置，以统一应用内所有 JSON 的序列化和反序列化行为。
 *   4.  **性能优化**：利用懒加载 (`by lazy`) 和缓存 (`LruCache`) 来优化资源消耗和重复计算。
 *
 * What: 功效作用
 *   该文件提供了以下具体功能：
 *   1.  **类型别名 (`KLog`)**: 为 `com.orhanobut.logger.Logger` 创建了一个更简洁的别名 `KLog`，方便日志记录。
 *   2.  **全局 GSON 实例**: 提供一个经过特殊配置的 `GSON` 单例，能够处理 `Int` 和 `Date` 类型的反序列化异常，并排除了带有 `TRANSIENT` 修饰符的字段。
 *   3.  **全局 Toast 工具**: `toast` 函数提供了一个简单、线程安全的方式来显示 Toast 消息，并复用同一个 `Toast` 实例以避免消息堆积。
 *   4.  **文件目录创建工具**: `createDir` 函数封装了创建文件目录的逻辑，并处理了路径已存在（但为文件）等边缘情况。
 *   5.  **URL 处理扩展**: 为 `String` 类添加了 `urlHost` 和 `urlPath` 两个扩展属性，可以方便地从一个 URL 字符串中提取其主机名和路径。内部使用 `LruCache` 缓存 `Uri` 对象，以提高重复解析的性能。
 *
 * How: 核心技术
 *   1.  **顶层声明**: 文件中的函数和属性都是顶层声明，这意味着它们不属于任何类，可以直接通过包名导入和使用，这是 Kotlin 实现全局函数的推荐方式。
 *   2.  **懒加载 (`by lazy`)**: `GSON`、`TOAST` 和 `urlCache` 都使用了 `by lazy` 委托属性，确保它们的初始化是线程安全的，并且只在首次被访问时执行一次。
 *   3.  **GsonBuilder 配置**: `GSON` 实例通过 `GsonBuilder` 进行了深度定制：
 *      *   `excludeFieldsWithModifiers(TRANSIENT)`: 忽略 Java 中的 `transient` 关键字，这在序列化时很有用。
 *      *   `registerTypeAdapter`: 为 `Int` 和 `Date` 注册了自定义的 `JsonDeserializer`，以增强反序列化过程的健壮性，能够处理 null、空字符串或格式错误的数据。
 *   4.  **扩展属性**: `urlHost` 和 `urlPath` 是对 `String` 类的扩展属性。它们利用 `get()` 方法提供了自定义的取值逻辑，使得从字符串中获取 URL 部分就像访问一个普通属性一样自然。
 *   5.  **LruCache**: 在 URL 解析中引入了 `LruCache`，将 `String` 形式的 URL 与其解析后的 `Uri` 对象进行映射和缓存。这避免了对同一个 URL 字符串的重复、昂贵的解析操作，是一个典型的空间换时间优化策略。
 *   6.  **线程切换 (`postMain`)**: `toast` 函数内部调用了 `postMain`（推测是切换到 Android 主线程的工具函数），确保了 UI 操作（显示 Toast）总是在主线程执行，避免了多线程问题。
 */
package me.jbusdriver.base

import android.net.Uri
import android.support.v4.util.LruCache
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.orhanobut.logger.Logger
import java.io.File
import java.lang.reflect.Modifier.TRANSIENT
import java.util.*

typealias  KLog = Logger

val GSON by lazy {
    GsonBuilder().excludeFieldsWithModifiers(TRANSIENT)
        .registerTypeAdapter(Int::class.java, JsonDeserializer<Int> { json, _, _ ->
            if (json.isJsonNull || json.asString.isEmpty()) {
                return@JsonDeserializer null
            }
            try {
                return@JsonDeserializer json.asInt
            } catch (e: NumberFormatException) {
                return@JsonDeserializer null
            }
        }).registerTypeAdapter(Date::class.java, JsonDeserializer { json, _, _ ->
        try {
            return@JsonDeserializer Date(json.asJsonPrimitive.asString)
        } catch (e: Exception) {
            return@JsonDeserializer Date()
        }
    }).serializeNulls().create()
}


private val TOAST: Toast by lazy { Toast.makeText(JBusManager.context.applicationContext, "", Toast.LENGTH_LONG) }

fun toast(str: String, duration: Int = Toast.LENGTH_LONG) {
    postMain {
        TOAST.setText(str)
        TOAST.duration = duration
        TOAST.show()
    }

}


fun createDir(collectDir: String): String? {
    File(collectDir.trim()).let {
        try {
            if (!it.exists() && it.mkdirs()) return collectDir
            if (it.exists()) {
                if (it.isDirectory) {
                    return collectDir
                } else {
                    it.delete()
                    createDir(collectDir) //recreate
                }
            }
        } catch (e: Exception) {
//            MobclickAgent.reportError(JBus, e)
        }
    }
    return null
}

private val urlCache by lazy { LruCache<String, Uri>(512) }

//string url -> get url host
val String.urlHost: String
    get() = (urlCache.get(this) ?: let {
        val uri = Uri.parse(this)
        urlCache.put(this, uri)
        uri
    }).let {
        checkNotNull(it)
        "${it.scheme}://${it.host}"
    }


val String.urlPath: String
    get() = (urlCache.get(this) ?: let {
        val uri = Uri.parse(this)
        urlCache.put(this, uri)
        uri
    })?.path ?: ""