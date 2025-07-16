/**
 * @author J.query
 * @date 2019/4/15
 * @email j-query@foxmail.com
 * Why: 设计目的
 *   `JBusManager` 是一个单例对象，其核心设计目的是在整个 Android 应用中提供一个可靠的、全局的 `Context` 访问点，
 *   并同时管理应用的 `Activity` 栈。这解决了在非 `Activity` 或 `Fragment` 环境（如工具类、后台服务）中安全获取 `Context` 的常见难题。
 *   具体目的包括：
 *   1.  **全局 Context 提供者**：避免在代码中到处传递 `Context` 对象，简化代码结构。
 *   2.  **Activity 栈管理**：通过 `Application.ActivityLifecycleCallbacks` 接口，自动追踪所有 `Activity` 的生命周期，维护一个存活的 `Activity` 列表。
 *   3.  **防止内存泄漏**：使用 `WeakReference` 来持有 `Activity` 和 `Application` 的引用，避免因为长生命周期的单例持有短生命周期的 `Activity` 实例而导致的内存泄漏。
 *
 * What: 功效作用
 *   `JBusManager` 提供了以下功能：
 *   1.  **自动 Activity 注册与注销**：实现了 `ActivityLifecycleCallbacks` 接口，当 `Activity` 创建时，将其弱引用添加到 `manager` 列表中；当 `Activity` 销毁时，自动从列表中移除。
 *   2.  **安全的 Context 获取**：`context` 属性提供了一个智能的获取机制。它首先尝试从 `Activity` 栈中获取一个可用的 `Activity` 作为 `Context`，如果失败（比如所有 `Activity` 都已被销毁），则回退到使用 `Application` 的 `Context`。这种策略确保了在应用存活期间总能获取到一个有效的 `Context`。
 *   3.  **应用初始化入口**：`setContext(app: Application)` 方法需要在 `Application` 的 `onCreate` 中调用，用于初始化 `JBusManager` 并传入 `Application` 的 `Context`。
 *
 * How: 核心技术
 *   1.  **单例模式 (`object`)**: 使用 Kotlin 的 `object` 关键字，保证 `JBusManager` 在应用中是唯一的实例，便于全局状态管理。
 *   2.  **ActivityLifecycleCallbacks**: 这是 Android Framework 提供的接口，用于监听应用中所有 `Activity` 的生命周期事件。`JBusManager` 通过实现这个接口，实现了对 `Activity` 栈的无侵入式自动管理。
 *   3.  **弱引用 (`WeakReference`)**: `manager` 列表存储的是 `WeakReference<Activity>`，而不是 `Activity` 的强引用。这意味着当 `Activity` 不再被其他地方强引用时，垃圾回收器可以回收它，即使 `JBusManager` 这个单例还持有对它的引用。这是避免 `Context` 相关内存泄漏的关键技术。
 *   4.  **备用上下文 (`Fallback Context`)**: `context` 的 `get()` 方法实现了一个优雅的降级策略。优先使用 `Activity` 的 `Context`，因为它通常与 UI 相关。如果获取不到，则使用 `Application` 的 `Context` 作为备用。`Application` 的 `Context` 生命周期与应用一样长，因此总是可用的。
 *   5.  **空安全与错误处理**: `context` 的 getter 方法使用了 Kotlin 的安全调用 `?.` 和 elvis 操作符 `?:`，并在最后使用 `error()` 函数。如果连 `Application` 的 `Context` 都获取不到（意味着 `setContext` 未被调用），程序会立即抛出异常，这是一种 “快速失败” 的设计哲学，能帮助开发者尽早发现初始化问题。
 */
package me.jbusdriver.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.lang.ref.WeakReference

object JBusManager : Application.ActivityLifecycleCallbacks {

    val manager = mutableListOf<WeakReference<Activity>>()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        manager.add(WeakReference(activity))
    }

    override fun onActivityStarted(activity: Activity?) {
    }

    override fun onActivityPaused(activity: Activity?) {
    }

    override fun onActivityResumed(activity: Activity?) {
    }

    override fun onActivityStopped(activity: Activity?) {
    }

    override fun onActivityDestroyed(activity: Activity?) {
        manager.removeAll { it.get() == activity }
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }

    val context: Context
        get() = manager.firstOrNull()?.get() as? Context
            ?: ref.get() ?: error("can't get context")

    private lateinit var ref: WeakReference<Context>
    fun setContext(app: Application) {
        this.ref = WeakReference(app)
    }
}