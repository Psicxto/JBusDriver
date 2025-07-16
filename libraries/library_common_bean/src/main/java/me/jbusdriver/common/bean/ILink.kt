/**
 * Why: 设计目的
 * `ILink` 接口的设计目的在于创建一个高度抽象和可复用的契约，用于表示任何具有“链接”属性的对象。
 * 在应用中，很多不同类型的数据实体（如电影、演员、磁力链接等）都可能包含一个可点击的链接。通过定义此接口，可以：
 * 1.  **实现多态与统一处理**：任何实现了 `ILink` 接口的对象，都可以被视为一个“可链接”的实体。这使得我们可以编写通用的逻辑来处理这些对象的链接，例如，统一的链接点击跳转、链接的收藏、分享等，而无需关心对象的具体类型。
 * 2.  **强制约束**：接口强制实现类必须提供一个 `link` 属性，确保了链接信息的存在，避免了空指针或属性缺失的风险。
 * 3.  **继承与组合**：它同时继承了 `ICollectCategory` 和 `Serializable` 接口，这是一个关键的设计决策。
 *     -   继承 `ICollectCategory` 意味着所有“链接”默认都是“可被分类收藏的”，这为构建统一的收藏系统奠定了基础。
 *     -   继承 `Serializable` 意味着所有“链接”对象都可以被序列化，从而可以方便地在 Android 组件（如 Activity, Fragment）之间通过 Intent 或 Bundle 进行传递，或者持久化到磁盘。
 * 4.  **解耦**：依赖此接口的组件（如一个通用的链接列表适配器）与具体的数据模型解耦，增强了系统的灵活性和可扩展性。
 */

/**
 * What: 功效作用
 * 该接口为实现类定义了以下核心能力：
 * - `val link: String`: 提供一个只读的字符串属性，用于获取该对象的 URL 链接。
 * - **可分类收藏**：通过继承 `ICollectCategory`，获得了 `categoryId` 属性，使得对象可以被归入不同的收藏夹。
 * - **可序列化**：通过继承 `Serializable`，使得对象实例可以被转换成字节流，用于跨进程通信或持久化。
 */

/**
 * How: 核心技术
 * 1.  **Kotlin 接口 (`interface`)**: 定义了一个接口，作为行为和属性的契约。
 * 2.  **接口继承**: `ILink` 同时继承了另外两个接口 `ICollectCategory` 和 `Serializable`。这是 Kotlin（和 Java）支持多重接口继承的特性，允许一个类型同时具备多种不同的能力或特征。
 * 3.  **抽象属性 (`val link: String`)**: 声明了一个只读的抽象属性 `link`。`val` 关键字意味着实现类只需要为该属性提供一个 getter 即可。具体的返回值由每个实现类根据自身逻辑决定。
 * 4.  **标记接口 (`Serializable`)**: `Serializable` 是一个标记接口（Marker Interface），它本身没有任何方法。一个类实现此接口，只是为了向 JVM “声明”该类的对象是可被序列化的。
 */
package me.jbusdriver.common.bean

import java.io.Serializable

interface ILink : ICollectCategory, Serializable {
    val link: String
}