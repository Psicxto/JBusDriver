/**
 * Why: 设计目的
 * `CategoryTable` 是一个 Kotlin 的 `object`（单例对象），其唯一的设计目的是为了集中管理和定义 `t_category` 数据库表的元数据，即表名和所有列名。
 * 这种设计的核心思想是 **“关注点分离”** 和 **“避免魔法字符串”** (Magic Strings)。
 * 1.  **避免魔法字符串**：在数据库操作代码（如 SQL 查询语句、`ContentValues` 的 key）中，直接硬编码字符串（如 "t_category", "name"）是一种不好的实践。这些字符串被称为“魔法字符串”，因为它们容易因拼写错误而导致运行时错误，且难以进行静态检查和重构。将这些常量统一定义在一个地方，可以确保整个应用中都使用相同的、正确的值。
 * 2.  **提高可维护性**：如果未来需要修改表名或列名，只需在这个文件中修改一次即可，所有引用该常量的地方都会自动更新。这极大地降低了维护成本和出错的风险。
 * 3.  **代码清晰与自文档化**：通过 `CategoryTable.COLUMN_NAME` 这样的引用，代码的意图变得非常清晰，比直接使用 `"name"` 更具可读性。它本身就起到了文档的作用，告诉开发者这个常量代表的是分类表的名称字段。
 * 4.  **类型安全与编译时检查**：使用常量可以利用 IDE 的代码补全和编译器的静态检查，减少拼写错误。
 */

/**
 * What: 功效作用
 * - **定义表名**：`TABLE_NAME` 常量定义了分类表在数据库中的名称为 `t_category`。
 * - **定义列名**：
 *   - `COLUMN_ID`: 定义了主键列的名称为 `id`。
 *   - `COLUMN_P_ID`: 定义了父分类ID列的名称为 `pid`。
 *   - `COLUMN_NAME`: 定义了分类名称列的名称为 `name`。
 *   - `COLUMN_TREE`: 定义了层级路径树列的名称为 `tree`。
 *   - `COLUMN_ORDER`: 定义了排序列的名称为 `orderIndex`。
 * - **提供全局访问点**：作为一个 `object`，它在整个应用中只有一个实例，可以从任何地方通过 `CategoryTable.TABLE_NAME` 的方式直接、方便地访问这些常量。
 */

/**
 * How: 核心技术
 * 1.  **Kotlin `object`**: `object` 是 Kotlin 中实现单例模式的关键字。它会创建一个类并只生成一个实例。这非常适合用于定义一组相关的常量或工具函数，因为它们不需要多个实例。
 * 2.  **`const val`**: `const` 是 Kotlin 中的一个修饰符，用于声明编译时常量（Compile-time Constants）。
 *    - **编译时常量**：`const val` 的值必须在编译期间就能确定，不能是函数返回值或任何在运行时才能确定的值。这使得编译器可以直接将这些常量的值内联到使用它们的地方，从而提高性能。
 *    - **适用范围**：`const val` 只能在顶层或 `object` 中声明，且必须是基本类型或 `String` 类型。
 *    - **与 `val` 的区别**：普通的 `val` 是只读的，但其值可以在运行时确定。而 `const val` 必须在编译时就确定。在这个场景下，表名和列名是固定不变的，因此使用 `const val` 是最合适的选择。
 */
package me.jbusdriver.common.bean.db

//region collect category
object CategoryTable {
    const val TABLE_NAME = "t_category"
    const val COLUMN_ID = "id"
    const val COLUMN_P_ID = "pid"
    const val COLUMN_NAME = "name"
    const val COLUMN_TREE = "tree"
    const val COLUMN_ORDER = "orderIndex"
}