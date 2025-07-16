/**
 * @author jbusdriver
 * @description This file provides a mechanism to monitor the progress of file downloads using OkHttp.
 * It includes an interface for progress listeners, a global dispatcher for progress events, and a custom ResponseBody to intercept and report download progress.
 *
 * Why: 设计目的
 * 在很多应用场景下，如下载文件、加载大图等，用户需要看到一个明确的进度指示，以了解当前下载的状态和预估剩余时间。原生的 OkHttp `ResponseBody` 并不直接提供进度的回调。
 * `OkHttpDownloadProgressManager` 的设计目的就是为了解决这个问题，通过一种非侵入式的方式，为 OkHttp 的网络响应添加进度监听功能。它通过装饰者模式包装原始的 `ResponseBody`，
 * 在数据流被读取时，计算已下载的字节数并通知外部监听器，从而实现对下载进度的实时监控。
 *
 * What: 功效作用
 * 1.  **`OnProgressListener` 接口**: 定义了一个标准的进度监听器接口，包含 `onProgress` 方法，用于接收下载进度更新。回调参数包括下载链接 `url`、已读字节数 `bytesRead`、总字节数 `totalBytes`、是否完成 `isDone` 以及可能发生的异常 `exception`。
 * 2.  **全局监听器管理**: 
 *     -   提供 `addProgressListener` 和 `removeProgressListener` 方法，用于动态地注册和注销全局的进度监听器。
 *     -   使用 `CopyOnWriteArrayList<WeakReference<OnProgressListener>>` 来存储监听器列表。`CopyOnWriteArrayList` 保证了在遍历监听器时进行添加或删除操作的线程安全；`WeakReference` 则避免了因监听器未被及时注销而导致的内存泄漏。
 * 3.  **`GlobalProgressListener`**: 这是一个单例的 `OnProgressListener` 实现，它的作用是将进度事件分发给所有已注册的全局监听器。当 `ProgressResponseBody` 上报进度时，会通过这个全局监听器通知所有关心进度的模块。
 * 4.  **`ProgressResponseBody`**: 这是一个自定义的 `ResponseBody`，它包装了原始的 `ResponseBody`。核心在于重写了 `source()` 方法，并返回一个经过包装的 `ForwardingSource`。在这个 `ForwardingSource` 的 `read` 方法中，每次从网络流中读取数据时，都会累加已读字节数，并通过 `progressListener` 回调出去。
 * 5.  **与 `NetClient` 集成**: 这个进度管理机制通常与 `NetClient` 中的 `PROGRESS_INTERCEPTOR` 配合使用。拦截器会判断响应是否需要进度监听，如果需要，就将原始的 `ResponseBody` 替换为 `ProgressResponseBody`，从而激活进度监控。
 *
 * How: 核心技术
 * 1.  **装饰者模式 (Decorator Pattern)**: `ProgressResponseBody` 是对 `okhttp3.ResponseBody` 的一个装饰。它在不改变 `ResponseBody` 接口的前提下，为其增加了进度监听的功能。这是通过持有原始 `ResponseBody` 的引用，并代理其 `contentType()` 和 `contentLength()` 等方法实现的。
 * 2.  **`okio.ForwardingSource`**: 这是 Okio 库提供的一个工具类，用于包装一个 `Source`（数据源）并拦截其 `read` 方法。`ProgressResponseBody` 正是利用它来监控从网络流中读取数据的过程。在重写的 `read` 方法中，每次调用 `super.read()` 之后，就可以获取到本次读取的字节数，从而计算出总的下载进度。
 * 3.  **弱引用 (`WeakReference`)**: 为了防止内存泄漏，全局监听器列表 `listeners` 中存放的是 `WeakReference<OnProgressListener>`。如果一个 `OnProgressListener` 的实例在外部不再被强引用（例如，一个 Activity 被销毁了），垃圾回收器就可以回收它。在分发进度事件时，会检查 `it.get()` 是否为 `null`，如果为 `null`，说明监听器已被回收，就从列表中移除对应的弱引用。
 * 4.  **`CopyOnWriteArrayList`**: 这是一个线程安全的 `List` 实现，其特点是“写入时复制”。当有修改操作（add, remove）时，它会创建一个新的底层数组，这使得在遍历（read）操作时不会受到修改操作的干扰，避免了 `ConcurrentModificationException`，非常适合“读多写少”的监听器列表场景。
 * 5.  **Kotlin 扩展与懒加载**: 使用 `by lazy` 实现了 `listeners` 的懒加载，只有在第一次访问时才会创建实例。文件顶层的 `GlobalProgressListener` 和 `add/remove` 方法，利用了 Kotlin 的文件级函数和属性的特性，提供了一个简洁的全局访问入口。
 */
package me.jbusdriver.base.http

import okhttp3.ResponseBody
import okio.*
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList


interface OnProgressListener {

    fun onProgress(url: String, bytesRead: Long, totalBytes: Long, isDone: Boolean, exception: Exception?)
}

private val listeners by lazy { CopyOnWriteArrayList<WeakReference<OnProgressListener>>() }

val GlobalProgressListener
    get() = object : OnProgressListener {
        override fun onProgress(
            url: String,
            bytesRead: Long,
            totalBytes: Long,
            isDone: Boolean,
            exception: Exception?
        ) {
            if (listeners.isEmpty()) return
            listeners.forEach {
                it.get()?.onProgress(url, bytesRead, totalBytes, isDone, exception)
                    ?: listeners.remove(it)
            }
        }
    }


fun addProgressListener(progressListener: OnProgressListener) {
    if (findProgressListener(progressListener) == null) {
        listeners.add(WeakReference(progressListener))
    }
}

fun removeProgressListener(progressListener: OnProgressListener) {
    val listener = findProgressListener(progressListener)
    if (listener != null) {
        listeners.remove(listener)
    }
}

private fun findProgressListener(listener: OnProgressListener): WeakReference<OnProgressListener>? =
    listeners.find { it.get() == listener }

class ProgressResponseBody(
    private val imageUrl: String,
    private val responseBody: ResponseBody?,
    private val progressListener: OnProgressListener?
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null

    override fun contentType() = responseBody?.contentType()

    override fun contentLength() = responseBody?.contentLength() ?: 0L

    override fun source(): BufferedSource? {
        if (bufferedSource == null && responseBody != null) {
            bufferedSource = Okio.buffer(source(responseBody.source()))
        }
        return bufferedSource
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            private var totalBytesRead: Long = 0

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead == -1L) 0 else bytesRead

                progressListener?.onProgress(imageUrl, totalBytesRead, contentLength(), bytesRead == -1L, null)
                return bytesRead
            }
        }
    }
}