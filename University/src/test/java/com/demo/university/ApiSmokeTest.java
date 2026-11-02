package com.demo.university;

import com.demo.university.common.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接口冒烟测试：用嵌入式 Tomcat 真正把 war 的目录结构跑起来，
 * 验证「Jersey 路由 / 登录拦截 / Jackson 序列化 / 会话 / DAO 事务」整条链路。
 * 数据库使用 H2 内存库，无需安装 MySQL。
 */
class ApiSmokeTest {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static Tomcat tomcat;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws Exception {
        System.setProperty("db.url",
                "jdbc:h2:mem:smoke_api;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        System.setProperty("db.driver", "org.h2.Driver");
        System.setProperty("db.username", "sa");
        System.setProperty("db.password", "");
        System.setProperty("db.autoInit", "true");

        tomcat = new Tomcat();
        tomcat.setBaseDir(new File("target/tomcat-smoke").getAbsolutePath());
        tomcat.setPort(0);
        tomcat.getConnector();

        StandardContext context = (StandardContext) tomcat.addWebapp("",
                new File("src/main/webapp").getAbsolutePath());
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                new File("target/classes").getAbsolutePath(), "/"));
        context.setResources(resources);

        tomcat.start();
        baseUrl = "http://localhost:" + tomcat.getConnector().getLocalPort();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
        System.clearProperty("db.url");
        System.clearProperty("db.driver");
        System.clearProperty("db.username");
        System.clearProperty("db.password");
        System.clearProperty("db.autoInit");
    }

    @Test
    void pingReportsDatabaseConnected() throws Exception {
        Response response = get("/api/ping", null);
        assertEquals(200, response.status);
        JsonNode data = json(response.body).path("data");
        assertTrue(data.path("db").path("connected").asBoolean(), "数据库应连通：" + response.body);
        assertTrue(data.path("db").path("users").asLong() >= 1, "应已创建默认管理员");
    }

    @Test
    void protectedApiRequiresLogin() throws Exception {
        Response response = get("/api/books/mine", null);
        assertEquals(401, response.status, "未登录访问我的发布应返回 401");
        assertEquals(401, json(response.body).path("code").asInt());
    }

    @Test
    void publicBookSearchWorks() throws Exception {
        Response response = get("/api/books?page=1&size=5", null);
        assertEquals(200, response.status);
        JsonNode data = json(response.body).path("data");
        assertTrue(data.path("list").isArray(), "应返回分页列表");
        assertTrue(data.has("total"));
    }

    @Test
    void fullTradeFlowOverHttp() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String sellerCookie = register("seller" + suffix).cookie;
        String buyerCookie = register("buyer" + suffix).cookie;
        assertNotNull(sellerCookie, "注册后应返回会话 Cookie");

        // 卖家发布教材
        String title = "接口测试教材" + suffix;
        Response created = post("/api/books", "{\"title\":\"" + title + "\",\"categoryId\":1,"
                + "\"conditionLevel\":2,\"price\":12.50,\"stock\":1,\"description\":\"九成新\"}", sellerCookie);
        assertEquals(200, created.status, created.body);
        long bookId = json(created.body).path("data").path("id").asLong();
        assertTrue(bookId > 0);

        // 列表能搜到
        Response search = get("/api/books?q=" + title, null);
        assertEquals(1, json(search.body).path("data").path("total").asInt(), "关键词搜索应命中 1 条");

        // 详情 + 收藏
        assertEquals(200, get("/api/books/" + bookId, buyerCookie).status);
        assertEquals(200, post("/api/favorites/" + bookId, "{}", buyerCookie).status);
        assertEquals(1, json(get("/api/favorites", buyerCookie).body).path("data").path("list").size());

        // 买家下单 → 库存扣减
        Response order = post("/api/orders", "{\"bookId\":" + bookId + ",\"quantity\":1,"
                + "\"tradePlace\":\"图书馆\",\"contact\":\"13800000000\"}", buyerCookie);
        assertEquals(200, order.status, order.body);
        long orderId = json(order.body).path("data").path("id").asLong();

        JsonNode afterOrder = json(get("/api/books/" + bookId, null).body).path("data");
        assertEquals(0, afterOrder.path("stock").asInt(), "下单后库存应为 0");

        // 卖家不能替买家付款
        assertEquals(403, put("/api/orders/" + orderId + "/pay", "{}", sellerCookie).status);

        // 完整流转：付款 -> 交付 -> 收货
        assertEquals(200, put("/api/orders/" + orderId + "/pay", "{}", buyerCookie).status);
        assertEquals(200, put("/api/orders/" + orderId + "/deliver", "{}", sellerCookie).status);
        assertEquals(200, put("/api/orders/" + orderId + "/confirm", "{}", buyerCookie).status);

        // 评价
        Response review = post("/api/reviews", "{\"orderId\":" + orderId
                + ",\"rating\":5,\"content\":\"书况很好\"}", buyerCookie);
        assertEquals(200, review.status, review.body);
        assertEquals(1, json(get("/api/reviews?bookId=" + bookId, null).body).path("data").size());

        // 未登录不能看订单
        assertEquals(401, get("/api/orders", null).status);
    }

    @Test
    void wantedFlowOverHttp() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String ownerCookie = register("want" + suffix).cookie;
        String otherCookie = register("want2" + suffix).cookie;
        String title = "求购线性代数" + suffix;

        // 发布求购
        Response created = post("/api/wanted", "{\"title\":\"" + title + "\",\"categoryId\":1,"
                + "\"expectedPrice\":15,\"conditionLevel\":2,\"contact\":\"微信abc123\","
                + "\"description\":\"需要上册\"}", ownerCookie);
        assertEquals(200, created.status, created.body);
        long wantedId = json(created.body).path("data").path("id").asLong();
        assertTrue(wantedId > 0);

        // 未登录浏览：能看到求购，但联系方式不可见
        JsonNode page = json(get("/api/wanted?status=0", null).body).path("data");
        assertTrue(page.path("total").asInt() >= 1);
        JsonNode target = null;
        for (JsonNode item : page.path("list")) {
            if (item.path("id").asLong() == wantedId) {
                target = item;
            }
        }
        assertNotNull(target, "列表里应能搜到刚发布的求购");
        assertEquals(false, target.path("contactVisible").asBoolean(), "未登录不应返回联系方式");

        // 登录后可以看到联系方式
        JsonNode detail = json(get("/api/wanted/" + wantedId, otherCookie).body).path("data");
        assertEquals(true, detail.path("contactVisible").asBoolean());
        assertEquals("微信abc123", detail.path("contact").asText());

        // 不能操作他人的求购
        assertEquals(403, put("/api/wanted/" + wantedId + "/status", "{\"status\":1}", otherCookie).status);

        // 本人标记已买到
        assertEquals(200, put("/api/wanted/" + wantedId + "/status", "{\"status\":1}", ownerCookie).status);
        // 默认列表只显示求购中，已买到的不应出现
        JsonNode openList = json(get("/api/wanted?status=0&size=50", null).body).path("data");
        for (JsonNode item : openList.path("list")) {
            assertTrue(item.path("id").asLong() != wantedId, "已买到的求购不应出现在求购中列表");
        }

        // 我的求购（含已买到）
        assertEquals(1, json(get("/api/wanted/mine", ownerCookie).body).path("data").path("total").asInt());
        // 未登录不能看"我的求购"
        assertEquals(401, get("/api/wanted/mine", null).status);
    }

    /* ---------------- 工具方法 ---------------- */

    private record Response(int status, String body, String cookie) {
    }

    private static Response register(String username) throws Exception {
        return post("/api/auth/register",
                "{\"username\":\"" + username + "\",\"password\":\"123456\",\"nickname\":\"测试" + username + "\"}",
                null);
    }

    private static Response get(String path, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
        return send(builder, cookie);
    }

    private static Response post(String path, String body, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        return send(builder, cookie);
    }

    private static Response put(String path, String body, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));
        return send(builder, cookie);
    }

    private static Response send(HttpRequest.Builder builder, String cookie) throws Exception {
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        String session = null;
        for (String header : response.headers().allValues("Set-Cookie")) {
            if (header.startsWith("JSESSIONID=")) {
                session = header.split(";")[0];
            }
        }
        return new Response(response.statusCode(), response.body(), session);
    }

    private static JsonNode json(String body) throws Exception {
        return JsonUtil.mapper().readTree(body);
    }
}
