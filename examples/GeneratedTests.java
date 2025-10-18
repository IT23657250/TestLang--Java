import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.http.*; import java.net.*; import java.time.Duration;
import java.nio.charset.StandardCharsets; import java.util.*;

public class GeneratedTests {
  static String BASE = "http://localhost:8080";
  static Map<String,String> DEFAULT_HEADERS = new LinkedHashMap<>();
  static HttpClient client;

  @BeforeAll
  static void setup() {
    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    DEFAULT_HEADERS.put("Content-Type","application/json");
    DEFAULT_HEADERS.put("X-App","TestLangDemo");
  }

  @Test
  void test_Login() throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + "/api/login"))
      .timeout(Duration.ofSeconds(10))
      .POST(HttpRequest.BodyPublishers.ofString("{ \"username\": \"admin\", \"password\": \"1234\" }"));
    for (var e: DEFAULT_HEADERS.entrySet()) b.header(e.getKey(), e.getValue());
    HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    assertEquals(200, resp.statusCode());
    assertTrue(resp.headers().firstValue("Content-Type").orElse("").contains("json"));
    assertTrue(resp.body().contains("\"token\":"));
  }

  @Test
  void test_GetUser() throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + "/api/users/42"))
      .timeout(Duration.ofSeconds(10))
      .GET();
    for (var e: DEFAULT_HEADERS.entrySet()) b.header(e.getKey(), e.getValue());
    HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    assertEquals(200, resp.statusCode());
    assertTrue(resp.body().contains("\"id\": 42"));
  }

  @Test
  void test_UpdateUserRole() throws Exception {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + "/api/users/42"))
      .timeout(Duration.ofSeconds(10))
      .PUT(HttpRequest.BodyPublishers.ofString("{ \"role\": \"ADMIN\" }"));
    for (var e: DEFAULT_HEADERS.entrySet()) b.header(e.getKey(), e.getValue());
    b.setHeader("Content-Type","application/json");
    HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    assertEquals(200, resp.statusCode());
    assertEquals("TestLangDemo", resp.headers().firstValue("X-App").orElse(""));
    assertTrue(resp.headers().firstValue("Content-Type").orElse("").contains("json"));
    assertTrue(resp.body().contains("\"updated\": true"));
    assertTrue(resp.body().contains("\"role\": \"ADMIN\""));
  }
}