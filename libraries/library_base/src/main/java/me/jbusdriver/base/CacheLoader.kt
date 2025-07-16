/**
 * @author J.query
 * @date 2019/4/15
 * @email j-query@foxmail.com
 * Why: 设计目的
 *   `CacheLoader` 是一个用于管理应用缓存的单例对象，它整合了内存缓存（`LruCache`）和磁盘缓存（`ACache`），
 *   旨在提供一个统一、高效、分层的缓存解决方案。其设计目的在于：
 *   1.  **分层缓存**：结合内存的速度优势和磁盘的持久性与大容量优势，构建二级缓存体系。
 *   2.  **简化缓存操作**：封装复杂的缓存读写逻辑，提供简洁的 API 接口，方便业务层调用。
 *   3.  **异步化与响应式**：利用 RxJava (`Flowable`) 将缓存读取操作异步化，避免阻塞主线程，并以响应式流的形式提供数据。
 *   4.  **动态内存管理**：根据设备的可用内存动态调整 `LruCache` 的大小，以适应不同性能的设备。
 *
 * What: 功效作用
 *   `CacheLoader` 提供了以下核心功能：
 *   1.  **统一缓存入口**：通过 `cacheLruAndDisk`、`cacheLru`、`cacheDisk` 等方法，开发者可以轻松地将数据同时或分别存入内存和磁盘缓存。
 *   2.  **响应式数据读取**：`fromLruAsync` 和 `fromDiskAsync` 方法将缓存读取操作封装成 `Flowable`，允许调用者以非阻塞的方式订阅缓存数据。
 *   3.  **同步数据读取**：`justLru` 和 `justDisk` 方法提供了同步的缓存读取方式，返回一个立即发射数据或完成的 `Flowable`。
 *   4.  **缓存淘汰与管理**：`LruCache` 自动处理内存缓存的淘汰（最近最少使用），而 `ACache` 负责磁盘缓存的生命周期管理。`removeCacheLike` 方法支持按关键字或正则表达式批量删除缓存。
 *   5.  **自动数据转换**：在缓存数据时，自动使用 `GSON` 将对象转换为 JSON 字符串，简化了存储过程。
 *
 * How: 核心技术
 *   1.  **单例模式 (`object`)**: 使用 Kotlin 的 `object` 关键字，确保 `CacheLoader` 在应用中只有一个实例，便于全局访问和状态管理。
 *   2.  **懒加载 (`by lazy`)**: `lru` 和 `acache` 实例使用 `by lazy` 进行初始化，确保只在首次访问时才创建，提高了启动性能并保证了线程安全。
 *   3.  **LruCache**: `LruCache` 是 Android SDK 提供的内存缓存类，`CacheLoader` 通过重写 `sizeOf` 方法来精确计算每个缓存项占用的内存大小，并重写 `entryRemoved` 来监控缓存的移除事件。
 *   4.  **ACache 集成**: `CacheLoader` 依赖之前分析过的 `ACache` 库作为其磁盘缓存层，实现了数据的持久化存储。
 *   5.  **RxJava (`Flowable`)**: `Flowable` 被用来封装异步的缓存读取操作。`interval`、`flatMap`、`timeout`、`take(1)` 等操作符的组合使用，实现了一种轮询式的异步获取机制：在一定时间内（6秒）反复尝试从缓存中获取数据，一旦获取成功或超时则停止。
 *   6.  **动态内存计算**: `initMemCache` 方法通过 `ActivityManager` 获取当前设备的可用内存，并据此动态设定 `LruCache` 的容量，实现了对不同设备的自适应。
 *   7.  **后台任务调度**: `removeCacheLike` 方法使用 `Schedulers.computation().createWorker()` 将缓存清理任务调度到计算线程池执行，避免了在主线程或IO线程上执行可能耗时的批量删除操作。
 */
package me.jbusdriver.base

import android.app.Activity
import android.app.ActivityManager
import android.support.v4.util.LruCache
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import me.jbusdriver.base.JBusManager.context
import java.util.concurrent.TimeUnit


object CacheLoader {
    private const val TAG = "CacheLoader"


    private fun initMemCache(): LruCache<String, String> {
        val memoryInfo = ActivityManager.MemoryInfo()
        val myActivityManager = JBusManager.context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        //获得系统可用内存，保存在MemoryInfo对象上
        myActivityManager.getMemoryInfo(memoryInfo)
        val memSize = memoryInfo.availMem.formatFileSize()
        KLog.t(TAG).d("max availMem = $memSize")
        if (memoryInfo.lowMemory) {
            KLog.w("可能的内存不足")
            toast("当前可用内存:$memSize,请注意释放内存")
        }
        val cacheSize = if (memoryInfo.availMem > 32 * 1024 * 1024) 4 * 1024 * 1024 else 2 * 1024 * 1024
        KLog.t(TAG).d("max cacheSize = ${cacheSize.toLong().formatFileSize()}")
        return object : LruCache<String, String>(cacheSize) { //4m
            override fun entryRemoved(evicted: Boolean, key: String, oldValue: String, newValue: String?) {
                KLog.i(
                    String.format(
                        "entryRemoved : evicted = %s , key = %20s , oldValue = %30s , newValue = %30s",
                        evicted.toString(),
                        key,
                        oldValue,
                        newValue
                    )
                )
                if (evicted) oldValue.let { null } ?: oldValue.let { newValue }
            }

            override fun sizeOf(key: String, value: String): Int {
                val length = value.toByteArray().size
                KLog.i("key = $key  sizeOf = [$length]bytes format:${(this.size() + length).toLong().formatFileSize()}")
                return length
            }

        }
    }

    @JvmStatic
    val lru: LruCache<String, String> by lazy {
        initMemCache()
    }


    @JvmStatic
    val acache: ACache  by lazy {
        ACache.get(context)
    }

    /*============================cache====================================*/
    fun cacheLruAndDisk(pair: Pair<String, Any>, seconds: Int? = null) = with(GSON.toJson(pair.second)) {
        lru.put(pair.first, this)
        seconds?.let { acache.put(pair.first, GSON.toJson(pair.second), seconds) }
            ?: acache.put(pair.first, GSON.toJson(pair.second))
    }

    fun cacheLru(pair: Pair<String, Any>) = lru.put(pair.first, GSON.toJson(pair.second))
    fun cacheDisk(pair: Pair<String, Any>, seconds: Int? = null) =
        seconds?.let { acache.put(pair.first, v2Str(pair.second), seconds) }
            ?: acache.put(pair.first, v2Str(pair.second))

    private fun v2Str(obj: Any): String = when (obj) {
        is CharSequence -> obj.toString()
        else -> obj.toJsonString()
    }

    /*============================cache to flowable====================================*/
    fun fromLruAsync(key: String): Flowable<String> =
        Flowable.interval(0, 800, TimeUnit.MILLISECONDS, Schedulers.io()).flatMap {
            val v = lru[key]
            v?.let { Flowable.just(it) } ?: Flowable.empty()
        }.timeout(6, TimeUnit.SECONDS, Flowable.empty()).take(1).subscribeOn(Schedulers.io())

    fun fromDiskAsync(key: String, add2Lru: Boolean = true): Flowable<String> =
        Flowable.interval(0, 800, TimeUnit.MILLISECONDS, Schedulers.io()).flatMap {
            val v = acache.getAsString(key)
            v?.let { Flowable.just(it) } ?: Flowable.empty()
        }.timeout(6, TimeUnit.SECONDS, Flowable.empty()).take(1).doOnNext { if (add2Lru) lru.put(key, it) }.subscribeOn(
            Schedulers.io()
        )


    fun justLru(key: String): Flowable<String> {
        val v = lru[key]
        return v?.let { Flowable.just(v) } ?: Flowable.empty()
    }

    fun justDisk(key: String, add2Lru: Boolean = true): Flowable<String> {
        val v = acache.getAsString(key)
        return v?.let { Flowable.just(v).doOnError { if (add2Lru) lru.put(key, v) } }
            ?: Flowable.empty()
    }

    /*===============================remove cache=====================================*/
    /**
     * 只会先从lru中删除再删除disk的
     */
    fun removeCacheLike(vararg keys: String, isRegex: Boolean = false) {
        Schedulers.computation().createWorker().schedule {
            lru.snapshot().keys.let { cacheCopyKeys ->
                keys.forEach { removeKey ->
                    val filterAction: (String) -> Boolean =
                        { s -> if (isRegex) s.contains(removeKey.toRegex()) else s.contains(removeKey) }
                    cacheCopyKeys.filter(filterAction).forEach {
                        KLog.i("removeCacheLike : $it")
                        cacheCopyKeys.remove(it); lru.remove(it);acache.remove(it)
                    }
                }
            }
        }
    }

}
