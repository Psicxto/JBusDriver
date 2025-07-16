package me.jbusdriver.base.mvp.model

import io.reactivex.Flowable
import me.jbusdriver.base.addUserCase

/**
 * @author jbusdriver
 * MVP `Model` 层的抽象基类，实现了 `BaseModel` 接口。
 * 它旨在简化 `Model` 的创建，特别是对于那些主要关注单一数据请求操作（`requestFor`）而缓存逻辑相对简单的场景。
 * @param P 请求参数的类型。
 * @param R 返回数据的类型。
 * @param op 一个高阶函数，封装了实际的数据请求逻辑。它接受参数P并返回一个`Flowable<R>`。
 */
abstract class AbstractBaseModel<in P, R>(private val op: (P) -> Flowable<R>) : BaseModel<P, R> {

    /**
     * 实现了 `BaseModel` 的 `requestFor` 方法。
     * 它直接调用构造函数中传入的 `op` 函数来执行数据请求，并通过 `addUserCase()` 扩展函数（推测是用例调度或线程管理）来处理返回的 `Flowable`。
     * @param t 请求参数。
     * @return 返回经过处理的 `Flowable<R>` 数据流。
     */
    override fun requestFor(t: P): Flowable<R> = op.invoke(t).addUserCase()

    //注意：这个抽象类没有实现 `requestFromCache` 方法，因此任何继承自它的具体 `Model` 类仍然需要自己实现缓存逻辑。
}