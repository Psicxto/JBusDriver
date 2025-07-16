package me.jbusdriver.ui.holder

import android.content.Context
import android.graphics.Bitmap
import android.support.v7.graphics.Palette
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import io.reactivex.Flowable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.layout_detail_relative_movies.view.*
import me.jbusdriver.R
import me.jbusdriver.base.GlideApp
import me.jbusdriver.base.SchedulersCompat
import me.jbusdriver.base.SimpleSubscriber
import me.jbusdriver.base.inflate
import me.jbusdriver.common.toGlideNoHostUrl
import me.jbusdriver.mvp.bean.Movie
import me.jbusdriver.mvp.bean.convertDBItem
import me.jbusdriver.mvp.model.CollectModel
import me.jbusdriver.ui.activity.MovieDetailActivity
import me.jbusdriver.ui.data.AppConfiguration
import me.jbusdriver.ui.data.contextMenu.LinkMenu
import java.util.*

/**
 * 用于详情页面展示“相关电影”的 Holder。
 * 它内部使用一个水平滚动的 RecyclerView 来展示电影列表。
 */
class RelativeMovieHolder(context: Context) : BaseHolder(context) {

    // 懒加载并初始化 Holder 的视图
    val view by lazy {
        weakRef.get()?.let {
            it.inflate(R.layout.layout_detail_relative_movies).apply {
                // 设置 RecyclerView 为水平线性布局
                rv_recycle_relative_movies.layoutManager =
                        LinearLayoutManager(it, LinearLayoutManager.HORIZONTAL, false)
                // 绑定适配器
                relativeAdapter.bindToRecyclerView(rv_recycle_relative_movies)
                // 启用嵌套滚动
                rv_recycle_relative_movies.isNestedScrollingEnabled = true
                // 设置 item 点击事件，点击后跳转到对应的电影详情页
                relativeAdapter.setOnItemClickListener { _, v, position ->
                    relativeAdapter.data.getOrNull(position)?.let {
                        MovieDetailActivity.start(v.context, it)
                    }
                }
                // 设置 item 长按事件，弹出上下文菜单
                relativeAdapter.setOnItemLongClickListener { _, view, position ->
                    relativeAdapter.data.getOrNull(position)?.let { movie ->
                        // 根据是否已收藏，动态生成菜单项
                        val action = (if (CollectModel.has(movie.convertDBItem())) LinkMenu.movieActions.minus("收藏")
                        else LinkMenu.movieActions.minus("取消收藏")).toMutableMap()

                        // 如果开启了分类功能，替换“收藏”为“收藏到分类...”
                        if (AppConfiguration.enableCategory) {
                            val ac = action.remove("收藏")
                            if (ac != null) {
                                action["收藏到分类..."] = ac
                            }
                        }

                        // 使用 MaterialDialog 显示菜单
                        MaterialDialog.Builder(view.context).title(movie.title)
                            .items(action.keys)
                            .itemsCallback { _, _, _, text ->
                                action[text]?.invoke(movie)
                            }
                            .show()
                    }
                    return@setOnItemLongClickListener true
                }
            }
        } ?: error("context ref is finish")
    }

    // 相关电影列表的适配器
    private val relativeAdapter: BaseQuickAdapter<Movie, BaseViewHolder> by lazy {
        object : BaseQuickAdapter<Movie, BaseViewHolder>(R.layout.layout_detail_relative_movies_item) {
            override fun convert(holder: BaseViewHolder, item: Movie) {
                // 使用 Glide 加载图片，并将其作为 Bitmap 处理
                GlideApp.with(holder.itemView.context).asBitmap().load(item.imageUrl.toGlideNoHostUrl)
                    .into(object : BitmapImageViewTarget(holder.getView(R.id.iv_relative_movie_image)) {
                        override fun setResource(resource: Bitmap?) {
                            super.setResource(resource)
                            resource?.let {
                                // 图片加载成功后，使用 Palette 库从图片中提取颜色
                                Flowable.just(it).map {
                                    Palette.from(it).generate() // 生成调色板
                                }.compose(SchedulersCompat.io()) // 切换到 IO 线程进行计算
                                    .subscribeWith(object : SimpleSubscriber<Palette>() {
                                        override fun onNext(it: Palette) {
                                            super.onNext(it)
                                            // 从调色板中选择一个合适的颜色样本
                                            val swatch = listOfNotNull(
                                                it.lightMutedSwatch,
                                                it.lightVibrantSwatch,
                                                it.vibrantSwatch,
                                                it.mutedSwatch
                                            )
                                            if (!swatch.isEmpty()) {
                                                // 随机选择一个颜色样本，并将其应用到标题的背景和文本颜色上
                                                swatch[randomNum(swatch.size)].let {
                                                    holder.setBackgroundColor(R.id.tv_relative_movie_title, it.rgb)
                                                    holder.setTextColor(R.id.tv_relative_movie_title, it.bodyTextColor)
                                                }
                                            }
                                        }
                                    })
                                    .addTo(rxManager) // 将订阅添加到 rxManager 中统一管理

                            }
                        }
                    })
                // 加载电影标题
                holder.setText(R.id.tv_relative_movie_title, item.title)
            }
        }
    }


    private val random = Random()
    // 生成一个指定范围内的随机数
    private fun randomNum(number: Int) = Math.abs(random.nextInt() % number)

    /**
     * 初始化 Holder，加载相关电影数据。
     * @param relativeMovies 相关电影数据列表
     */
    fun init(relativeMovies: List<Movie>) {
        // 如果数据为空，显示提示信息
        if (relativeMovies.isEmpty()) view.tv_movie_relative_none_tip.visibility = View.VISIBLE
        else {
            // 否则，加载数据到适配器
            relativeAdapter.setNewData(relativeMovies)
        }
    }

}