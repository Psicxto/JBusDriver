/**
 * @author J.query
 * @date 2019/4/15
 * @email j-query@foxmail.com
 * Why: 设计目的
 *   `RxBus` 是一个基于 RxJava 实现的事件总线（Event Bus）。其设计目的在于提供一个轻量级、解耦的组件间通信机制。
 *   在 Android 应用中，不同组件（如 Activities, Fragments, Services）之间经常需要进行通信，传统的通信方式（如接口回调、BroadcastReceiver、Handler）
 *   往往会导致代码耦合度高、逻辑复杂。`RxBus` 利用响应式编程的思想，旨在解决以下问题：
 *   1.  **解耦**：允许事件的发布者和订阅者之间完全不知道对方的存在，只需共享事件（Event）对象模型即可。
 *   2.  **简化通信**：将复杂的组件间通信简化为“发布事件”和“订阅事件”两个操作。
 *   3.  **线程安全**：提供一个线程安全的事件总线，可以在任何线程发布事件，并在指定的线程订阅和处理事件。
 *
 * What: 功效作用
 *   `RxBus` 提供了以下核心功能：
 *   1.  **事件发布 (`post`)**: `post(obj: Any)` 方法允许应用的任何部分发布一个任意类型的事件对象。
 *   2.  **事件订阅 (`toFlowable`)**: `toFlowable(clz: Class<T>)` 方法允许订阅者根据事件的 `Class` 类型来过滤和订阅事件流。返回的是一个 `Flowable<T>`，订阅者可以利用 RxJava 强大的操作符链对其进行处理（如线程切换、过滤、转换等）。
 *   3.  **背压策略**: 在转换为 `Flowable` 时，使用了 `BackpressureStrategy.DROP` 策略。这意味着如果下游订阅者处理事件的速度跟不上上游发布事件的速度，新的事件将会被丢弃。这是一种适用于不要求所有事件都必须被处理的场景的策略。
 *   4.  **订阅者检查 (`hasSubscribers`)**: `hasSubscribers()` 方法可以检查当前是否有任何订阅者正在监听总线。
 *
 * How: 核心技术
 *   1.  **单例模式 (`object`)**: 使用 Kotlin 的 `object` 关键字，确保 `RxBus` 在整个应用中是唯一的实例，这是事件总线模式的典型实现方式。
 *   2.  **RxRelay (`PublishRelay`)**: `RxBus` 的核心是 `PublishRelay`，它来自 Jake Wharton 的 `RxRelay` 库。`PublishRelay` 类似于 RxJava 中的 `PublishSubject`，但有一个关键区别：它不会因为上游的 `onError` 或 `onComplete` 事件而终止。这意味着即使某个事件流出错，事件总线本身依然可以继续工作，这对于一个长生命周期的全局总线来说至关重要。
 *   3.  **序列化 (`toSerialized()`)**: 调用 `toSerialized()` 方法将 `PublishRelay` 包装成一个线程安全的版本。这确保了可以从多个线程并发地调用 `post` 方法而不会产生竞态条件。
 *   4.  **类型过滤 (`ofType`)**: `ofType(clz: Class<T>)` 是 RxJava 的一个关键操作符，它能够从事件流中只选择指定类型的事件进行传递，这是实现按类型订阅功能的核心。
 *   5.  **Flowable 与背压**: `toFlowable(BackpressureStrategy.DROP)` 将 “热” 的 `Relay` 转换为支持背压的 `Flowable`。`PublishRelay` 本身不处理背压，通过这种转换，`RxBus` 能够适应响应式流的规范，并明确处理了生产者-消费者速率不匹配的情况。
 */
package me.jbusdriver.base

import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.BackpressureStrategy

object RxBus {
    private val mBus = PublishRelay.create<Any>().toSerialized()

    fun post(obj: Any) {
        mBus.accept(obj)
    }

    fun <T> toFlowable(clz: Class<T>) = mBus.ofType(clz).toFlowable(BackpressureStrategy.DROP)

    fun hasSubscribers() = mBus.hasObservers()
}