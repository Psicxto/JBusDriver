/**
 * Why: 设计目的
 * `Category` 类是一个数据类（data class），其核心设计目的是为了在数据库中表示和操作具有层级关系的分类数据。
 * 在一个复杂的收藏系统中，用户可能需要创建多级分类来组织他们的收藏项（如电影、演员、链接等）。
 * 这个类的设计旨在：
 * 1.  **结构化数据**：以清晰的、面向对象的方式封装一个分类所必需的全部信息，如名称、父分类ID、层级路径（tree）和排序顺序。
 * 2.  **简化数据库操作**：提供 `cv()` 方法，将对象模型直接转换为 Android 数据库操作所需的 `ContentValues`，极大地简化了插入和更新数据库记录的代码。
 * 3.  **支持层级结构**：通过 `pid`（父ID）和 `tree`（路径树）字段，能够有效地表示和查询无限深度的分类层级。`tree` 字段的设计是一种常见的数据库层级数据存储优化技巧（路径枚举），便于快速查询某个分类下的所有子分类。
 * 4.  **提供默认分类**：通过顶层属性预定义了几个核心的、不可删除的根分类（电影、演员、链接），为系统提供了初始的数据结构，并简化了业务逻辑中的判断。
 * 5.  **提高代码可读性和健壮性**：使用数据类自动生成 `equals()`, `hashCode()`, `toString()` 等方法，并自定义了 `equals()` 和 `equalAll()` 以满足不同的比较需求。
 */

/**
 * What: 功效作用
 * - **数据模型**：作为分类表的 ORM (Object-Relational Mapping) 模型，每个 `Category` 实例对应数据库中的一条记录。
 * - **属性**：
 *   - `name`: 分类名称。
 *   - `pid`: 父分类的 ID，根分类为 -1。
 *   - `tree`: 层级路径，例如 "1/3/5/" 表示 ID 为 5 的分类在 ID 为 3 的分类下，而 3 又在 ID 为 1 的分类下。这种设计便于快速进行层级查询。
 *   - `order`: 兄弟分类间的排序值。
 *   - `id`: 分类自身的唯一 ID，在数据库中是主键，可空表示尚未存入数据库。
 *   - `depth`: 通过 `tree` 属性懒加载计算出的分类深度。
 * - **方法**：
 *   - `cv()`: 将 `Category` 对象转换为 `ContentValues`，用于数据库的插入或更新。
 *   - `equals()`: 重写了 `equals` 方法，仅通过 `id` 来判断两个分类对象是否相等，符合数据库实体对象的比较逻辑。
 *   - `equalAll()`: 提供一个更严格的比较方法，判断两个分类对象的所有核心属性是否都相等。
 * - **预定义实例**：
 *   - `MovieCategory`, `ActressCategory`, `LinkCategory`: 创建了三个预设的、固定的根分类实例，代表三种主要的收藏类型。
 *   - `AllFirstParentDBCategoryGroup`: 一个懒加载的 `ArrayMap`，将预设的根分类的 ID 与其对象实例关联起来，便于快速查找。
 */

/**
 * How: 核心技术
 * 1.  **Kotlin 数据类 (`data class`)**: 自动为主构造函数中的属性生成 `equals()`, `hashCode()`, `toString()`, `copy()` 和 `componentN()` 函数，极大地减少了模板代码。
 * 2.  **懒加载委托 (`by lazy`)**: `depth` 属性使用 `by lazy` 实现。其计算逻辑（分割 `tree` 字符串）只有在第一次访问 `depth` 属性时才会执行，之后会缓存结果。这是一种性能优化，避免了不必要的计算。
 * 3.  **`@delegate:Transient` 注解**: `depth` 属性被 `transient` 修饰，这意味着在使用某些序列化框架（如 Gson）时，这个字段将被忽略，不会被序列化。这是合理的，因为它是一个派生值，可以随时从 `tree` 属性计算得出。
 * 4.  **扩展函数思想（`cv()`）**: `cv()` 方法虽然不是扩展函数，但体现了类似的封装思想，将与特定框架（Android `ContentValues`）相关的转换逻辑封装在模型类内部，保持了外部代码的整洁。
 * 5.  **顶层属性和函数**: `MovieCategory` 等预定义分类和 `AllFirstParentDBCategoryGroup` 都是在文件顶层声明的，作为单例常量在整个应用中共享，无需通过类名访问。
 * 6.  **路径枚举模型 (Path Enumeration)**: `tree` 字段的设计是数据库中存储层级数据的一种经典模式。它通过将从根到当前节点的路径存储在一个字符串中，使得查询某个节点的所有子孙节点变得非常高效（例如，`WHERE tree LIKE '1/3/%'`）。
 */
package me.jbusdriver.common.bean.db

import android.content.ContentValues
import me.jbusdriver.base.arrayMapof

data class Category(val name: String, val pid: Int = -1, val tree: String, var order: Int = 0) {
    var id: Int? = null

    @delegate:Transient
    val depth: Int by lazy { tree.split("/").filter { it.isNotBlank() }.size }

    fun cv(update: Boolean = false): ContentValues = ContentValues().also {
        if (id != null || update) it.put(CategoryTable.COLUMN_ID, id)
        it.put(CategoryTable.COLUMN_NAME, name)
        it.put(CategoryTable.COLUMN_P_ID, pid)
        it.put(CategoryTable.COLUMN_TREE, tree)
        it.put(CategoryTable.COLUMN_ORDER, order)
    }

    override fun equals(other: Any?) =
        other?.let { (it as? Category)?.id == this.id } ?: false

    fun equalAll(other: Category?) =
        other?.let { it.id == this.id && it.name == this.name && it.pid == this.pid && it.tree == this.tree }
            ?: false
}

/**
 * 预留 [3..9]的分类
 */
val MovieCategory = Category("默认电影分类", -1, "1/", Int.MAX_VALUE).apply { id = 1 }
val ActressCategory = Category("默认演员分类", -1, "2/", Int.MAX_VALUE).apply { id = 2 }
val LinkCategory = Category("默认链接分类", -1, "10/", Int.MAX_VALUE).apply { id = 10 }
val AllFirstParentDBCategoryGroup by lazy { arrayMapof(1 to MovieCategory, 2 to ActressCategory, 10 to LinkCategory) }