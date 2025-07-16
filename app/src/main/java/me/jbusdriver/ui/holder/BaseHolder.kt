package me.jbusdriver.ui.holder

import android.content.Context
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

/**
 * Holder 的基类，提供了一些通用的功能，主要是为了管理资源和避免内存泄漏。
 * 通常用于 RecyclerView 的 ViewHolder 或其他需要持有 Context 的场景。
 */
open class BaseHolder(context: Context) {
    // 使用弱引用持有 Context，防止因长时间持有 Context 实例而导致的内存泄漏。
    protected val weakRef by lazy { WeakReference(context) }
    // 用于管理 RxJava 的订阅（Disposables），方便在 Holder 生命周期结束时统一取消订阅。
    protected val rxManager by lazy { CompositeDisposable() }

    /**
     * 释放资源的方法。当 Holder 不再需要时（例如 ViewHolder 被回收时），应调用此方法。
     */
    open fun release() {
        // 清空并处理掉所有的 RxJava 订阅，避免异步操作在 Holder 销毁后继续执行。
        rxManager.clear()
        rxManager.dispose()
        // 清除对 Context 的弱引用。
        weakRef.clear()
    }
}