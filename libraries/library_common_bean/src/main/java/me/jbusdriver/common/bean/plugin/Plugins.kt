/**
 * Why: 设计目的
 * `Plugins` 类的设计目的是作为一个容器或包装器，用于封装从某个数据源（如远程服务器的API响应）获取到的插件列表。
 * 这种设计的考虑如下：
 * 1.  **结构化API响应**：当使用像 Gson 或 Moshi 这样的库来解析 JSON 响应时，将 JSON 的顶层结构映射到一个类是非常常见的做法。如果服务器返回的 JSON 是 `{"internal": [...]}` 这样的结构，那么 `Plugins` 类就能完美地与之对应，从而简化反序列化过程。
 * 2.  **封装与抽象**：它将插件列表 `internal` 封装在一个具名的类中，而不是直接使用一个裸的 `List<PluginBean>`。这提高了代码的可读性，`Plugins` 这个类型比 `List<PluginBean>` 更能清晰地表达其业务含义——它代表了一组插件的集合。
 * 3.  **未来扩展性**：虽然当前 `Plugins` 类非常简单，只包含一个 `internal` 列表，但这种包装器设计为未来的扩展提供了可能。例如，未来服务器的响应可能增加一些元数据，如 `"apiVersion": 2`, `"lastUpdated": "2023-10-27"` 等。届时，只需在 `Plugins` 类中添加相应的字段即可，而不需要改变所有使用插件列表的地方的签名。
 */

/**
 * What: 功效作用
 * - **数据容器**：它的主要作用是作为一个数据传输对象（DTO, Data Transfer Object），用于承载一个 `PluginBean` 的列表。
 * - **属性**：
 *   - `internal`: 一个 `List<PluginBean>` 类型的只读属性，用于存储插件信息对象的集合。它被初始化为空列表，其内容通常会在反序列化时被填充。
 * - **调试支持**：重写了 `toString()` 方法，以便在打印 `Plugins` 对象时，能够输出其包含的 `internal` 列表的内容，这对于调试非常有用。
 */

/**
 * How: 核心技术
 * 1.  **Kotlin 类 (`class`)**: 一个标准的 Kotlin 类定义。
 * 2.  **属性初始化**: `val internal: List<PluginBean> = emptyList()` 声明了一个名为 `internal` 的只读属性，并将其初始化为一个空的 `List`。这确保了即使在没有成功解析或填充数据的情况下，`internal` 属性也永远不会是 `null`，从而避免了空指针异常。
 * 3.  **`toString()` 重写**: 通过 `override fun toString()`: 重写了 `Any` 类的 `toString` 方法，提供了对该类实例更有意义的字符串表示形式，便于日志记录和调试。
 */
package me.jbusdriver.common.bean.plugin

class Plugins {
    val internal: List<PluginBean> = emptyList()
    override fun toString(): String {
        return "Plugins(internal=$internal)"
    }
}