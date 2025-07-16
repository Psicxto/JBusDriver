package me.jbusdriver.base.mvp.model

import io.reactivex.Flowable

/**
 * @author jbusdriver
 * @date 2017/4/8
 * MVP架构中的Model层基础接口。
 * 定义了数据获取的通用契约，主要用于处理网络请求或本地数据加载。
 * @param T 请求参数的类型，使用`in`关键字表示逆变，意味着可以接受T及其父类型的参数。
 * @param R 返回数据的类型。
 */
interface BaseModel<in T, R> {

    /**
     * 发起网络请求或执行主要的数据获取操作。
     * @param t 请求所需的参数。
     * @return 返回一个包含结果数据R的Flowable流。默认实现返回一个空的Flowable，避免子类强制实现。
     */
    fun requestFor(t: T): Flowable<R> = Flowable.empty()

    /**
     * 从缓存中获取数据。
     * @param t 请求所需的参数。
     * @return 返回一个包含缓存数据R的Flowable流。这是一个抽象方法，需要子类具体实现。
     */
    fun requestFromCache(t: T): Flowable<R>

}