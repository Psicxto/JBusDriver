/**
 * ==================================================================================================
 * <p>
 * 设计目的 (Why)
 * <p>
 * AppBaseRecycleFragment 是一个为包含 RecyclerView 的 Fragment 设计的通用基类，旨在统一处理列表页面的通用逻辑，
 * 例如下拉刷新、上拉加载更多、空状态视图、加载中状态以及错误状态。它继承自 AppBaseFragment，因此也具备了 MVP 架构
 * 的特性，能够与 Presenter 进行交互，实现数据加载与视图更新的分离。
 * <p>
 * ==================================================================================================
 * <p>
 * 功效作用 (What)
 * <p>
 * 1.  **集成下拉刷新**：内置 SwipeRefreshLayout，提供下拉刷新功能，并将其与 Presenter 的 onRefresh 事件绑定。
 * 2.  **集成上拉加载**：使用 com.chad.library.adapter.base.BaseQuickAdapter，实现了上拉加载更多的功能，并与 Presenter 的 onLoadMore 事件绑定。
 * 3.  **统一的加载状态管理**：封装了 showLoading、dismissLoading 方法，通过 SwipeRefreshLayout 的刷新状态来展示加载动画。
 * 4.  **列表数据自动更新**：showContents 方法可以将从 Presenter 获取的数据自动添加到 Adapter 中。
 * 5.  **加载更多状态处理**：封装了 loadMoreComplete、loadMoreEnd、loadMoreFail 等方法，用于更新加载更多的 UI 状态。
 * 6.  **空视图与错误视图**：在列表无数据或加载失败时，能够自动展示相应的提示视图（如“没有数据”或“加载失败”），并提供点击重试功能。
 * 7.  **动画效果**：为列表项提供了默认的进入动画（透明度与位移动画）。
 * 8.  **可配置性**：通过抽象属性和方法，子类可以方便地提供自己的 RecyclerView、LayoutManager 和 Adapter，实现了高度的可定制性。
 * <p>
 * ==================================================================================================
 * <p>
 * 核心技术 (How)
 * <p>
 * 1.  **泛型约束**：通过泛型 <P : BasePresenter.BaseRefreshLoadMorePresenter<V>, V : BaseView.BaseListWithRefreshView, M>，
 *     将 Presenter、View 和数据模型进行类型绑定，确保了 MVP 各层之间的类型安全。
 * 2.  **抽象成员**：通过抽象属性 swipeView、recycleView、layoutManager 和 adapter，强制子类提供必要的 UI 组件和数据适配器，
 *     使得基类能够操作这些由子类定义的具体实现。
 * 3.  **BaseQuickAdapter 集成**：利用了第三方库 BaseQuickAdapter 的强大功能，简化了 RecyclerView 的 Adapter 开发，
 *     特别是其对加载更多的内置支持。
 * 4.  **SwipeRefreshLayout 集成**：封装了 SwipeRefreshLayout 的初始化和事件监听，简化了下拉刷新的实现。
 * 5.  **状态机思想**：通过 EmptyState 密封类（sealed class），定义了列表的几种空状态（如 NoData、ErrorEmpty），
 *     并根据不同的状态提供不同的视图，这是一种简单的状态机实现。
 * 6.  **接口回调**：通过实现 BaseView.BaseListWithRefreshView 接口，定义了一系列 View 层需要响应的方法，
 *     这些方法由 Presenter 在数据处理完成后调用，从而更新 UI。
 * <p>
 * ==================================================================================================
 */
package me.jbusdriver.base.common

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.loadmore.SimpleLoadMoreView
import io.reactivex.Flowable
import me.jbusdriver.base.R
import me.jbusdriver.base.dpToPx
import me.jbusdriver.base.mvp.BaseView
import me.jbusdriver.base.mvp.presenter.BasePresenter

/**
 * App中所有包含RecycleView的fragment基类,带有下拉刷新,上拉加载更多.
 */
abstract class AppBaseRecycleFragment<P : BasePresenter.BaseRefreshLoadMorePresenter<V>, V : BaseView.BaseListWithRefreshView, M> :
    AppBaseFragment<P, V>(), BaseView.BaseListWithRefreshView {


    /**
     * view 销毁后获取时要从view中重新获取; ex : 切换横竖屏
     * 重复使用fragment是不推荐lazy方式初始化.可能到时view引用的对象还是老对象.
     */
    abstract val swipeView: SwipeRefreshLayout? //下拉刷新视图
    abstract val recycleView: RecyclerView //列表视图
    abstract val layoutManager: RecyclerView.LayoutManager //列表布局管理器
    abstract val adapter: BaseQuickAdapter<M, in BaseViewHolder> //列表适配器

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun initWidget(rootView: View) {
        recycleView.layoutManager = layoutManager

        adapter.openLoadAnimation {
            arrayOf(
                ObjectAnimator.ofFloat(it, "alpha", 0.0f, 1.0f),
                ObjectAnimator.ofFloat(it, "translationY", 120f, 0f)
            )
        }
        swipeView?.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryDark, R.color.colorPrimaryLight)
        swipeView?.setOnRefreshListener { mBasePresenter?.onRefresh() }
        adapter.bindToRecyclerView(recycleView)
        adapter.setOnLoadMoreListener({ mBasePresenter?.onLoadMore() }, recycleView)
        adapter.setLoadMoreView(SimpleLoadMoreView())


    }

    override fun showLoading() {
        swipeView?.let {
            if (!it.isRefreshing) {
                it.post {
                    it.setProgressViewOffset(false, 0, viewContext.dpToPx(24f))
                    it.isRefreshing = true
                }
            }
        } ?: super.showLoading()

        adapter.removeAllFooterView()
    }

    override fun dismissLoading() {
        swipeView?.let {
            it.post { it.isRefreshing = false }
        } ?: super.dismissLoading()
    }

    override fun showContents(data: List<*>) {
        (data as? MutableList<M> ?: data.toMutableList() as? MutableList<M>)?.let {
            adapter.addData(it)
        }
    }

    override fun loadMoreComplete() {
        adapter.loadMoreComplete()
    }

    override fun loadMoreEnd(clickable: Boolean) {
        if (adapter.getEmptyView() == null && adapter.getData().isEmpty()) {
            adapter.setEmptyView(EmptyState.NoData(viewContext).getEmptyView())
        }
        adapter.loadMoreEnd()
        adapter.enableLoadMoreEndClick(clickable)
    }


    override fun loadMoreFail() {
        if (adapter.getEmptyViewCount() <= 0 && adapter.getData().isEmpty()) {
            adapter.setEmptyView(EmptyState.ErrorEmpty(viewContext).getEmptyView().apply {
                setOnClickListener { mBasePresenter?.onRefresh() }
            })
        }
        adapter.loadMoreFail()

    }

    override fun enableRefresh(bool: Boolean) {
        swipeView?.isEnabled = bool
    }

    override fun enableLoadMore(bool: Boolean) {
        adapter.setEnableLoadMore(bool)
    }

    override fun getRequestParams(page: Int): Flowable<String> = Flowable.empty()


    override fun resetList() {
        adapter.setNewData(null)
    }

    override fun showError(e: Throwable?) {
        adapter.loadMoreFail()
    }

    sealed class EmptyState(val tip: String) {
        class NoData(val context: Context) : EmptyState("没有数据") {
            override fun getEmptyView(): View {
                return TextView(context).apply {
                    text = tip
                    layoutParams =
                            ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT, context.dpToPx(36f))
                                .apply {
                                    gravity = Gravity.CENTER
                                }
                }

            }
        }

        class ErrorEmpty(val context: Context) : EmptyState("加载失败") {
            override fun getEmptyView(): View {
                return TextView(context).apply {
                    text = tip
                    layoutParams =
                            ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT, context.dpToPx(36f))
                                .apply {
                                    gravity = Gravity.CENTER
                                }
                }

            }
        }

        abstract fun getEmptyView(): View
    }
}


