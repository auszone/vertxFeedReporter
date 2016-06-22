import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.impl.verticle.PackageHelper;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;


import java.util.Base64;

/**
 * Created by akuo on 6/8/16.
 */
public class Reporter {

  private final static String host = "localhost";
  private final static String feedURI = "/hawkular/inventory/feeds";
  private final static String feedId = "vertx-localhost";
  private final static String resourceURI = "/hawkular/inventory/feeds/" + feedId + "/resources";
  private final static String vertxResourceId = "vertx";
  private final static String metricTypeURI = "/hawkular/inventory/feeds/" + feedId + "/metricTypes";
  private final static String eventBusResourceId = "event-bus";
  private final static String metricURI = resourceURI + "/" + eventBusResourceId + "/metrics";
  private final static String metricId = "registered-handlers";
  private final static String metricTypeId = "something-countable";
  private HttpClient client;
  private Vertx vertx;

  public static void main(String [] argv) {
    Reporter reporter = new Reporter();
    try {
      // step 1: create a feed
      HttpClientRequest request = reporter.getHttpRequest(host, feedURI, HttpMethod.POST).handler(response -> {
        System.out.println("Created feed!");
        // step 2: create a vertx resource
        HttpClientRequest request1 = reporter.getHttpRequest(host, resourceURI, HttpMethod.POST).handler(response1 -> {
          System.out.println("Created vertx resource!");
          // step 3: create a event bus resource
          HttpClientRequest request2 = reporter.getHttpRequest(host, resourceURI, HttpMethod.POST).handler(response2 -> {
            System.out.println("Created event_bus resource!");
            // step 4: create a metric type
            HttpClientRequest request3 = reporter.getHttpRequest(host, metricTypeURI, HttpMethod.POST).handler(response3 -> {
              System.out.println("Created metric type COUNTER!");
              // step 5: create a metric
              HttpClientRequest request4 = reporter.getHttpRequest(host, metricURI, HttpMethod.POST).handler(response4 -> {
                System.out.println("Created metric " + metricId);
                reporter.vertx.close();
              });
              JsonObject json4 = new JsonObject().put("id", metricId).put("metricTypePath", "/mt;" + metricTypeId);
              request4.end(json4.encode());
            });
            JsonObject json3 = new JsonObject().put("id", metricTypeId).put("type", "COUNTER").put("unit", "NONE").put("collectionInterval", "30");
            request3.end(json3.encode());
          });
          JsonObject json2 = new JsonObject().put("id", eventBusResourceId).put("resourceTypePath", "/rt;URL");
          request2.end(json2.encode());
        });
        JsonObject json1 = new JsonObject().put("id", vertxResourceId).put("resourceTypePath", "/rt;URL")
                .put("properties", new JsonObject().put("type", "standalone"));
        request1.end(json1.encode());
      });

      JsonObject json = new JsonObject().put("id", feedId);
      request.end(json.encode());
    } catch (Exception ex) {
      System.err.println(ex.getLocalizedMessage());
    }
  }

  public Reporter() {
    vertx = Vertx.vertx();
    HttpClientOptions options = new HttpClientOptions().setDefaultHost(host).setDefaultPort(8080).setKeepAlive(false);
    client = vertx.createHttpClient(options);
  }

  private void addHeaders(HttpClientRequest request) {
    request.putHeader("Authorization", "Basic " + getAuthString("jdoe", "password"));
    request.putHeader("Content-Type", "application/json");
    request.putHeader("Hawkular-Tenant", "hawkular");
  }

  private HttpClientRequest getHttpRequest(String host, String URI, HttpMethod action) {
    HttpClientOptions options = new HttpClientOptions().setDefaultHost(host).setDefaultPort(8080).setKeepAlive(false);
    if (client == null) client = vertx.createHttpClient(options);
    HttpClientRequest request = client.request(action, URI);
    addHeaders(request);
    return request;
  }

  private String getAuthString(String username, String passwd) {
    String s = username + ":" + passwd;
    return Base64.getEncoder().encodeToString(s.getBytes());
  }
}
