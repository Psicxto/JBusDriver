package me.jbusdriver.base.common

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.gyf.barlibrary.ImmersionBar
import com.umeng.analytics.MobclickAgent
import io.reactivex.disposables.CompositeDisposable
import me.jbusdriver.base.KLog

/**
 * @author jbusdriver
 * @date 2016/8/11
 * App中所有Activity的基类，封装了通用功能，旨在简化子类实现和统一行为。
 * 主要功能包括：
 * 1. RxJava订阅的生命周期管理。
 * 2. 日志记录。
 * 3. 沉浸式状态栏的初始化和销毁。
 * 4. 友盟统计的生命周期集成。
 * 5. Toolbar返回按钮的统一处理。
 * 6. 兼容性的`isDestroyed`判断。
 */
abstract class BaseActivity : AppCompatActivity() {

    //region Why: 设计目的
    // 作为一个抽象基类，`BaseActivity`旨在通过模板方法模式，为项目中所有的Activity提供一个统一的、可复用的基础架构。
    // 它的核心目的是将一些通用的、与业务无关的功能（如生命周期管理、日志、UI通用行为）进行封装，
    // 从而让子类Activity可以更专注于具体的业务逻辑实现，减少样板代码，提高开发效率和代码的可维护性。
    //endregion

    //region What: 功效作用
    // 1. **RxJava订阅管理**: 通过`rxManager` (CompositeDisposable)，自动管理在Activity中创建的RxJava订阅。在`onDestroy`时自动取消所有订阅，有效防止因异步操作持有Activity引用而导致的内存泄漏。
    // 2. **日志**: 提供一个基于类名的`TAG`属性，方便在所有子类中以统一的格式输出日志。
    // 3. **沉浸式状态栏**: 集成了`ImmersionBar`库，简化了状态栏和导航栏的样式定制。通过`immersionBar`属性暴露实例，并在`onDestroy`时自动销毁，避免内存泄漏。
    // 4. **第三方服务集成**: 在`onResume`和`onPause`中自动调用友盟统计（MobclickAgent）的生命周期方法，实现了无侵入式的页面统计功能。
    // 5. **UI标准化行为**: 统一处理了Toolbar的返回按钮（`android.R.id.home`）点击事件，默认行为是调用`onBackPressed()`，简化了返回逻辑的实现。
    // 6. **API兼容性处理**: 提供了`isDestroyedCompatible`属性，解决了在Android API 17以下版本中`isDestroyed()`方法不可用的问题，提供了跨版本的统一判断方式。
    // 7. **上下文提供**: 提供了`viewContext`属性，方便在需要Context的地方（尤其是在MVP的Presenter中）直接使用，而无需传递`this`。
    //endregion

    //region How: 核心技术
    // 1. **抽象类 (Abstract Class)**: 使用`abstract`关键字定义，不能被直接实例化，必须由子类继承。这是实现模板方法设计模式的基础。
    // 2. **Kotlin属性代理 (Delegated Properties)**: `rxManager`, `TAG`, `immersionBar`, `viewContext`都使用了`by lazy`的属性代理。这使得它们的初始化被推迟到首次访问时，实现了懒加载，优化了性能，并使代码更简洁。
    // 3. **生命周期回调 (Lifecycle Callbacks)**: 重写了`onCreate`, `onResume`, `onPause`, `onDestroy`等Activity的生命周期方法，在这些关键节点上注入通用逻辑（如初始化、资源释放、统计上报）。
    // 4. **依赖管理**: 将`CompositeDisposable`、`ImmersionBar`等工具类的实例化和销毁逻辑封装在基类中，上层代码无需关心它们的具体实现和生命周期。
    // 5. **兼容性判断**: 通过`Build.VERSION.SDK_INT`进行运行时版本检查，为`isDestroyed`提供了两种不同的实现，确保在所有支持的Android版本上都能正常工作。
    //endregion

    /**
     * 用于管理该Activity生命周期内的所有RxJava订阅，防止内存泄漏。
     * 在`onDestroy`时会自动调用`clear()`和`dispose()`。
     */
    protected val rxManager by lazy { CompositeDisposable() }

    /**
     * 日志标签，默认为当前Activity的类名。
     */
    protected val TAG: String by lazy { this::class.java.simpleName }

    // 用于在API 17以下版本兼容isDestroyed属性
    private var destroyed = false

    /**
     * 沉浸式状态栏管理库的实例，懒加载初始化。
     * 子类可以通过它来定制状态栏和导航栏样式。
     */
    protected val immersionBar by lazy { ImmersionBar.with(this)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        //在Activity创建时记录日志
        KLog.t(TAG).d("onCreate $savedInstanceState")
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        //友盟统计：页面恢复时上报
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        //友盟统计：页面暂停时上报
        MobclickAgent.onPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        //在Activity销毁时，清理所有RxJava订阅，防止内存泄漏
        rxManager.clear()
        rxManager.dispose()
        KLog.t(TAG).d("onDestroy")
        //销毁沉浸式状态栏，防止内存泄漏
        immersionBar.destroy()
        //标记Activity已销毁，用于API 17以下的兼容判断
        destroyed = true
    }

    /**
     * 统一处理`onOptionsItemSelected`事件，特别是`android.R.id.home`（通常是Toolbar的返回箭头）。
     * 点击返回箭头时，执行`onBackPressed()`操作，实现统一的返回行为。
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 提供一个兼容所有API版本的`isDestroyed`判断。
     * API 17及以上，使用系统原生的`isDestroyed()`方法。
     * API 17以下，通过我们自己维护的`destroyed`标志位和`isFinishing()`结合判断。
     */
    val isDestroyedCompatible: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                isDestroyedCompatible17
            } else {
                destroyed || super.isFinishing()
            }
        }

    @get:TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private val isDestroyedCompatible17: Boolean
        get() = super.isDestroyed()

    /**
     * 提供一个方便的Context引用，其值就是Activity本身。
     * 使用`lazy`代理确保只在第一次使用时初始化。
     */
    val viewContext: Context by lazy { this }
}
