package me.jbusdriver.base.common

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.umeng.analytics.MobclickAgent
import io.reactivex.disposables.CompositeDisposable
import me.jbusdriver.base.JBusManager
import me.jbusdriver.base.KLog

/**
 * @author jbusdriver
 * @date 2016/8/11
 * App中所有Fragment的基类，提供了通用的功能和生命周期管理。
 * 主要功能包括：
 * 1. RxJava订阅的生命周期管理。
 * 2. 日志记录。
 * 3. 友盟统计的页面计时。
 * 4. 提供安全的Context引用。
 * 5. 提供一个临时的Bundle用于数据暂存。
 */
open class BaseFragment : Fragment() {

    //region Why: 设计目的
    // `BaseFragment`的设计目的与`BaseActivity`类似，旨在为项目中所有的Fragment提供一个统一的、包含通用功能的基类。
    // 通过封装Fragment生命周期管理、日志、统计、上下文安全获取等常用操作，
    // 极大地简化了子类Fragment的开发，减少了重复代码，并确保了在整个应用中Fragment行为的一致性和健壮性。
    // 特别是针对Fragment复杂的生命周期，提供了更精细的资源管理（如在`onDestroyView`和`onDestroy`中分别处理订阅）。
    //endregion

    //region What: 功效作用
    // 1. **RxJava订阅管理**: `rxManager`在Fragment的生命周期内管理异步任务。关键在于它在`onDestroyView`时调用`clear()`，这可以取消所有与视图相关的订阅，防止在Fragment视图重建（如从后台返回）时发生内存泄漏或UI更新错误。在`onDestroy`时调用`dispose()`，彻底终结订阅容器。
    // 2. **日志**: 提供基于类名的`TAG`属性，方便进行统一的日志记录。
    // 3. **友盟统计**: 在`onResume`和`onPause`中自动调用`MobclickAgent.onPageStart/End`，实现了对Fragment作为独立页面的时长统计，无需在每个业务Fragment中手动添加。
    // 4. **安全的Context获取**: `viewContext`属性提供了一个安全的获取`Context`的方式。它首先尝试获取`activity`，如果`activity`为`null`（可能发生在Fragment生命周期的边缘状态或异步回调中），则回退到全局的`ApplicationContext`。这可以有效避免`NullPointerException`。
    // 5. **临时数据存储**: `tempSaveBundle`提供了一个`Bundle`实例，用于临时存储数据。虽然它没有直接与`onSaveInstanceState`挂钩，但可以作为Fragment内部逻辑的一个便捷的数据暂存区。
    //endregion

    //region How: 核心技术
    // 1. **开放类 (Open Class)**: 使用`open`关键字，使得这个类可以被其他类继承。
    // 2. **Kotlin属性代理 (`by lazy`)**: `TAG`, `rxManager`, `tempSaveBundle`都使用了懒加载，确保只在首次使用时才进行初始化，提高了效率。
    // 3. **Fragment生命周期精细化管理**: 充分利用了Fragment的生命周期回调。在`onDestroyView`（视图销毁）和`onDestroy`（实例销毁）这两个不同的阶段对`rxManager`进行不同程度的清理（`clear` vs `dispose`），这是处理Fragment资源释放的最佳实践。
    // 4. **空安全操作符 (Elvis Operator `?:`)**: 在`viewContext`的getter中，使用了Elvis操作符`?:`来提供一个备用值（`JBusManager.context`），当`activity`为`null`时，保证了返回的`Context`非空，代码更健壮。
    // 5. **依赖全局上下文**: 当`activity`不可用时，依赖于一个单例或全局可访问的`JBusManager.context`来获取`ApplicationContext`，这是一种在无法获取Activity级别Context时的常见备用策略。
    //endregion

    /**
     * 日志标签，默认为当前Fragment的类名。
     */
    protected val TAG: String by lazy { this::class.java.simpleName }

    /**
     * 用于管理该Fragment生命周期内的所有RxJava订阅。
     * 在`onDestroyView`时会`clear()`，在`onDestroy`时会`dispose()`，以防止内存泄漏。
     */
    val rxManager by lazy { CompositeDisposable() }

    /**
     * 一个临时的Bundle，可用于在Fragment重建时保存和恢复少量数据，但其在此处未与`onSaveInstanceState`联动，
     * 可能用于其他自定义的暂存目的。
     */
    protected val tempSaveBundle by lazy { Bundle() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        KLog.t(TAG).d("onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        //友盟统计：当Fragment可见时，开始统计页面时长
        MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        //友盟统计：当Fragment不可见时，结束统计页面时长
        MobclickAgent.onPageEnd(TAG)
    }

    /**
     * 当Fragment的视图被销毁时调用。
     * 在此清理与视图相关的资源，特别是`rxManager`中的订阅，防止因视图重建而导致的重复订阅或内存泄漏。
     */
    override fun onDestroyView() {
        super.onDestroyView()
        KLog.t(TAG).d("onDestroyView")
        // 清理所有与View生命周期相关的订阅，但保留rxManager容器以便在下次onCreateView后继续使用
        rxManager.clear()
    }

    /**
     * 当Fragment本身被销毁时调用。
     * 在此彻底释放所有资源，包括`rxManager`。
     */
    override fun onDestroy() {
        super.onDestroy()
        // 再次清理，并彻底废弃rxManager容器，防止内存泄漏
        rxManager.clear()
        rxManager.dispose()
    }

    /**
     * 提供一个安全的Context引用。
     * 优先返回`activity`，如果`activity`为null（例如Fragment已脱离Activity），则返回全局的`ApplicationContext`。
     * 这避免了在异步回调中使用`getActivity()`可能导致的空指针异常。
     */
    val viewContext: Context
        get() = activity ?: JBusManager.context
}
