package com.github.monkeywie.proxyee.crawler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.monkeywie.proxyee.download.DownloadDouyinVideo;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.CertDownIntercept;
import com.github.monkeywie.proxyee.intercept.common.FullRequestIntercept;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import com.github.monkeywie.proxyee.util.FileUtil;
import com.github.monkeywie.proxyee.util.HttpUtil;
import com.github.monkeywie.proxyee.util.RedisUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.io.IOException;
import java.nio.charset.Charset;

public class Douyin {

    public static void main(String[] args) {
        new Thread(Douyin::proxyFun).start();
        //这个数字是下载线程数量，数字越大下载越快，最小得1
        int count = 1;
        for (int i = 0; i < count; i++) {
            new Thread(DownloadDouyinVideo::downloadFun).start();
        }
    }

    public static void proxyFun(){
        HttpProxyServerConfig config = new HttpProxyServerConfig();
        config.setHandleSsl(true);
        new HttpProxyServer()
                .serverConfig(config)
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        pipeline.addLast(new CertDownIntercept());
                        pipeline.addLast(new FullResponseIntercept() {

                            @Override
                            public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                //喜欢
                                String model1 = "/aweme/v1/aweme/favorite/";

                                String [] models = {
                                        //喜欢
                                        "/aweme/v1/aweme/favorite/",
                                        //评论
                                        ""
                                };
                                String uri = pipeline.getHttpRequest().uri();

                                if(uri.contains(model1)){
                                    return true;
                                }
                                return false;
                            }

                            @Override
                            public void handelResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                String content = httpResponse.content().toString(Charset.defaultCharset());
                                JSONObject json = JSONObject.parseObject(content);
                                System.out.println(json);
                                JSONArray awemeList = json.getJSONArray("aweme_list");
                                for (Object o : awemeList) {
                                    JSONObject aweme = JSONObject.parseObject(o.toString());
                                    String id = aweme.getString("aweme_id");
                                    String desc = aweme.getString("desc");
                                    String url = null;
                                    try {
                                        url = aweme.getJSONObject("video").getJSONObject("play_addr").getJSONArray("url_list").get(0).toString();
                                    }catch (Exception e){
                                        continue;
                                    }
                                    RedisUtil.getRedisUtil().lpush("douyin_url",id+"###"+url);
                                    String result = id + "," + desc + "," + url;
                                    try {
                                        FileUtil.writeLine(result);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }


                            }
                        });
                    }
                })
                .start(9999);
    }

}
