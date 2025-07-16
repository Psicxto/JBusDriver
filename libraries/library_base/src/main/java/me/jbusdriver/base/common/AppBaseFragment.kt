package me.jbusdriver.base.common

import android.app.Application
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.rxkotlin.addTo
import me.jbusdriver.base.KLog
import me.jbusdriver.base.mvp.BaseView
import me.jbusdriver.base.mvp.presenter.BasePresenter
import me.jbusdriver.base.mvp.presenter.loader.PresenterFactory
import me.jbusdriver.base.mvp.presenter.loader.PresenterLoader
import me.jbusdriver.base.postMain
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author jbusdriver
 * @date 2016/7/21
 * @description 集成MVP架构和懒加载机制的Fragment基类。
 */
abstract class AppBaseFragment<P : BasePresenter<V>, V> : BaseFragment(), LoaderManager.LoaderCallbacks<P>,
    PresenterFactory<P>, BaseView {

    //region Why: 设计目的
    // `AppBaseFragment` 继承自 `AppBaseActivity` 的MVP思想，旨在为Fragment提供一个统一的、功能增强的基类。
    // 它的核心设计目标有两个：
    // 1. **在Fragment中实现与Activity一致的、可跨越生命周期重建的MVP模式**：利用`Loader`机制管理`Presenter`，确保在Fragment重建时（例如，因内存回收或配置变更）`Presenter`及其状态得以保留。
    // 2. **实现高效的懒加载（Lazy Load）机制**：特别是在与`ViewPager`结合使用时，确保只有当Fragment对用户可见时才加载其数据，避免不必要的网络请求和计算，优化应用性能和用户体验。
    //endregion

    //region What: 功效作用
    // 1. **持久化的Presenter**: 同`AppBaseActivity`，通过`Loader`机制确保`Presenter`在Fragment重建后能够存活。
    // 2. **视图缓存**: 通过`WeakReference<View>`缓存Fragment的根视图（`rootView`）。当Fragment的视图被销毁（`onDestroyView`）但Fragment实例未被销毁时（例如在`ViewPager`中），下次创建视图时可以直接复用，避免了布局的重新解析和`initWidget`的重复调用，提高了UI渲染效率。
    // 3. **懒加载**: 实现了`lazyLoad`逻辑，结合`setUserVisibleHint`和`onHiddenChanged`，精确控制数据加载的时机。只有当Fragment首次对用户可见时，才会调用`lazyLoad`方法，触发数据加载。
    // 4. **统一的生命周期管理**: 将Fragment的生命周期事件（`onStart`, `onResume`, `onPause`, `onStop`, `onDestroyView`）转发给`Presenter`，实现了`View`和`Presenter`生命周期的同步。
    // 5. **模板方法模式**: 定义了`layoutId`、`initWidget`、`initData`、`lazyLoad`等抽象或空方法，为子类提供清晰的实现入口，规范了Fragment的开发流程。
    // 6. **加载对话框**: 实现了`BaseView`的`showLoading`/`dismissLoading`接口，提供加载提示。
    //endregion

    //region How: 核心技术
    // 1. **Loader机制**: 与`AppBaseActivity`相同，使用`LoaderManager`和自定义的`PresenterLoader`来管理`Presenter`的生命周期。
    // 2. **视图的弱引用缓存 (WeakReference)**: 使用`rootViewWeakRef`来持有对根视图的弱引用。这既能缓存视图以提高性能，又能避免因持有视图而导致的内存泄漏（如果Fragment实例存活而其视图本该被回收）。
    // 3. **Fragment可见性判断**: 综合利用`setUserVisibleHint(Boolean)`和`onHiddenChanged(Boolean)`来判断Fragment是否对用户可见。`setUserVisibleHint`主要用于`ViewPager`，而`onHiddenChanged`用于`FragmentTransaction`的`show/hide`操作，覆盖了Fragment可见性变化的两种主要场景。
    // 4. **状态标志位**: 使用`mFirstStart`、`mViewReCreate`、`isLazyLoaded`等多个布尔标志位，精确地控制`initWidget`、`initData`和`lazyLoad`的调用时机，确保它们只在需要时执行一次。
    // 5. **原子布尔 (AtomicBoolean)**: `mNeedToCallStart`用于解决`onStart`和`onLoadFinished`的异步调用问题，确保`doStart`在`Presenter`准备就绪后执行，保证了线程安全。
    // 6. **生命周期回调的精心编排**: 在`onCreateView`, `onViewCreated`, `onActivityCreated`, `onStart`等生命周期方法中，通过对各种状态的判断，实现了视图创建、`Presenter`绑定和数据加载的有序进行。
    //endregion

    // 标志位，用于处理Presenter加载完成与Fragment.onStart()的异步问题
    private val mNeedToCallStart = AtomicBoolean(false)
    // 标志位，判断是否是Fragment首次创建（非重建）
    private var mFirstStart: Boolean = false
    // 标志位，判断根视图是否是重新创建的
    private var mViewReCreate = false
    // 持有的Presenter实例
    protected var mBasePresenter: P? = null
    // 根视图的弱引用，用于缓存视图
    private var rootViewWeakRef: WeakReference<View>? = null
    // 每个Fragment实例的Loader的唯一标识符
    private var mUniqueLoaderIdentifier: Int = 0
    // 懒加载完成的标志位
    private var isLazyLoaded = false

    // 加载中对话框
    protected var placeDialogHolder: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化标志位和唯一的Loader ID
        mFirstStart = savedInstanceState == null ||
                savedInstanceState.getBoolean(C.SavedInstanceState.RECREATION_SAVED_STATE)
        mUniqueLoaderIdentifier = savedInstanceState?.getInt(C.SavedInstanceState.LOADER_ID_SAVED_STATE) ?: AppBaseActivity.sViewCounter.incrementAndGet()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        require(activity != null)
        // 将状态数据暂存到Activity的intent中（此做法较为特殊）
        activity!!.intent.putExtra(
            C.SavedInstanceState.LOADER_SAVED_STATES + mUniqueLoaderIdentifier,
            savedInstanceState
        )
        // 初始化Loader，开始加载Presenter
        loaderManager.initLoader(mUniqueLoaderIdentifier, null, this).startLoading()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // 视图缓存逻辑
        rootViewWeakRef?.get()?.let {
            // 如果视图已存在，从其父视图中移除，以便复用
            ((it.parent as? View) as? ViewGroup)?.removeView(rootViewWeakRef?.get())
        } ?: run {
            // 如果视图不存在，则创建新视图
            if (!mFirstStart) mViewReCreate = true // 如果不是首次创建，说明视图是重建的
            inflater.inflate(layoutId, container, false)?.let {
                rootViewWeakRef = WeakReference(it) // 存入弱引用
            }
        }
        return rootViewWeakRef?.get()
    }

    /**
     * 子类必须提供布局ID
     */
    protected abstract val layoutId: Int

    /**
     * 子类必须实现，用于初始化控件
     */
    protected abstract fun initWidget(rootView: View)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 只有在首次创建或视图重建时才初始化控件
        if (mFirstStart || mViewReCreate) {
            initWidget(rootViewWeakRef?.get() ?: error("view is not inflated!!"))
        }
    }

    private fun doStart() {
        KLog.t(TAG).d("$this doStart isFirst: $mFirstStart, id: $mUniqueLoaderIdentifier")
        requireNotNull(mBasePresenter)
        // 1. 附加View到Presenter
        mBasePresenter?.onViewAttached(this as V)
        // 2. 通知Presenter onStart
        mBasePresenter?.onStart(mFirstStart)
        // 3. 初始化数据（仅首次或视图重建时）
        if (mFirstStart || mViewReCreate) {
            initData()
        }
        KLog.d("doStart lazyLoad $TAG $mFirstStart $isLazyLoaded $userVisibleHint")
        // 4. 触发懒加载（如果条件满足）
        if ((mFirstStart || mViewReCreate) && !isLazyLoaded && userVisibleHint) {
            lazyLoad()
        }

        // 重置标志位
        mFirstStart = false
        mViewReCreate = false
    }

    override fun onStart() {
        super.onStart()
        // 协调onStart与onLoadFinished的异步执行
        if (mBasePresenter == null) {
            mNeedToCallStart.set(true)
        } else {
            doStart()
        }
    }

    override fun onResume() {
        super.onResume()
        mBasePresenter?.onResume()
    }

    /**
     * 当使用show/hide管理Fragment时，此方法会被调用，用于更新可见性状态。
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        userVisibleHint = !hidden
    }

    /**
     * 当在ViewPager中滑动时，此方法会被调用，用于更新可见性状态并触发懒加载。
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (userVisibleHint) {
            onVisible()
        } else {
            onInvisible()
        }
    }

    /**
     * 当Fragment变为可见时调用。
     */
    protected open fun onVisible() {
        // 如果尚未懒加载且Presenter已就绪，则执行懒加载
        if (isLazyLoaded || mBasePresenter == null) {
            // mBasePresenter可能尚未初始化，懒加载逻辑会推迟到doStart中执行
        } else {
            lazyLoad()
        }
    }

    /**
     * 懒加载的核心方法。子类可以重写此方法以实现自己的数据加载逻辑。
     */
    protected open fun lazyLoad() {
        if (isLazyLoaded) {
            return
        }
        // 如果Presenter实现了LazyLoaderPresenter接口，则调用其lazyLoad方法
        if (mBasePresenter is BasePresenter.LazyLoaderPresenter) (mBasePresenter as? BasePresenter.LazyLoaderPresenter)?.lazyLoad()
        isLazyLoaded = true
    }

    /**
     * 当Fragment变为不可见时调用。
     */
    protected open fun onInvisible() {}

    /**
     * 子类可以重写此方法以初始化数据。
     */
    protected open fun initData() {}


    override fun onPause() {
        super.onPause()
        mBasePresenter?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mBasePresenter?.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 在视图销毁时，从Presenter分离View，防止内存泄漏
        mBasePresenter?.onViewDetached()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理视图的弱引用
        rootViewWeakRef?.clear()
        rootViewWeakRef = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存状态，以便重建后恢复
        outState.putBoolean(C.SavedInstanceState.RECREATION_SAVED_STATE, mFirstStart)
        outState.putInt(C.SavedInstanceState.LOADER_ID_SAVED_STATE, mUniqueLoaderIdentifier)
    }


    override fun onCreateLoader(id: Int, args: Bundle?) = PresenterLoader(viewContext, this)

    /**
     * Loader加载完成回调。
     * 注意：在某些情况下，Fragment的此回调可能会被调用两次，需要开发者注意处理幂等性。
     * @param loader Loader实例
     * @param data Presenter实例
     */
    override fun onLoadFinished(loader: Loader<P>, data: P) {
        //fragment中会赋值两次，可以设置flag。
        mBasePresenter = data
        val bundleKey = C.SavedInstanceState.LOADER_SAVED_STATES + mUniqueLoaderIdentifier
        activity!!.intent.getBundleExtra(bundleKey)?.let {
            restoreState(it)
            activity!!.intent.removeExtra(bundleKey)
        }
        if (mNeedToCallStart.compareAndSet(true, false)) {
            doStart()
        }
    }

    override fun onLoaderReset(loader: Loader<P>) {
        mBasePresenter = null
    }

    override fun showLoading() {
        postMain {
            if (viewContext is Application) return@postMain
            placeDialogHolder = MaterialDialog.Builder(viewContext).content("正在加载...").progress(true, 0).show()
        }.addTo(rxManager)

    }

    override fun dismissLoading() {
        postMain {
            placeDialogHolder?.dismiss()
            placeDialogHolder = null
        }.addTo(rxManager)
    }

    protected open fun restoreState(bundle: Bundle) {
        mBasePresenter?.restoreFromState()
    }

}
