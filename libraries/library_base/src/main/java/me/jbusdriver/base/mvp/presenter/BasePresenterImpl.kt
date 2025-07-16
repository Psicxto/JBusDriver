package me.jbusdriver.base.mvp.presenter

import io.reactivex.disposables.CompositeDisposable
import me.jbusdriver.base.KLog
import me.jbusdriver.base.mvp.BaseView
import kotlin.properties.Delegates

/**
 * `BasePresenter` 接口的基础实现类，提供了Presenter生命周期方法的默认实现和一些通用功能。
 * 具体的业务Presenter可以继承此类，从而只需关注自身的业务逻辑实现，而无需重复编写生命周期管理代码。
 * @param V 泛型参数，限定了此Presenter能绑定的View的类型，必须是`BaseView`的子类。
 * @author Administrator
 * @date 2016/11/25
 */
open class BasePresenterImpl<V : BaseView> : BasePresenter<V> {

    /**
     * 对View的弱引用，防止内存泄漏。使用`@JvmField`注解是为了在Java代码中可以直接访问字段。
     * 可空类型，因为View的生命周期与Presenter并不同步。
     */
    @JvmField
    protected var mView: V? = null

    /**
     * 标记是否是Presenter的第一次启动。使用`Delegates.notNull()`确保在使用前必须被初始化。
     */
    private var isFirstStart: Boolean by Delegates.notNull()

    /**
     * 用于管理RxJava的订阅（Subscription/Disposable），方便在Presenter销毁时统一取消，避免内存泄漏。
     * 使用`lazy`委托实现懒加载。
     */
    protected val rxManager by lazy { CompositeDisposable() }

    /**
     * 日志标签，使用类名作为TAG，方便调试。
     */
    private val TAG: String by lazy { this.javaClass.simpleName }


    /**
     * 绑定View到Presenter。
     */
    override fun onViewAttached(view: V) {
        KLog.t(TAG).i("$this:onViewAttached $view")
        mView = view
        assert(mView != null) //断言确保view不为空
    }


    /**
     * 在View启动时调用。如果是首次启动且当前Presenter不是懒加载类型，则直接调用`onFirstLoad()`加载初始数据。
     * 懒加载的逻辑将由`LazyLoaderPresenter`的实现者自己控制。
     */
    override fun onStart(firstStart: Boolean) {
        isFirstStart = firstStart
        if (firstStart && this !is BasePresenter.LazyLoaderPresenter) {
            //如果是LazyLoaderPresenter, 交给LazyLoaderPresenter的实现者自己处理何时调用onFirstLoad
            onFirstLoad()
            return
        }
    }


    /**
     * 首次加载数据的方法，子类可以重写此方法以实现自己的初始化逻辑。
     */
    override fun onFirstLoad() {
        //默认空实现
    }

    override fun onResume() {
        //默认空实现
    }

    override fun onPause() {
        //默认空实现
    }

    override fun onStop() {
        //默认空实现
    }


    /**
     * 解绑View。在此处会隐藏加载框、清空所有RxJava订阅并置空View的引用。
     */
    override fun onViewDetached() {
        mView?.dismissLoading()
        rxManager.clear() //取消所有订阅，但容器不清空，可继续使用
        mView = null

    }

    /**
     * Presenter销毁时调用。彻底清空并处理掉RxJava的订阅管理器。
     */
    override fun onPresenterDestroyed() {
        KLog.t(TAG).i("$this:onPresenterDestroyed")
        rxManager.clear()
        rxManager.dispose() //容器被处理，无法再使用
    }

    /**
     * 状态恢复，默认空实现。
     */
    override fun restoreFromState() {
        //no op
    }
}