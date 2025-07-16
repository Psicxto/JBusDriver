package me.jbusdriver.base.common

import android.annotation.TargetApi
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import com.afollestad.materialdialogs.MaterialDialog
import me.jbusdriver.base.KLog
import me.jbusdriver.base.inflate
import me.jbusdriver.base.mvp.BaseView
import me.jbusdriver.base.mvp.presenter.BasePresenter
import me.jbusdriver.base.mvp.presenter.loader.PresenterFactory
import me.jbusdriver.base.mvp.presenter.loader.PresenterLoader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author jbusdriver
 * @date 2016/7/21
 * 集成了MVP架构的Activity基类，通过`Loader`机制来管理`Presenter`的生命周期。
 * 这使得`Presenter`可以在Activity因配置变更（如屏幕旋转）重建时存活下来，从而保留其内部状态。
 * @param P Presenter的类型，必须是`BasePresenter`的子类。
 * @param V View的类型，必须是`BaseView`的子类，Activity自身将作为View的实现。
 */
abstract class AppBaseActivity<P : BasePresenter<V>, in V : BaseView> : BaseActivity(),
    LoaderManager.LoaderCallbacks<P>, PresenterFactory<P>, BaseView {

    //region Why: 设计目的
    // `AppBaseActivity`的核心设计目标是解决MVP架构在Android中的一个经典痛点：Presenter的生命周期管理，
    // 特别是在Activity因配置变更（如屏幕旋转）而销毁重建时，如何优雅地保留Presenter实例及其内部状态。
    // 它通过利用Android框架提供的`Loader`机制，将Presenter的生命周期与Activity的生命周期解耦，
    // 使得Presenter能够“存活”于Activity的重建过程中，从而避免了数据的重新加载和状态的丢失，提升了用户体验和应用性能。
    //endregion

    //region What: 功效作用
    // 1. **Presenter的持久化**: 在Activity重建时，`LoaderManager`会保留`PresenterLoader`实例，从而复用已经创建的`Presenter`对象，而不是重新创建一个新的。这保证了Presenter内部的数据和状态得以保留。
    // 2. **自动化的生命周期绑定**: 封装了`Presenter`与`View`（即Activity）的attach和detach逻辑。在`onStart`时附加View，在`onDestroy`时分离View，开发者无需手动管理。
    // 3. **模板化的MVP实现**: 继承自`AppBaseActivity`的子类，只需要实现`layoutId`（提供布局）和`createPresenter`（提供Presenter实例）两个核心方法，即可快速构建一个功能完备的MVP页面。
    // 4. **状态恢复**: 提供了`onSaveInstanceState`和`restoreState`的框架，虽然在此基类中`restoreState`是空实现，但为子类Presenter提供了恢复状态的入口。
    // 5. **加载对话框**: 实现了`BaseView`的`showLoading`/`dismissLoading`接口，提供了一个全局的、统一的加载中对话框，简化了UI交互。
    // 6. **唯一的Loader ID**: 通过一个静态的`AtomicInteger`为每个Activity实例生成唯一的Loader ID，确保了`LoaderManager`能够正确地管理每个Activity对应的`Presenter`。
    //endregion

    //region How: 核心技术
    // 1. **Loader机制**: 这是实现Presenter持久化的核心。`Loader`是Android提供的一个用于在后台异步加载数据并能感知Activity/Fragment生命周期的组件。当Activity重建时，如果已存在具有相同ID的Loader，系统会直接复用它，而不是重新创建。`PresenterLoader`就是利用这个特性来持有和管理Presenter实例。
    // 2. **LoaderManager.LoaderCallbacks**: Activity通过实现这个接口来与Loader进行交互。`onCreateLoader`负责创建`PresenterLoader`，`onLoadFinished`在Loader加载完成（即Presenter创建或复用完成）时被回调，`onLoaderReset`在Loader被销毁时回调。
    // 3. **PresenterFactory模式**: 定义了一个`createPresenter()`接口方法，将Presenter的实例化过程委托给子类。`PresenterLoader`在需要创建Presenter时，会调用这个工厂方法。这遵循了依赖倒置原则。
    // 4. **泛型编程**: 使用泛型`<P : BasePresenter<V>, in V : BaseView>`来约束Presenter和View的类型，实现了类型安全，使得在编译期就能发现类型不匹配的错误。
    // 5. **原子操作类 (Atomic Classes)**: 使用`AtomicBoolean`和`AtomicInteger`来处理并发和状态同步问题。`mNeedToCallStart`解决了`onStart`和`onLoadFinished`的异步执行问题；`sViewCounter`保证了多线程环境下Loader ID的唯一性。
    // 6. **生命周期协调**: 通过`mFirstStart`和`mNeedToCallStart`等标志位，精细地协调了Activity和Presenter复杂的生命周期事件，确保在正确的时机调用`onViewAttached`和`onStart`等方法。
    //endregion

    // 标志位，用于处理Presenter加载完成与Activity.onStart()的异步问题
    private val mNeedToCallStart = AtomicBoolean(false)
    // 标志位，判断是否是Activity首次创建（非重建）
    private var mFirstStart: Boolean = false
    // 持有的Presenter实例
    protected var mBasePresenter: P? = null
    // 每个Activity实例的Loader的唯一标识符，用于在重建后能找到同一个Loader
    private var mUniqueLoaderIdentifier: Int = 0

    // 加载中对话框
    private var placeDialogHolder: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. 判断是否是首次启动。如果是，mFirstStart为true。
        mFirstStart = savedInstanceState == null || savedInstanceState.getBoolean(C.SavedInstanceState.RECREATION_SAVED_STATE)
        // 2. 获取或创建一个唯一的Loader ID。如果是重建，从savedInstanceState恢复；否则，通过原子计数器创建一个新的。
        mUniqueLoaderIdentifier = savedInstanceState?.getInt(C.SavedInstanceState.LOADER_ID_SAVED_STATE) ?: sViewCounter.incrementAndGet()

        setContentView(this.inflate(layoutId))

        // 3. 初始化LoaderManager。这将触发LoaderCallbacks的回调，最终创建或复用Presenter。
        supportLoaderManager.initLoader(mUniqueLoaderIdentifier, savedInstanceState, this@AppBaseActivity)

        // 将状态数据暂存到intent中，以便后续恢复 (此做法略显不寻常，通常直接在Loader中处理)
        if (savedInstanceState != null) {
            intent.putExtra(C.SavedInstanceState.LOADER_SAVED_STATES + mUniqueLoaderIdentifier, savedInstanceState)
        }
    }

    override fun onStart() {
        super.onStart()
        // onStart可能会在Presenter加载完成（onLoadFinished）之前调用。
        // 如果此时Presenter还未就绪，则设置标志位，等待onLoadFinished回调时再执行doStart。
        if (mBasePresenter == null) {
            mNeedToCallStart.set(true)
        } else {
            doStart()
        }
    }

    /**
     * 核心方法，用于将View附加到Presenter，并触发生命周期回调。
     */
    protected open fun doStart() {
        KLog.t(TAG).d("$this doStart isFirst: $mFirstStart, id: $mUniqueLoaderIdentifier")
        requireNotNull(mBasePresenter)
        // 1. 将当前Activity(View)附加到Presenter
        mBasePresenter?.onViewAttached(this as V)
        // 2. 通知Presenter，它的View已经启动。传递mFirstStart告知是否是首次加载。
        mBasePresenter?.onStart(mFirstStart)
        // 3. 尝试恢复Presenter的状态
        val bundleKey = C.SavedInstanceState.LOADER_SAVED_STATES + mUniqueLoaderIdentifier
        intent.getBundleExtra(bundleKey)?.let {
            restoreState(it)
            intent.removeExtra(bundleKey)
        }
        mFirstStart = false // 后续的onStart不再是首次启动
    }

    //region Presenter Lifecycle
    // 将Activity的生命周期事件传递给Presenter
    override fun onResume() {
        super.onResume()
        mBasePresenter?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mBasePresenter?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mBasePresenter?.onStop()
    }

    override fun onDestroy() {
        // 在Activity销毁前，从Presenter分离View，防止内存泄漏
        mBasePresenter?.onViewDetached()
        super.onDestroy()
    }
    //endregion

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        // 保存关键状态（是否首次启动、LoaderID），以便Activity重建后恢复
        outState?.putBoolean(C.SavedInstanceState.RECREATION_SAVED_STATE, mFirstStart)
        outState?.putInt(C.SavedInstanceState.LOADER_ID_SAVED_STATE, mUniqueLoaderIdentifier)
    }

    /**
     * 子类必须提供布局ID。
     */
    protected abstract val layoutId: Int

    /**
     * LoaderCallbacks回调：创建Loader实例。
     * @return 返回一个PresenterLoader，它会使用我们提供的PresenterFactory（即Activity自身）来创建Presenter。
     */
    override fun onCreateLoader(id: Int, args: Bundle?) = PresenterLoader(this, this)

    /**
     * LoaderCallbacks回调：当Loader加载完成时调用。
     * @param data Presenter实例，由PresenterLoader创建或复用。
     */
    override fun onLoadFinished(loader: Loader<P>, data: P) {
        mBasePresenter = data
        // 检查是否需要在此时执行doStart（对应onStart提前调用的情况）
        if (mNeedToCallStart.compareAndSet(true, false)) {
            doStart()
        }
    }

    /**
     * LoaderCallbacks回调：当Loader被重置时调用（通常在Activity最终销毁时）。
     */
    override fun onLoaderReset(loader: Loader<P>) {
        mBasePresenter = null
    }

    //endregion

    //region BaseView implementation
    // 实现BaseView接口，提供加载对话框的显示和隐藏
    override fun showLoading() {
        runOnUiThread {
            if (viewContext is Application || isDestroyedCompatible) return@runOnUiThread
            placeDialogHolder = MaterialDialog.Builder(viewContext).content("正在加载...").progress(true, 0).show()
        }
    }

    override fun dismissLoading() {
        runOnUiThread {
            placeDialogHolder?.dismiss()
            placeDialogHolder = null
        }
    }
    //endregion

    protected open fun restoreState(bundle: Bundle) {
        // 为子类提供恢复状态的扩展点
        mBasePresenter?.restoreFromState()
    }

    companion object {
        // 全局静态计数器，为每个AppBaseActivity实例生成唯一的Loader ID，确保不会冲突。
        val sViewCounter = AtomicInteger(Integer.MIN_VALUE)
    }
}
