import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;


import java.util.Base64;

/**
 * Created by akuo on 6/8/16.
 */
public class Reporter {

  private final static String host = "localhost";
  private final static String inventoryBaseURI = "/hawkular/inventory/deprecated/";
  private final static String feedURI = inventoryBaseURI + "feeds/";
  private final static String feedId = "vertx-localhost";
  private final static String resourceURI =  feedURI + feedId + "/resources/";
  private final static String resourceTypeURI =  feedURI + feedId + "/resourceTypes/";
  private final static String vertxResourceId = "vertx";
  private final static String metricTypeURI = feedURI + feedId + "/metricTypes/";
  private final static String eventBusResourceId = "event-bus";
  private final static String metricURI = resourceURI + eventBusResourceId + "/metrics/";
  private final static String metricId = "registered-handlers";
  private final static String metricTypeId = "something-countable";
  private HttpClient client;
  private Vertx vertx;

  public static void main(String [] argv) {
    Reporter reporter = new Reporter();
    try {
      Future<HttpClientResponse> fut = Future.future();
      Future<HttpClientResponse> fut1 = Future.future();
      Future<HttpClientResponse> fut2 = Future.future();
      Future<HttpClientResponse> fut3 = Future.future();
      Future<HttpClientResponse> fut4 = Future.future();
      Future<HttpClientResponse> fut5 = Future.future();

      HttpClientRequest request = reporter.getHttpRequest(host, feedURI, HttpMethod.POST).handler(response -> {
        if (response.statusCode() == 201) {
          System.out.println("Created feed!");
          fut.complete();
        } else {
          fut.fail("Fail when creating feed");
        }
      });
      JsonObject json = new JsonObject().put("id", feedId);
      request.end(json.encode());

      fut.compose(response -> {
        // step 1: create customized resource type
        HttpClientRequest request1 = reporter.getHttpRequest(host, resourceTypeURI, HttpMethod.POST).handler(response1 -> {
          if (response1.statusCode() == 201) {
            System.out.println("Created customized resource type");
            fut1.complete();
          } else {
            response1.bodyHandler(buffer -> {
              System.err.println(buffer.getBuffer(0, buffer.length()));
            });
            fut1.fail("Fail when creating resource type");
          }
        });
        JsonObject json1 = new JsonObject().put("id", "MYRT");
        request1.end(json1.encode());
      }, fut1);

      fut1.compose(response -> {
        // step 2: create a vertx resource
        HttpClientRequest request1 = reporter.getHttpRequest(host, resourceURI, HttpMethod.POST).handler(response1 -> {
          System.out.println("Created vertx resource!");
          fut2.complete();
        });
        JsonObject json1 = new JsonObject().put("id", vertxResourceId).put("resourceTypePath", "/rt;MYRT")
                .put("properties", new JsonObject().put("type", "standalone"));
        request1.end(json1.encode());
      }, fut2);

      fut2.compose(response -> {
        // step 3: create a event bus resource
        HttpClientRequest request2 = reporter.getHttpRequest(host, resourceURI, HttpMethod.POST).handler(response2 -> {
          System.out.println("Created event_bus resource!");
          fut3.complete();
        });
        JsonObject json2 = new JsonObject().put("id", eventBusResourceId).put("resourceTypePath", "/rt;MYRT");
        request2.end(json2.encode());
      }, fut3);

      fut3.compose(response -> {
        // step 4: create a metric type
        HttpClientRequest request3 = reporter.getHttpRequest(host, metricTypeURI, HttpMethod.POST).handler(response3 -> {
          System.out.println("Created metric type COUNTER!");
          fut4.complete();
        });
        JsonObject json3 = new JsonObject().put("id", metricTypeId).put("type", "COUNTER").put("unit", "NONE").put("collectionInterval", "30");
        request3.end(json3.encode());
      }, fut4);

      fut4.compose(response -> {
        // step 5: create a metric
        HttpClientRequest request4 = reporter.getHttpRequest(host, metricURI, HttpMethod.POST).handler(response4 -> {
          System.out.println("Created metric " + metricId);
          fut5.complete();
        });
        JsonObject json4 = new JsonObject().put("id", metricId).put("metricTypePath", "/mt;" + metricTypeId);
        request4.end(json4.encode());
      }, fut5);

      fut5.setHandler(httpClientResponseAsyncResult -> {
        reporter.vertx.close();
      });

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
    HttpClientRequest request = client.request(action, URI).exceptionHandler(throwable -> {
      System.err.println(throwable.getLocalizedMessage());
      vertx.close();
    });
    addHeaders(request);
    return request;
  }

  private String getAuthString(String username, String passwd) {
    String s = username + ":" + passwd;
    return Base64.getEncoder().encodeToString(s.getBytes());
  }
}
