package com.yeezhao.wbcrawler.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yeezhao.wbcrawler.base.CrawlerConsts;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author congzicun
 * @since 2015-06-12 10:55 PM
 */
public class ChampsParser {
    Log LOG = LogFactory.getLog(ChampsParser.class);

    private DefaultHttpClient client;

    private String username;
    private String password;
    private String cookie;
    private String requestKey;

    public ChampsParser() {
        BasicCookieStore cookieStore = new BasicCookieStore();
        client = new DefaultHttpClient();
        client.setCookieStore(cookieStore);
        client.getParams().setParameter("http.protocol.cookie-policy",
                CookiePolicy.BROWSER_COMPATIBILITY);
        client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
        client.setRedirectStrategy(new LaxRedirectStrategy());
        Configuration conf = HBaseConfiguration.create();
        conf.addResource(CrawlerConsts.CRAWLER_CONF_FILE);
        username = conf.get(CrawlerConsts.CHAMPS_USERNAME_CONF);
        password = conf.get(CrawlerConsts.CHAMPS_PASSWORD_CONF);
        cookie = conf.get(CrawlerConsts.CHAMPS_COOKIE_CONF);
        requestKey = conf.get(CrawlerConsts.CHAMPS_REQKEY_CONF);
    }


    public boolean login() throws UnsupportedEncodingException {
        try {
            HttpPost post = new HttpPost(
                    "https://www.champssports.com/login/login_action.cfm?secured=false&bv_RR_enabled=false&bv_AA_enabled=false&bv_JS_enabled=true&ignorebv=false");
            post.setHeader("Host", "www.champssports.com");
            post.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:32.0) Gecko/20100101 Firefox/32.0");
            post.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            post.setHeader("Accept-Encoding", "gzip, deflate");
            post.setHeader("Cookie", cookie);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("login_email", username));
            nvps.add(new BasicNameValuePair("login_password", password));
            nvps.add(new BasicNameValuePair("requestKey", requestKey));
            nvps.add(new BasicNameValuePair("co_cd", "20"));


            post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            HttpResponse response = client.execute(post);
            System.out.println("Number of Cookies" + client.getCookieStore().getCookies().size());
            for (Cookie c : client.getCookieStore().getCookies()) {
                System.out.println(c.getName() + "=" + c.getValue());
            }
            System.out.println(response.getStatusLine().getStatusCode());
            post.releaseConnection();
            return response.getStatusLine().getStatusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkOut() throws IOException {
        String baseUrl = "https://www.champssports.com/checkout/";
        String url = String.format(baseUrl, requestKey);
        System.out.println(url);
        HttpGet getMethod = new HttpGet(url);
        getMethod.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:32.0) Gecko/20100101 Firefox/32.0");
        getMethod.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        getMethod.setHeader("Connection", "keep-alive");
        getMethod.setHeader("Cookie", cookie);
        HttpResponse response = client.execute(getMethod);
        String entity = EntityUtils.toString(response.getEntity());
        System.out.println(entity);
        return true;
    }
    public boolean addCart(String requestKey, String sku) throws UnsupportedEncodingException {
        try {
            String baseUrl = "http://www.champssports.com/shoppingcart/gateway?requestKey=%s&action=add&qty=1&sku=04401123&size=08.0&fulfillmentType=SHIP_TO_HOME&storeNumber=0&storeCostOfGoods=0&_=1435986994470";
            String url = String.format(baseUrl, requestKey);
            System.out.println(url);
            HttpGet getMethod = new HttpGet(url);
            getMethod.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:32.0) Gecko/20100101 Firefox/32.0");
            getMethod.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            getMethod.setHeader("Connection", "keep-alive");
            getMethod.setHeader("Cookie", cookie);
            HttpResponse response = client.execute(getMethod);
            String entity = EntityUtils.toString(response.getEntity());
            JsonParser jsonParser = new JsonParser();
            JsonObject jo = (JsonObject)jsonParser.parse(entity);
            System.out.println(entity);
            return jo.get("success").getAsBoolean();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getRequestKey() {
        String url = "http://www.champssports.com/shoppingcart/gateway?action=requestKey&_=1435986994189";
        HttpGet getMethod = new HttpGet(url);
        getMethod.setHeader("Cookie", cookie);

        HttpResponse response = null;
        try {
            response = client.execute(getMethod);
            String entity = EntityUtils.toString(response.getEntity());
            System.out.println(entity);
            JsonParser jsonParser = new JsonParser();
            JsonObject jo = (JsonObject)jsonParser.parse(entity);
            return jo.getAsJsonObject("data").get("RequestKey").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }


    public static void main(String[] args) throws Exception {
        // 创建 Options 对象

        ChampsParser webParser = new ChampsParser();
        if(webParser.login()) {
            System.out.println("Login success");
            while(true) {
                String requestKey = webParser.getRequestKey();
                if (requestKey != null) {
                    boolean ifAdd = webParser.addCart(requestKey, "");
                    if (ifAdd) {
                        System.out.println("Successfully added");
                        break;
                        //webParser.checkOut();
                    }else{
                        System.out.println("Failed added");
                    }
                    Thread.sleep(1000);
                }
            }
        }
    }
}

//    public String getFrontPage() throws IOException {
//        String url = "http://www.champssports.com/login/login_form.cfm?secured=false&status=success&errmsg=&bv_RR_enabled=false&bv_AA_enabled=false&bv_JS_enabled=true&ignorebv=false";
//        HttpGet getMethod = new HttpGet(url);
//        getMethod.setHeader("Host", "www.champssports.com");
//        getMethod.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:32.0) Gecko/20100101 Firefox/32.0");
//        getMethod.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
//        getMethod.setHeader("Accept-Encoding", "gzip, deflate");
//        getMethod.setHeader("Connection", "keep-alive");
//
//        HttpResponse response = client.execute(getMethod);
//        System.out.println("Number of Cookies" + client.getCookieStore().getCookies().size());
//
//        String entity = EntityUtils.toString(response.getEntity());
//        Document doc = Jsoup.parse(entity);
//        Elements links = doc.getElementsByTag("form");
//        String requestKey = "";
//        for (Element link : links) {
//            Elements e = link.select("input#requestKey");
//            requestKey = e.attr("value");
//            System.out.println(e);
//            System.out.println(requestKey + "#");
//        }
//        getMethod.releaseConnection();
//        return requestKey;
//
//    }
