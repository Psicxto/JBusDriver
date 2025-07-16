package me.jbusdriver.base.http;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * @author zhy
 * @date 16/3/1
 * @description
 * An OkHttp interceptor for logging HTTP requests and responses, which is useful for debugging network issues.
 * It can log request/response headers, method, URL, and body content.
 * The response body is only logged if it's a text-based format (e.g., JSON, XML, HTML).
 *
 * Why: 设计目的
 * 在应用开发和调试阶段，开发者需要一种简单有效的方式来监控和诊断网络通信问题。一个专门的日志拦截器可以透明地捕获所有通过 OkHttp 发出的请求和接收到的响应，
 * 并将关键信息（如 URL、请求方法、头部、请求体、响应码和响应体）格式化输出到日志中。这极大地简化了调试过程，使得开发者可以快速定位网络层面的错误，
 * 例如请求参数错误、服务器响应异常或数据格式问题。
 *
 * What: 功效作用
 * 1.  **请求日志记录**: 记录请求的方法（GET, POST等）、URL、请求头和请求体内容。
 * 2.  **响应日志记录**: 记录响应的URL、HTTP状态码、协议、响应消息以及响应体内容。
 * 3.  **内容类型过滤**: 能够判断响应体的 `MediaType`，只打印文本类型（如 `text/*`, `json`, `xml`, `html`）的内容，避免打印二进制文件（如图片、文件）等过大的数据，防止日志刷屏和性能问题。
 * 4.  **可配置性**: 构造函数允许传入一个 `tag` 用于日志过滤，并且可以通过 `showResponse` 参数控制是否打印响应体，提供了灵活性。
 * 5.  **无侵入式克隆**: 在读取响应体时，会先克隆（clone）`Response` 对象，读取完内容后再用原始内容重新构建一个新的 `ResponseBody` 放回响应中，这样可以确保原始的响应流不会被消费掉，后续的拦截器或调用者仍然可以正常读取响应数据。
 *
 * How: 核心技术
 * 1.  **`okhttp3.Interceptor` 接口**: 实现了 `Interceptor` 接口，这是 OkHttp 提供的一个强大的机制，允许开发者在网络请求的执行过程中插入自定义的逻辑。`intercept(Chain chain)` 方法是核心，它接收一个 `Chain` 对象，通过 `chain.request()` 获取请求，通过 `chain.proceed(request)` 将请求传递给下一个拦截器或服务器，并最终获取响应。
 * 2.  **`Request` 和 `Response` 对象操作**: 通过访问 `Request` 和 `Response` 对象，可以获取到所有关于请求和响应的元数据，包括 URL、头部、方法、协议和状态码等。
 * 3.  **`RequestBody` 和 `ResponseBody` 处理**: 
 *     -   对于 `RequestBody`，通过 `bodyToString` 方法，将请求体的内容读取为字符串。这里利用 `okio.Buffer` 将 `RequestBody` 的内容写入缓冲区，然后再从缓冲区读取为 UTF-8 字符串。
 *     -   对于 `ResponseBody`，为了避免读取后流被关闭，拦截器首先克隆了 `Response` 对象。然后从克隆的响应中读取 `body().string()`。读取后，再用 `ResponseBody.create(mediaType, resp)` 创建一个新的 `ResponseBody`，并用它构建一个新的 `Response` 对象返回。这是处理响应体日志记录的关键技巧，保证了响应流的完整性。
 * 4.  **`MediaType` 判断**: 通过 `isText(MediaType mediaType)` 方法，检查 `MediaType` 的 `type()` 和 `subtype()`，判断响应内容是否为文本格式，从而决定是否打印响应体，避免了处理和打印大量二进制数据带来的性能开销和日志混乱。
 */
public class LoggerInterceptor implements Interceptor {
    public static final String TAG = "OkHttpUtils";
    private String tag;
    private boolean showResponse;

    public LoggerInterceptor(String tag, boolean showResponse) {
        if (TextUtils.isEmpty(tag)) {
            tag = TAG;
        }
        this.showResponse = showResponse;
        this.tag = tag;
    }

    public LoggerInterceptor(String tag) {
        this(tag, false);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        logForRequest(request);
        Response response = chain.proceed(request);
        return logForResponse(response);
    }

    private Response logForResponse(Response response) {
        try {
            //===>response log
            Log.e(tag, "========response'log=======");
            Response.Builder builder = response.newBuilder();
            Response clone = builder.build();
            Log.e(tag, "url : " + clone.request().url());
            Log.e(tag, "code : " + clone.code());
            Log.e(tag, "protocol : " + clone.protocol());
            if (!TextUtils.isEmpty(clone.message()))
                Log.e(tag, "message : " + clone.message());

            if (showResponse) {
                ResponseBody body = clone.body();
                if (body != null) {
                    MediaType mediaType = body.contentType();
                    if (mediaType != null) {
                        Log.e(tag, "responseBody's contentType : " + mediaType.toString());
                        if (isText(mediaType)) {
                            String resp = body.string();
                            Log.e(tag, "responseBody's content : " + resp);

                            body = ResponseBody.create(mediaType, resp);
                            return response.newBuilder().body(body).build();
                        } else {
                            Log.e(tag, "responseBody's content : " + " maybe [file part] , too large too print , ignored!");
                        }
                    }
                }
            }

            Log.e(tag, "========response'log=======end");
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return response;
    }

    private void logForRequest(Request request) {
        try {
            String url = request.url().toString();
            Headers headers = request.headers();
            Log.e(tag, "========request'log=======");
            Log.e(tag, "method : " + request.method());
            Log.e(tag, "url : " + url);
            if (headers != null && headers.size() > 0) {
                Log.e(tag, "headers : " + headers.toString());
            }
            RequestBody requestBody = request.body();
            if (requestBody != null) {
                MediaType mediaType = requestBody.contentType();
                if (mediaType != null) {
                    Log.e(tag, "requestBody's contentType : " + mediaType.toString());
                    if (isText(mediaType)) {
                        Log.e(tag, "requestBody's content : " + bodyToString(request));
                    } else {
                        Log.e(tag, "requestBody's content : " + " maybe [file part] , too large too print , ignored!");
                    }
                }
            }
            Log.e(tag, "========request'log=======end");
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    private boolean isText(MediaType mediaType) {
        if (mediaType.type() != null && mediaType.type().equals("text")) {
            return true;
        }
        if (mediaType.subtype() != null) {
            if (mediaType.subtype().equals("json") ||
                    mediaType.subtype().equals("xml") ||
                    mediaType.subtype().equals("html") ||
                    mediaType.subtype().equals("webviewhtml")
                    )
                return true;
        }
        return false;
    }

    private String bodyToString(final Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "something error when show requestBody.";
        }
    }
}
