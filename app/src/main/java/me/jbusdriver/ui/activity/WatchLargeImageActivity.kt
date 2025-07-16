package me.jbusdriver.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Priority
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.gyf.barlibrary.ImmersionBar
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_watch_large_image.*
import kotlinx.android.synthetic.main.layout_large_image_item.view.*
import me.jbusdriver.R
import me.jbusdriver.base.*
import me.jbusdriver.base.common.BaseActivity
import me.jbusdriver.base.http.OnProgressListener
import me.jbusdriver.base.http.addProgressListener
import me.jbusdriver.base.http.removeProgressListener
import me.jbusdriver.common.toGlideNoHostUrl
import java.io.File
import kotlin.random.Random

/**
 * 查看大图的 Activity，支持手势缩放和左右滑动切换
 */
class WatchLargeImageActivity : BaseActivity() {

    //从 Intent 中懒加载图片 URL 列表
    private val urls by lazy {
        intent.getStringArrayListExtra(INTENT_IMAGE_URL) ?: emptyList<String>()
    }
    //存储每个页面 View 的列表
    private val imageViewList: ArrayList<View> = arrayListOf()
    //从 Intent 中懒加载当前显示的图片索引
    private val index by lazy { intent.getIntExtra(INDEX, -1) }
    //懒加载图片保存目录
    private val imageSaveDir by lazy {
        val packageName = JBusManager.context.packageName
        val pathSuffix = File.separator + "download" + File.separator + "image" + File.separator
        //尝试在外部存储创建目录，失败则在内部缓存目录创建
        createDir(Environment.getExternalStorageDirectory().absolutePath + File.separator + packageName + pathSuffix)
            ?: createDir(JBusManager.context.cacheDir.absolutePath + packageName + pathSuffix)
            ?: error("cant not create collect dir in anywhere") //都失败则抛出异常

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_large_image)
        initWidget() //初始化控件
    }

    override fun onDestroy() {
        // 清理所有PhotoView和Glide资源，防止内存泄漏
        imageViewList.forEach { view ->
            view.pv_image_large?.let { photoView ->
                GlideApp.with(this).clear(photoView)
                photoView.setImageDrawable(null)
            }
        }
        super.onDestroy()
    }


    @SuppressLint("SetTextI18n")
    private fun initWidget() {
        immersionBar.transparentBar().init() //设置沉浸式状态栏
        val statusBarHeight = ImmersionBar.getStatusBarHeight(this)

        //为每个 URL 创建一个 View 并添加到列表中
        urls.mapTo(imageViewList) {
            this@WatchLargeImageActivity.inflate(R.layout.layout_large_image_item).apply {
                //调整进度条的顶部边距，使其位于状态栏下方
                (pb_hor_progress.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = statusBarHeight
            }
        }

        vp_largeImage.adapter = MyViewPagerAdapter() //设置 ViewPager 适配器
        // 设置ViewPager缓存页面数，避免频繁创建销毁
        vp_largeImage.offscreenPageLimit = 1
        vp_largeImage.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                //页面切换时更新索引显示
                this@WatchLargeImageActivity.tv_url_index.text = "${position + 1} / ${imageViewList.size}"
                // 清理非当前页面的PhotoView缩放状态，重置为1.0
                imageViewList.forEachIndexed { index, view ->
                    if (index != position) {
                        view.pv_image_large?.let { photoView ->
                            if (photoView.scale != 1.0f) {
                                photoView.scale = 1.0f
                            }
                        }
                    }
                }
            }
        })
        //设置初始显示的页面
        vp_largeImage.currentItem = if (index == -1) 0 else index
        this@WatchLargeImageActivity.tv_url_index.text = "${vp_largeImage.currentItem + 1} / ${imageViewList.size}"

        //下载按钮点击事件
        iv_download.setOnClickListener {
            val url = urls[vp_largeImage.currentItem]
            //从 URL 中解析文件名，如果失败则生成一个随机文件名
            val fileName = url.urlPath.split("/").lastOrNull()
                ?: "${System.currentTimeMillis()}-${(Random(System.currentTimeMillis()).nextFloat() * 1000).toInt()}.jpg"
            //使用 Glide 下载图片文件
            Single.fromFuture(GlideApp.with(this).download(url).submit())
                .doOnSuccess { source ->
                    //下载成功后，将文件从 Glide 缓存复制到指定的保存目录
                    val target = File(imageSaveDir + fileName)
                    source.copyTo(target, true)
                }.subscribeOn(Schedulers.io()) //在 IO 线程执行文件操作
                .subscribeBy {
                    toast("文件保存至${imageSaveDir}下")
                }
                .addTo(rxManager)

        }

    }


    companion object {

        private const val INTENT_IMAGE_URL = "INTENT_IMAGE_URL"
        private const val INDEX = "currentIndex"

        /**
         * 启动大图查看器
         * @param context 上下文
         * @param urls 图片URL列表
         * @param index 初始显示的图片索引
         */
        fun startShow(context: Context, urls: List<String>, index: Int = -1) {
            val intent = Intent(context, WatchLargeImageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putStringArrayListExtra(INTENT_IMAGE_URL, ArrayList(urls))
            intent.putExtra(INDEX, index)
            context.startActivity(intent)
        }

    }

    /**
     * ViewPager 的适配器
     */
    inner class MyViewPagerAdapter : PagerAdapter() {

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val view = imageViewList[position]
            // 销毁页面时，清理PhotoView状态和Glide加载，防止内存泄漏
            view.pv_image_large?.let { photoView ->
                GlideApp.with(this@WatchLargeImageActivity).clear(photoView)
                photoView.setImageDrawable(null)
                // 重置PhotoView的缩放状态
                photoView.scale = 1.0f
            }
            container.removeView(view)//从容器中删除页卡
        }


        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            //实例化页面
            return imageViewList.getOrNull(position)?.apply {
                container.addView(this, 0)//添加页卡到容器
                loadImage(this, position) //加载图片
            } ?: error("can not instantiateItem for $position in $imageViewList")
        }

        private fun loadImage(view: View, position: Int) {
            // 检查Activity是否已销毁，防止异步加载导致崩溃
            if (isDestroyedCompatible) return
            
            view.findViewById<View>(R.id.pb_hor_progress)?.animate()?.alpha(1f)?.setDuration(300)?.start()
            //根据页面与当前页的距离，设置不同的加载优先级
            val offset = Math.abs(vp_largeImage.currentItem - position)
            val priority = when (offset) {
                in 0..1 -> Priority.IMMEDIATE //相邻页面最高优先级
                in 2..5 -> Priority.HIGH
                in 6..10 -> Priority.NORMAL
                else -> Priority.LOW
            }
            val url = urls[position]
            
            // 先清理之前的加载，避免图片错位
            GlideApp.with(this@WatchLargeImageActivity).clear(view.pv_image_large)
            
            //使用 Glide 加载图片
            GlideApp.with(this@WatchLargeImageActivity)
                .load(url.toGlideNoHostUrl) //加载URL
                .transition(DrawableTransitionOptions.withCrossFade()) //淡入淡出过渡效果
                .error(R.drawable.ic_image_error) //设置加载错误时显示的图片
                .fitCenter() //图片居中显示
                .priority(priority) //设置加载优先级
                .into(object : DrawableImageViewTarget(view.pv_image_large) {
                    //自定义 Target 以监听加载进度
                    val listener = object : OnProgressListener {
                        override fun onProgress(
                            imageUrl: String,
                            bytesRead: Long,
                            totalBytes: Long,
                            isDone: Boolean,
                            exception: Exception?
                        ) {
                            if (totalBytes == 0L) return
                            if (url != imageUrl) return //确保是当前图片的进度
                            postMain {
                                //view.pb_hor_progress.visibility = View.GONE
                                view.pb_hor_progress.isIndeterminate = false
                                view.pb_hor_progress.progress = (bytesRead * 1.0f / totalBytes * 100).toInt()
                                if (isDone) {
                                    //加载完成后隐藏进度条
                                    view.pb_hor_progress.animate().alpha(0f).setDuration(500).start()
                                }
                            }
                        }
                    }

                    override fun onLoadStarted(placeholder: Drawable?) {
                        super.onLoadStarted(placeholder)
                        //开始加载时，为 URL 添加进度监听器
                        addProgressListener(url, listener)
                    }

                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        super.onResourceReady(resource, transition)
                        //资源准备好后，移除进度监听器
                        removeProgressListener(url)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        //加载失败后，移除进度监听器
                        removeProgressListener(url)
                    }

                    override fun onStop() {
                        super.onStop()
                        //停止加载时，移除进度监听器
                        removeProgressListener(url)
                    }
                })

        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object` //判断视图是否为当前对象
        }

        override fun getCount(): Int {
            return imageViewList.size //返回页面数量
        }
    }
}
