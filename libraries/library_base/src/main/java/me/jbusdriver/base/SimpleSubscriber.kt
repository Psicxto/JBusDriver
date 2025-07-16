/**
 * Why: 设计目的
 * `SimpleSubscriber` 是一个为 RxJava 的 `Flowable` 设计的简化版订阅者（Subscriber）基类。其主要设计目的如下：
 * 1.  **简化模板代码**：在使用 RxJava 时，`Subscriber` 的 `onNext`, `onError`, `onComplete` 三个方法通常都需要实现。然而，在很多场景下，开发者可能只关心 `onNext`，或者只需要一个通用的错误处理逻辑。`SimpleSubscriber` 提供了一个默认的实现，开发者只需继承它并重写自己关心的方法即可，从而减少了样板代码。
 * 2.  **提供默认的错误处理**：它内置了一个基础的 `onError` 实现，能够打印错误堆栈、记录日志，并处理常见的 HTTP 错误（如 404 Not Found）。这为应用提供了一个统一的、基础的错误反馈机制，避免了在每个订阅者中重复编写相似的错误处理代码。
 * 3.  **自动资源管理**：在 `onComplete` 和 `onError` 方法中，它会自动调用 `cancel()`（继承自 `DisposableSubscriber` 的 `dispose()`），确保订阅在完成或出错后能够被及时取消，从而释放资源，防止内存泄漏。
 * 4.  **提高代码可读性**：通过继承 `SimpleSubscriber`，代码的意图变得更加清晰。开发者可以专注于核心的业务逻辑（通常在 `onNext` 中），而不是被 RxJava 的回调细节所淹没。
 */

/**
 * What: 功效作用
 * `SimpleSubscriber` 作为一个开放的（`open`）泛型类，提供了以下具体功能：
 * 1.  **默认的 `onComplete` 实现**: 当数据流正常结束时，此方法被调用，并立即取消订阅。
 * 2.  **默认的 `onError` 实现**: 当数据流中发生错误时，此方法被调用。它的行为包括：
 *     *   打印错误的堆栈跟踪 (`e.printStackTrace()`)。
 *     *   使用 `KLog` 记录错误信息。
 *     *   检查错误是否为 `HttpException`。如果是，会根据 HTTP 状态码进行处理。例如，对于 `404` 错误，它会弹出一个 “没有结果” 的 `Toast` 提示。
 *     *   最后，取消订阅。
 * 3.  **空的 `onNext` 实现**: `onNext` 方法被定义为空实现。这强制要求子类必须根据自己的业务需求来重写此方法，以处理接收到的数据 `t`。
 * 4.  **自动获取 TAG**: 它会自动使用当前子类的类名作为日志的 `TAG`，方便调试和追踪。
 */

/**
 * How: 核心技术
 * 1.  **继承 `DisposableSubscriber<T>`**: `SimpleSubscriber` 继承自 RxJava 2 的 `DisposableSubscriber`。这是一个抽象类，它实现了 `FlowableSubscriber` 和 `Disposable` 接口。关键在于 `Disposable` 接口，它提供了 `dispose()` 和 `isDisposed()` 方法，使得订阅可以被手动取消，这是防止内存泄漏的关键机制。
 * 2.  **泛型 (`<T>`)**: 类被定义为泛型，使其可以处理任何类型的数据流，增强了其通用性和可重用性。
 * 3.  **Kotlin 的 `open` 关键字**: `SimpleSubscriber` 类及其方法被标记为 `open`，这意味着它们可以被其他类继承和重写。这是实现其作为基类设计的核心。
 * 4.  **`instanceof` 检查 (`is HttpException`)**: 在 `onError` 方法中，使用了 Kotlin 的 `is` 操作符（相当于 Java 的 `instanceof`）来检查异常类型，从而可以针对特定类型的异常（如网络请求相关的 `HttpException`）做专门处理。
 * 5.  **`when` 表达式**: 使用 Kotlin 的 `when` 表达式来替代 `switch-case`，根据 `HttpException` 的 `code()` 进行分支处理，代码更简洁、易读。
 * 6.  **依赖注入/全局工具**: 它依赖于全局的 `KLog` 对象进行日志记录和全局的 `toast` 函数来显示提示。这体现了对外部工具的依赖，是架构设计的一部分。
 */
package me.jbusdriver.base

import io.reactivex.subscribers.DisposableSubscriber
import retrofit2.HttpException

/**
 * Created by Administrator on 2016/7/21 0021.
 */
open class SimpleSubscriber<T> : DisposableSubscriber<T>() {

    private val TAG: String = this.javaClass.name

    override fun onComplete() {
        cancel()
    }

    override fun onError(e: Throwable) {
        e.printStackTrace()
        KLog.t(TAG).e("onError >> code = info : ${e.message}")
        if (e is HttpException) {
            when (e.code()) {
                404 -> toast("没有结果")
            }
        }
        cancel()
    }

    override fun onNext(t: T) {
    }


}
