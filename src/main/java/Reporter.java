import io.netty.handler.codec.base64.Base64Encoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import java.util.Base64;

/**
 * Created by akuo on 6/8/16.
 */
public class Reporter {

  private final static String feedURI = "/hawkular/inventory/feeds";
  private HttpClient client;
  private Vertx vertx;

  public static void main(String [] argv) {
    Reporter reporter = new Reporter();
    String feedId = String.valueOf(System.currentTimeMillis());
    reporter.reportFeed(feedId);
  }

  public Reporter() {
    vertx = Vertx.vertx();
  }

  public void reportFeed(String feedId) {
    HttpClient c = getHttpClient("localhost");
    HttpClientRequest request = c.request(HttpMethod.POST, feedURI, response -> {
      System.out.println("Received response with status code " + response.statusCode());
      System.out.println(response.bodyHandler(buffer -> {
        System.out.println(buffer.getString(0, buffer.length()));
      }));
      vertx.close();
    });
    request.putHeader("Authorization", "Basic " + getAuthString("jdoe", "password"));
    request.putHeader("Content-Type", "application/json");
    JsonObject json = new JsonObject().put("id", feedId);
    request.end(json.encode());
  }

  private HttpClient getHttpClient(String host) {
    HttpClientOptions options = new HttpClientOptions().setDefaultHost(host).setDefaultPort(8080);
    return vertx.createHttpClient(options);
  }

  private String getAuthString(String username, String passwd) {
    String s = username + ":" + passwd;
    return Base64.getEncoder().encodeToString(s.getBytes());
  }
}
