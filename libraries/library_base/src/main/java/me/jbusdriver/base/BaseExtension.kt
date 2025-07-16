/**
 * Why: 设计目的
 * `BaseExtension.kt` 是一个 Kotlin 扩展函数和属性的集合，其核心设计目的是为了：
 * 1.  **增强原生类的能力**：通过为 Android SDK 和其他库中的现有类（如 `Context`, `Long`, `Gson`, `Flowable`, `Cursor`）添加新的函数和属性，使其更易于使用，功能更强大。
 * 2.  **减少模板代码**：将常用的、重复性的代码片段封装成简洁的扩展，例如 dp/px 转换、View 加载、复制粘贴、获取屏幕宽度等，从而提高开发效率。
 * 3.  **提高代码可读性**：使用具有描述性的名称（如 `formatFileSize`, `copy`, `paste`, `toJsonString`）来代替复杂的原生 API 调用，使代码的意图更加清晰，更接近自然语言。
 * 4.  **促进链式调用**：许多扩展被设计为可以无缝地融入链式调用中，特别是在处理 RxJava 的 `Flowable` 或 `Gson` 的序列化/反序列化时。
 * 5.  **中心化工具函数**：将散落在项目中各处的通用工具函数集中到一个文件中，便于管理、复用和维护。
 */

/**
 * What: 功效作用
 * 该文件提供了覆盖多个方面的扩展功能：
 * - **尺寸单位转换**: 定义了 KB, MB, GB, TB 常量，并提供了 `Long.formatFileSize()` 用于将字节数格式化为人类可读的字符串（如 “1.5 GB”）。
 * - **集合创建**: `arrayMapof()` 函数提供了创建 `ArrayMap` 的便捷方式，类似于 `mapOf()`。
 * - **资源转换**: `Int.toColorInt()` 将颜色资源 ID 转换为颜色整数值，并处理了版本兼容性问题。
 * - **Context 扩展**: 提供了大量针对 `Context` 的扩展，包括：
 *     - `inflater`: 快速获取 `LayoutInflater`。
 *     - `displayMetrics`: 快速获取 `DisplayMetrics`。
 *     - `dpToPx`/`pxToDp`: dp 和 px 单位的相互转换。
 *     - `inflate`: 简化 View 的加载过程。
 *     - `screenWidth`: 获取屏幕宽度。
 *     - `spanCount`: 根据屏幕宽度动态计算 `GridLayoutManager` 的列数。
 *     - `copy`/`paste`: 封装了剪贴板的复制和粘贴操作。
 *     - `packageInfo`: 安全地获取应用的 `PackageInfo`。
 *     - `browse`: 启动浏览器打开一个 URL，并提供了错误处理回调。
 * - **线程调度**: `Main_Worker` 和 `IO_Worker` 提供了 RxJava 的 `Worker` 实例，`postMain` 函数可以方便地将任务调度到主线程执行。
 * - **Gson 扩展**: `Gson.fromJson<T>()` 使用了 reified 泛型，简化了泛型类型的反序列化。`Any?.toJsonString()` 提供了将任意对象转换为 JSON 字符串的快捷方法。
 * - **RxJava 扩展**: `Flowable<R>.addUserCase()` 为 `Flowable` 添加了一组标准的处理流程，包括设置超时、在 IO 线程订阅、只取第一个有效结果。
 * - **Cursor 扩展**: `getStringByColumn`, `getIntByColumn`, `getLongByColumn` 使得从 `Cursor` 中按列名获取数据更加安全和便捷，并提供了默认值处理。
 * - **SharedPreferences 工具**: `getSp` 和 `saveSp` 提供了对 `SharedPreferences` 的简单读写封装。
 */

/**
 * How: 核心技术
 * 1.  **Kotlin 扩展函数/属性**: 这是整个文件的基石。通过 `fun ClassName.functionName()` 或 `val ClassName.propertyName` 的语法，为已有的类添加新功能，而无需修改其源码。
 * 2.  **顶层声明**: 所有的函数和属性都定义在文件的顶层，而不是在类内部，这使得它们在整个模块中都可以被直接调用。
 * 3.  **Reified 泛型参数 (`reified T`)**: 在 `Gson.fromJson` 中使用，它允许在运行时获取泛型 `T` 的实际类型，从而避免了传递 `TypeToken` 的麻烦，极大地简化了 API。
 * 4.  **懒加载 (`by lazy`)**: `Main_Worker` 和 `IO_Worker` 使用 `by lazy` 进行初始化。这意味着 `Scheduler.createWorker()` 只有在第一次访问这两个属性时才会被调用，实现了延迟初始化和线程安全。
 * 5.  **操作符重载/约定**: 虽然此文件中没有直接的例子，但扩展函数的思想与操作符重载一脉相承，都是为了让 API 更简洁、更符合直觉。
 * 6.  **高阶函数**: `browse` 和 `postMain` 等函数接受 lambda 表达式作为参数（`errorHandler: (Throwable) -> Unit`, `block: () -> Unit`），使得调用方可以方便地传递行为（如错误处理逻辑、要执行的任务）。
 * 7.  **版本判断**: 在 `getColor` 中，通过 `Build.VERSION.SDK_INT` 判断 Android 版本，并调用相应的新旧 API，确保了向后兼容性。
 * 8.  **`@Nullable` 注解**: 用于标记 `Gson.fromJson` 的返回值可能为 null，为 Java 互操作性和静态分析工具提供了有用的信息。
 */
package me.jbusdriver.base

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.support.annotation.Nullable
import android.support.v4.util.ArrayMap
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

//region Size

const val KB = 1024.0
const val MB = KB * 1024
const val GB = MB * 1024
const val TB = GB * 1024

fun Long.formatFileSize(): String = Formatter.formatFileSize(JBusManager.manager.first().get(), this)
//endregion

//region array map
fun <K, V> arrayMapof(vararg pairs: Pair<K, V>): ArrayMap<K, V> = ArrayMap<K, V>(pairs.size).apply { putAll(pairs) }

fun <K, V> arrayMapof(): ArrayMap<K, V> = ArrayMap()
//endregion

//region convert
fun Int.toColorInt() = getColor(this)

private fun getColor(id: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        JBusManager.context.resources.getColor(id, null)
    } else JBusManager.context.resources.getColor(id)
}
//endregion


//region Context
val Main_Worker by lazy { AndroidSchedulers.mainThread().createWorker() }
val IO_Worker by lazy { Schedulers.io().createWorker() }

fun postMain(block: () -> Unit) = Main_Worker.schedule(block)


val Context.inflater: LayoutInflater
    get() = LayoutInflater.from(this)

val Context.displayMetrics: DisplayMetrics
    get() = resources.displayMetrics


fun Context.dpToPx(dp: Float) = (dp * this.displayMetrics.density + 0.5).toInt()

fun Context.pxToDp(px: Float) = (px / this.displayMetrics.density + 0.5).toInt()


private fun inflateView(
    context: Context, layoutResId: Int, parent: ViewGroup?,
    attachToRoot: Boolean
): View =
    LayoutInflater.from(context).inflate(layoutResId, parent, attachToRoot)

fun Context.inflate(layoutResId: Int, parent: ViewGroup? = null, attachToRoot: Boolean = false): View =
    inflateView(this, layoutResId, parent, attachToRoot)
//endregion


//region gson
@Nullable
inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)
//inline fun <reified T> Gson.fromJson(json: JsonElement) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)
//inline fun <reified T> Gson.fromJson(json: Reader) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)
//inline fun <reified T> Gson.fromJson(json: JsonReader) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun Any?.toJsonString() = GSON.toJson(this)
//endregion


//region http
fun <R> Flowable<R>.addUserCase(sec: Int = 12) =
    this.timeout(sec.toLong(), TimeUnit.SECONDS, Schedulers.io()) //超时
        .subscribeOn(Schedulers.io())
        .take(1)
        .filter { it != null }
//endregion

//region screenWidth
val Context.screenWidth: Int
    inline get() {
        val wm = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(this.displayMetrics)
        return displayMetrics.widthPixels
    }

val Context.spanCount: Int
    inline get() = with(this.screenWidth) {
        when {
            this <= 1080 -> 3
            this <= 1440 -> 4
            else -> 5
        }
    }
//endregion

//region copy paste
/**
 * 实现文本复制功能
 * add by wangqianzhou
 * @param content
 */
fun Context.copy(content: String) {
    // 得到剪贴板管理器
    val cmb = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cmb.primaryClip = ClipData.newPlainText(null, content)
}

/**
 * 实现粘贴功能
 * add by wangqianzhou
 * *
 * @return
 */
fun Context.paste(): String? {
    // 得到剪贴板管理器
    val cmb = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return cmb.primaryClip?.let {
        if (it.itemCount > 0) it.getItemAt(0).coerceToText(this)?.toString() else null
    }
}
//endregion

//region package info
val Context.packageInfo: PackageInfo?
    get() = try {
        this.packageManager.getPackageInfo(
            this.packageName, 0
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
//endregion

//region cursor
fun Cursor.getStringByColumn(colName: String): String? =
    try {
        this.getString(this.getColumnIndexOrThrow(colName))
    } catch (ex: Exception) {
        ""
    }

fun Cursor.getIntByColumn(colName: String): Int = try {
    this.getInt(this.getColumnIndexOrThrow(colName))
} catch (ex: Exception) {
    -1
}

fun Cursor.getLongByColumn(colName: String): Long = try {
    this.getLong(this.getColumnIndexOrThrow(colName))
} catch (ex: Exception) {
    -1
}
//endregion


fun Context.browse(url: String, errorHandler: (Throwable) -> Unit = {}) {
    try {
        startActivity(Intent().apply {
            this.action = "android.intent.action.VIEW"
            this.data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

        })
    } catch (e: Exception) {
        errorHandler(e)
    }
}

/*SharedPreferences*/

fun getSp(key: String): String? =
    JBusManager.context.applicationContext.getSharedPreferences("config", Context.MODE_PRIVATE).getString(key, null)

fun saveSp(key: String, value: String) = Schedulers.io().scheduleDirect {
    JBusManager.context.applicationContext.getSharedPreferences(
        "config",
        Context.MODE_PRIVATE
    ).edit().putString(key, value).apply()
}


