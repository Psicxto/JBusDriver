/**
 * Why: 设计目的
 * `PluginBean` 是一个数据类，其核心设计目的是为了创建一个独立于特定插件框架的、轻量级的插件信息模型。
 * 在项目中，可能使用了第三方插件框架（如此处的 `com.wlqq.phantom.library.pm.PluginInfo`），但直接在整个应用中依赖这个框架的 `PluginInfo` 类会导致强耦合。
 * `PluginBean` 的设计旨在：
 * 1.  **解耦**：通过定义自己的 `PluginBean`，业务逻辑层（如UI、数据管理）可以只依赖于这个应用内部定义的、稳定的数据结构，而无需关心底层插件框架的具体实现。如果未来需要更换插件框架，只需修改 `toPluginBean()` 这个转换函数，而业务层的代码基本不受影响。
 * 2.  **数据裁剪与扩展**：`PluginBean` 只包含应用关心的核心字段（如名称、版本、描述、下载URL等）。它可以从原始的 `PluginInfo` 中提取所需数据，并可以轻松地添加应用特有的字段（如 `eTag`, `url`），而无需修改原始的框架类。
 * 3.  **简化比较逻辑**：通过实现 `Comparable<PluginBean>` 接口，并重写 `compareTo` 方法，为插件对象提供了一种内置的、基于 `versionCode` 的自然排序能力。这使得插件列表的排序和版本比较变得非常简单直接。
 * 4.  **提供转换工具**：`toPluginBean()` 扩展函数的存在，使得从框架模型到应用模型的转换过程变得清晰、便捷和可重用。
 */

/**
 * What: 功效作用
 * - **数据模型**：定义了一个插件所需的核心属性：
 *   - `name`: 插件名称（通常是包名）。
 *   - `versionCode`: 插件的版本号，用于程序判断版本新旧。
 *   - `versionName`: 插件的版本名，用于向用户展示。
 *   - `desc`: 插件的描述信息。
 *   - `eTag`: 插件文件的 ETag，可用于缓存验证，判断文件是否已更新。
 *   - `url`: 插件的下载地址。
 * - **版本比较**：实现了 `Comparable` 接口，允许直接使用比较运算符（如 `>`, `<`）或集合的 `sort()` 方法来比较两个 `PluginBean` 实例的版本高低。
 * - **模型转换**：提供了一个 `PluginInfo.toPluginBean()` 的扩展函数，可以方便地将 `Phantom` 插件框架的 `PluginInfo` 对象转换为应用自身的 `PluginBean` 对象。
 */

/**
 * How: 核心技术
 * 1.  **Kotlin 数据类 (`data class`)**: 自动生成 `equals()`, `hashCode()`, `toString()`, `copy()` 和 `componentN()` 等方法，简化了 POJO 类的创建。
 * 2.  **接口实现 (`Comparable`)**: 通过实现 `Comparable<PluginBean>` 接口，并提供 `compareTo` 方法的具体实现，赋予了该类可比较的能力。比较逻辑是简单地用当前对象的 `versionCode` 减去另一个对象的 `versionCode`，返回值的正负和零决定了两者的大小关系。
 * 3.  **操作符重载 (`operator fun compareTo`)**: `compareTo` 方法用 `operator` 关键字修饰，这意味着 Kotlin 允许我们使用对应的操作符（如 `>`、`<`、`>=`、`<=`）来调用这个函数，使得代码更具可读性。例如，`pluginA > pluginB` 会被自动翻译成 `pluginA.compareTo(pluginB) > 0`。
 * 4.  **扩展函数 (`fun PluginInfo.toPluginBean()`)**: 这是 Kotlin 的一个强大特性。它允许我们为已有的类（即使是第三方库的类，如 `PluginInfo`）添加新的函数，而无需修改其源代码。这里为 `PluginInfo` 添加了 `toPluginBean` 方法，使其能“无缝”地转换为 `PluginBean`。
 */
package me.jbusdriver.common.bean.plugin

import com.wlqq.phantom.library.pm.PluginInfo

data class PluginBean(
    val name: String,
    val versionCode: Int,
    val versionName: String,
    val desc: String,
    val eTag: String,
    val url: String
) : Comparable<PluginBean> {

    override operator fun compareTo(other: PluginBean) = this.versionCode - other.versionCode

}


fun PluginInfo.toPluginBean() = PluginBean(
    name = this.packageName, versionCode = this.versionCode, versionName = this.versionName,
    desc = this.toString(), eTag = "", url = ""
)
