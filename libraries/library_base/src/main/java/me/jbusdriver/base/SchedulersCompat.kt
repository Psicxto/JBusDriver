/**
 * Why: 设计目的
 * SchedulersCompat 是一个工具类，旨在简化在 Android 应用中使用 RxJava 时的线程调度逻辑。
 * 在响应式编程中，尤其是在与 UI 交互的场景下，开发者需要频繁地在后台线程（用于执行耗时操作，如网络请求、数据库读写）和主线程（用于更新 UI）之间切换。
 * 直接编写 `subscribeOn()` 和 `observeOn()` 会导致代码重复和模板化。该类的目的就是将这些常用的线程切换模式封装成可重用的 `FlowableTransformer`，从而：
 * 1.  **减少模板代码**：避免在每个 RxJava 链中重复写 `subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())`。
 * 2.  **提高可读性**：使用如 `compose(SchedulersCompat.io())` 这样的声明式代码，使意图更清晰。
 * 3.  **统一管理**：将线程调度策略集中在一个地方，便于未来统一修改或扩展，例如添加统一的错误处理或日志记录。
 * 4.  **处理退订线程**：统一在 `Schedulers.single()` 线程上处理退订逻辑，确保退订操作的有序性和线程安全。
 */

/**
 * What: 功效作用
 * 该对象提供了一系列静态方法，每个方法返回一个 `FlowableTransformer<T, T>`。这些 `Transformer` 可以通过 `Flowable` 的 `compose()` 操作符应用到任何 RxJava 数据流上。
 * 1.  **computation()**: 将上游操作调度到 `Schedulers.computation()` 线程池（适用于计算密集型任务），并将结果回调到 Android 主线程。
 * 2.  **io()**: 将上游操作调度到 `Schedulers.io()` 线程池（适用于 I/O 密集型任务，如网络、磁盘操作），并将结果回调到 Android 主线程。这是 Android 开发中最常用的模式。
 * 3.  **single()**: 将上游操作调度到 `Schedulers.single()`，这是一个单线程的线程池，确保任务按顺序执行，并将结果回调到 Android 主线程。
 * 4.  **newThread()**: 为每个任务创建一个新线程来执行上游操作，并将结果回调到 Android 主线程。
 * 5.  **trampoline()**: 将任务调度到当前线程的队列中，等待当前任务执行完毕后再执行，并将结果回调到 Android 主线程。
 * 6.  **mainThread()**: 不改变上游的执行线程，仅将下游的观察者切换到 Android 主线程。适用于上游操作已经在期望的线程执行，只需确保 UI 更新在主线程的场景。
 * 7.  **统一退订线程**: 所有 `Transformer` 都使用 `.unsubscribeOn(Schedulers.single())` 来指定退订操作发生在 `single` 线程上。这可以防止在某些特定情况下（例如，在主线程上退订一个阻塞的 I/O 操作）可能引发的问题。
 */

/**
 * How: 核心技术
 * 1.  **Kotlin 单例对象 (`object`)**: `SchedulersCompat` 被定义为一个 `object`，这在 Kotlin 中是实现单例模式的最简洁方式。确保了全局只有一个实例，符合工具类的设计。
 * 2.  **RxJava `FlowableTransformer`**: 这是核心。`FlowableTransformer` 是一个函数接口，它接收一个 `Flowable` 并返回另一个 `Flowable`。它允许将一系列操作符封装起来，并通过 `compose()` 方法应用到流上，而不会破坏链式调用。
 * 3.  **高阶函数**: 每个方法（如 `io()`）都返回一个 `FlowableTransformer` 的实例，这个实例是通过一个 lambda 表达式创建的。这体现了函数式编程的思想，将行为（线程切换）作为对象返回。
 * 4.  **RxJava Schedulers**: 利用了 RxJava 内置的各种 `Scheduler`：
 *     *   `Schedulers.computation()`: CPU 密集型任务线程池。
 *     *   `Schedulers.io()`: I/O 密集型任务线程池，内部实现是无界的缓存线程池。
 *     *   `Schedulers.single()`: 单一后台线程，保证任务串行执行。
 *     *   `Schedulers.newThread()`: 每次都创建新线程。
 *     *   `Schedulers.trampoline()`: 在当前线程延迟执行。
 * 5.  **AndroidSchedulers.mainThread()**: 这是 `rxandroid` 库提供的 `Scheduler`，专门用于将操作调度到 Android 的主线程（UI 线程），是实现与 UI 安全交互的关键。
 * 6.  **@JvmStatic 注解**: 这个注解使得 Kotlin `object` 中的方法可以像 Java 中的静态方法一样被调用，增强了与 Java 代码的互操作性。
 */
package me.jbusdriver.base

import io.reactivex.FlowableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Joker on 2015/8/10.
 */
object SchedulersCompat {
    /**
     * Don't break the chain: use RxJava's compose() operator
     */
    @JvmStatic
    fun <T> computation(): FlowableTransformer<T, T> =
        FlowableTransformer {
            it.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.single())
        }

    @JvmStatic
    fun <T> io(): FlowableTransformer<T, T> =
        FlowableTransformer {
            it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.single())
        }

    @JvmStatic
    fun <T> single(): FlowableTransformer<T, T> =
        FlowableTransformer {
            it.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.single())
        }

    @JvmStatic
    fun <T> newThread(): FlowableTransformer<T, T> =
        FlowableTransformer {
            it.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.single())
        }

    @JvmStatic
    fun <T> trampoline(): FlowableTransformer<T, T> =
        FlowableTransformer {
            it.subscribeOn(Schedulers.trampoline()).observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.single())
        }

    @JvmStatic
    fun <T> mainThread(): FlowableTransformer<T, T> =
        FlowableTransformer { it.observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.single()) }
}
