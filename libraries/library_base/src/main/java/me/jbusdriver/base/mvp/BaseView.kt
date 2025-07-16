package me.jbusdriver.base.mvp

import android.content.Context
import io.reactivex.Flowable

/**
 * MVP架构中View层的顶层接口，定义了所有View层组件（如Activity, Fragment）需要具备的基本行为。
 * @author Administrator
 * @date 2016/7/21
 */
interface BaseView {

    /**
     * 提供一个上下文环境，通常是Activity或Fragment的Context。
     * Presenter可以通过这个属性来访问Android系统的资源和服务，而无需直接持有Activity的引用，有助于解耦。
     */
    val viewContext: Context

    /**
     * 显示一个加载指示器，例如一个ProgressBar。
     * 当Presenter正在执行耗时操作（如网络请求）时调用，向用户表明应用正在处理中。
     */
    fun showLoading()

    /**
     * 隐藏加载指示器。
     * 当耗时操作完成或失败后调用。
     */
    fun dismissLoading()

    /**
     * 将处理完成的数据显示到UI上。
     * @param data 泛型数据，可以是任何类型，由Presenter处理后传递过来。
     */
    fun <T> showContent(data: T?): Unit = TODO(" no impl")

    /**
     * 在UI上显示一个错误信息。
     * @param e 发生的异常，View层可以根据具体异常类型显示不同的错误提示。
     */
    fun showError(e: Throwable?): Unit = TODO(" no impl")


    /**
     * 针对包含列表的View的扩展接口，在`BaseView`的基础上增加了列表操作相关的行为。
     */
    interface BaseListView : BaseView {
        /**
         * 将一组列表数据显示到UI上，通常用于首次加载或刷新。
         * @param data 列表数据，通常是List<*>
         */
        fun showContents(data: List<*>)

        /**
         * 通知UI“加载更多”操作已成功完成。
         * RecyclerView的Adapter会据此更新状态。
         */
        fun loadMoreComplete()

        /**
         * 通知UI所有数据已加载完毕，没有更多数据了。
         * @param clickable 是否可以在“没有更多”的提示上进行点击操作。
         */
        fun loadMoreEnd(clickable: Boolean = false)

        /**
         * 通知UI“加载更多”操作失败。
         */
        fun loadMoreFail()

        /**
         * 控制下拉刷新功能是否可用。
         * @param bool true为可用，false为不可用。
         */
        fun enableRefresh(bool: Boolean)


    }

    /**
     * 针对需要刷新和加载更多功能的列表视图的进一步扩展接口。
     */
    interface BaseListWithRefreshView : BaseListView {

        /**
         * View层需要实现此方法，以提供构建网络请求所需的参数。
         * Presenter在需要请求数据时会调用此方法。
         * @param page 请求的页码。
         * @return 返回一个包含请求参数的RxJava Flowable<String>，但实际上这里的设计可能有些问题，
         *         通常View不应该负责构建请求，这里返回String可能是URL的一部分。
         *         更好的设计是返回一个包含所有参数的Map或数据类。
         */
        fun getRequestParams(page: Int): Flowable<String>


        /**
         * 重置列表状态，通常用于执行新的搜索或筛选操作，清空现有数据和页码。
         */
        fun resetList()

        /**
         * 控制上拉加载更多功能是否可用。
         * @param bool true为可用，false为不可用。
         */
        fun enableLoadMore(bool: Boolean)

    }


}
