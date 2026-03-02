package com.leehuang.his.api.config.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Description: 防止 XSS 攻击
 *      XSS：XSS（Cross-Site Scripting，跨站脚本攻击） 是一种常见的网络安全漏洞。攻击者利用网站对用户输入过滤不足的弱点，
 *           将恶意的脚本代码注入到网页中，通过各种手段向 Web 网站植入 JS 代码并让其他用户的浏览器执行这些代码。
 * 解决方法：XSS 内容转义
 *         创建一个执行转义的封装类，并继承 HttpServletRequestWrapper 类，
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    /**
     * 把原始请求对象包一层，后续所有方法调用都会先经过这里的逻辑，再决定是否继续传给真正的请求对象。
     * @param request
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 获取请求中指定的参数
     * @param name      参数名
     * @return          处理后的参数值
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if(!StrUtil.hasEmpty(value)){
            // cleanHtmlTag：将传入字符串中的 HTML 标签 script 清除
            value=HtmlUtil.cleanHtmlTag(value);
        }
        return value;
    }

    /**
     * 处理复选框、同名多值字段（如 hobby=swim&hobby=run）。
     * @param name      参数名
     * @return          处理后的同名参数值数组
     */
    @Override
    public String[] getParameterValues(String name) {
        // 1. 获取原始参数值数组（如复选框多选情况）
        String[] values = super.getParameterValues(name);
        // 2. 对每个非空值进行 XSS 清理
        if(values!=null){
            for (int i = 0;i < values.length;i++){
                String value = values[i];
                if(!StrUtil.hasEmpty(value)){
                    value=HtmlUtil.cleanHtmlTag(value);
                }
                // 3. 将处理完成后的参数值放回到数组中
                values[i] = value;
            }
        }
        return values;
    }

    /**
     * 获取所有请求参数的映射并进行批量 XSS 过滤
     * @return  处理后的参数映射值
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        // 1. 获取原始参数映射（键为参数名，值为参数值数组）
        Map<String, String[]> parameters = super.getParameterMap();
        // 2. 创建一个新的有序参数映射，用于存放过滤后的参数值
        LinkedHashMap<String, String[]> map = new LinkedHashMap<>();
        // 3. 每个参数值数组中的每个值进行 XSS 清理
        if(parameters != null){
            // 3.1 遍历每个参数 key 对应的参数值数组
            for (String key:parameters.keySet()){
                // 3.2 获取当前参数 key 对应的参数值数组
                String[] values = parameters.get(key);
                // 3.3 对数组中每个非空值进行 XSS 清理
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (!StrUtil.hasEmpty(value)) {
                        value = HtmlUtil.cleanHtmlTag(value);
                    }
                    // 3.4 将处理完成后的参数值放回到数组中中
                    values[i] = value;
                }
                // 3.5 将处理完成后的参数值数组放回到新的有序参数映射中
                map.put(key,values);
            }
        }
        return map;
    }

    /**
     * 获取 HTTP 请求头并进行 XSS 过滤（如自定义的请求头 X-Forwarded-For: <script>…</script>）
     * @param name      请求头名
     * @return          处理后的请求头值
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (!StrUtil.hasEmpty(value)) {
            value = HtmlUtil.cleanHtmlTag(value);
        }
        return value;
    }

    /**
     * 处理 JSON 格式请求体，对其中的字符串值进行 XSS 过滤
     * @return
     * @throws IOException
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 1. 读取原始请求体输入流
        InputStream in = super.getInputStream();

        // 2. 将输入流转换为字符串（body内容）
        // InputStreamReader reader=new InputStreamReader(in, Charset.forName("UTF-8"));
        // 2.1 创建字符读取器，将字节流转换为字符流，并指定 UTF-8 编码（HTTP 请求体本质上是字节流，需要转换为可读的字符数据）
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        // 2.2 创建缓冲读取器，为字符流添加缓冲功能，提高读取效率（减少 I/O 操作次数，提升处理速度），支持按行读取（readLine()方法）
        BufferedReader buffer = new BufferedReader(reader);
        // 2.3 准备存储请求体，用于累积整个请求体内容
        StringBuffer body = new StringBuffer();

        // 2.4 按行读取请求体内容，直到读取完毕
        // 2.4.1 先读取一行
        String line = buffer.readLine();
        // 2.4.2 循环读取每一行，直到读取完毕
        while(line != null){
            body.append(line);
            line = buffer.readLine();
        }
        // 2.5 关闭字符读取器和缓冲读取器，释放资源
        buffer.close();
        reader.close();
        in.close();

        // 3. 对 JSON 进行 XSS 清理
        // 3.1 将 JSON 字符串解析为 Map 对象
        Map<String,Object> map = JSONUtil.parseObj(body.toString());
        // 3.2 创建新的有序 Map 存储清理后的数据
        Map<String,Object> result = new LinkedHashMap<>();
        // 3.3 遍历原始 JSON 数据的每个键值对
        for(String key:map.keySet()){
            Object val = map.get(key);
            // 3.4 对字符串类型的值进行 XSS 清理，将非字符串值（如数字、布尔值）原样保留
            if(val instanceof String){
                if(!StrUtil.hasEmpty(val.toString())){
                    // 3.4.1 将处理完成后的值放入到新的 Map 中
                    result.put(key,HtmlUtil.cleanHtmlTag(val.toString()));
                }
            }
            else {
                // 3.4.2 非字符串值原样保留
                result.put(key,val);
            }
        }
        // 3.5 重新序列化 JSON，将清理后的 Map 转换回 JSON 字符串，确保返回的格式与原始请求体格式一致
        String json=JSONUtil.toJsonStr(result);
        // 4. 创建新的输入流，将清理后的 JSON 字符串包装为新的字节流
        ByteArrayInputStream bain=new ByteArrayInputStream(json.getBytes());
        // 5. 返回自定义的 ServletInputStream 输入流实现
        return new ServletInputStream() {
            // 应用程序调用 read() 方法时，实际读取的是清理后的数据
            @Override
            public int read() throws IOException {
                return bain.read();
            }

            // isFinished、isReady、setReadListener：实现 ServletInputStream 的必要抽象方法，此场景下不支持异步读取，因此提供最简实现
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}
