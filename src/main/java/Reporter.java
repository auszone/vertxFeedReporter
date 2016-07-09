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
      Future<Void> fut0 = Future.future();
      Future<Void> fut1 = Future.future();
      Future<Void> fut2 = Future.future();
      Future<Void> fut3 = Future.future();
      Future<Void> fut4 = Future.future();
      Future<Void> fut5 = Future.future();
      // step 0: create feed
      HttpClientRequest request = reporter.getHttpRequest(host, feedURI, HttpMethod.POST).handler(resp -> {
        if (resp.statusCode() == 201) {
          System.out.println("Created feed!");
          fut0.complete();
        } else {
          fut0.fail("Fail when creating feed");
        }
      });
      request.end(new JsonObject().put("id", feedId).encode());

      fut0.compose(Void -> {
        // step 1: create customized resource type
        HttpClientRequest req = reporter.getHttpRequest(host, resourceTypeURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created customized resource type");
            fut1.complete();
          } else {
            fut1.fail("Fail when creating resource type");
          }
        });
        JsonObject json = new JsonObject().put("id", "MYRT");
        req.end(json.encode());
      }, fut1);

      fut1.compose(Void -> {
        // step 2: create a vertx resource
        HttpClientRequest req = reporter.getHttpRequest(host, resourceURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created vertx resource!");
            fut2.complete();
          } else {
            fut2.fail("Fail when creating resource vertx");
          }
        });
        JsonObject json = new JsonObject().put("id", vertxResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;MYRT")
                .put("properties", new JsonObject().put("type", "standalone"));
        req.end(json.encode());
      }, fut2);

      fut2.compose(Void -> {
        // step 3: create a event bus resource
        HttpClientRequest req = reporter.getHttpRequest(host, resourceURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created event_bus resource!");
            fut3.complete();
          } else {
            fut3.fail("Fail when creating resource event bus");
          }
        });
        JsonObject json = new JsonObject().put("id", eventBusResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;MYRT");
        req.end(json.encode());
      }, fut3);

      fut3.compose(Void -> {
        // step 4: create a metric type
        HttpClientRequest req = reporter.getHttpRequest(host, metricTypeURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created metric type COUNTER!");
            fut4.complete();
          } else {
            fut4.fail("Fail when metric type COUNTER");
          }
        });
        JsonObject json = new JsonObject().put("id", metricTypeId).put("type", "COUNTER").put("unit", "NONE").put("collectionInterval", "30");
        req.end(json.encode());
      }, fut4);

      fut4.compose(Void -> {
        // step 5: create a metric
        HttpClientRequest req = reporter.getHttpRequest(host, metricURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created metric " + metricId);
            fut5.complete();
          } else {
            fut5.fail("Fail when creating metic " + metricId);
          }
        });
        JsonObject json = new JsonObject().put("id", metricId).put("metricTypePath", "/mt;" + metricTypeId);
        req.end(json.encode());
      }, fut5);

      fut5.setHandler(httpClientResponseAsyncResult -> {
        if (httpClientResponseAsyncResult.failed()) {
          System.err.println(httpClientResponseAsyncResult.cause());
          reporter.deleteFeed(feedId);
        }
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

  private void deleteFeed(String feedId) {
    getHttpRequest(host, feedURI + feedId, HttpMethod.DELETE).handler(resp -> {
      System.err.println("Deleted feed " + feedId);
      vertx.close();
    }).end();
  }
}
