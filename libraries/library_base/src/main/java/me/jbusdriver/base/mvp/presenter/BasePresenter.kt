package me.jbusdriver.base.mvp.presenter

/**
 * MVP架构中Presenter层的顶层接口，定义了所有Presenter需要遵循的生命周期和基本行为。
 * Presenter负责处理业务逻辑，并作为View和Model之间的桥梁。
 * @param V 泛型参数，限定了此Presenter能绑定的View的类型，必须是BaseView的子类。
 * @author Administrator
 * @date 2016/7/21
 */
interface BasePresenter<in V> {


    /**
     * 当View被附加到Presenter时调用。
     * 这个方法通常由基类实现，用于建立Presenter和View之间的连接。
     * 在Activity或Fragment的onStart之后调用。
     * @param view 附加的View实例。
     */
    fun onViewAttached(view: V)

    /**
     * 每次View启动时调用，此时可以保证View不为空。
     * 在onViewAttached之后调用。
     * @param firstStart 如果是Presenter生命周期内的第一次启动，则为true。
     */
    fun onStart(firstStart: Boolean)

    /**
     * 仅在Presenter第一次启动时调用一次，用于执行初始化的数据加载。
     * 不应手动调用此方法。
     */
    fun onFirstLoad()

    /**
     * 当View进入Resumed状态时调用。
     */
    fun onResume()

    /**
     * 当View进入Paused状态时调用。
     */
    fun onPause()


    /**
     * 每次View停止时调用。在此方法之后，View的引用将被置空，直到下一次onStart。
     */
    fun onStop()

    /**
     * 当View从Presenter上分离时调用。
     * 这个方法通常由基类实现，用于断开Presenter和View之间的连接，防止内存泄漏。
     */
    fun onViewDetached()

    /**
     * 当Presenter被最终销毁时调用。
     * 应在此方法中释放所有资源，例如取消网络请求、关闭数据库连接等。
     */
    fun onPresenterDestroyed()


    /**
     * 用于在发生配置更改（如屏幕旋转）后恢复Presenter的状态。
     */
    fun restoreFromState()


    //region 封装的接口


    /**
     * 为具备“加载更多”功能的Presenter定义的接口。
     */
    interface LoadMorePresenter {
        /**
         * 根据页码加载数据。
         * @param page 页码，通常从1开始递增。
         */
        fun loadData4Page(page: Int)

        /**
         * 触发“加载更多”的逻辑。
         */
        fun onLoadMore()

        /**
         * 判断是否还有下一页数据可供加载。
         * @return 如果有下一页，返回true；否则返回false。
         */
        fun hasLoadNext(): Boolean

    }

    /**
     * 为具备“下拉刷新”功能的Presenter定义的接口。
     */
    interface RefreshPresenter {
        /**
         * 当用户执行下拉刷新操作时调用。
         */
        fun onRefresh()
    }


    /**
     * 一个聚合接口，整合了基础Presenter、加载更多和下拉刷新功能。
     * 用于实现通用的列表类型Presenter。
     * @param V View的类型参数。
     */
    interface BaseRefreshLoadMorePresenter<V> : BasePresenter<V>, LoadMorePresenter, RefreshPresenter

    /**
     * 为在ViewPager中的Fragment实现懒加载功能而设计的接口。
     */
    interface LazyLoaderPresenter {
        /**
         * 当Fragment首次对用户可见时，调用此方法来加载数据。
         */
        fun lazyLoad()
    }


}
