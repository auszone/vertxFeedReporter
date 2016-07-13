import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import java.util.Base64;

/**
 * Created by akuo on 6/8/16.
 */
public class Reporter {

  private final static String host = "localhost";
  private final static String tenant = "hawkular";
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
      Future<Void> feedFut = Future.future();
      Future<Void> vertxRTFut = Future.future();
      Future<Void> vertxRsrFut = Future.future();
      Future<Void> eventBusRTFut = Future.future();
      Future<Void> eventBusRsrFut = Future.future();
      Future<Void> mTFut = Future.future();
      Future<Void> mtcFut = Future.future();
      Future<Void> assFut = Future.future();

      // step 0: create feed
      HttpClientRequest request = reporter.getHttpRequest(host, feedURI, HttpMethod.POST).handler(resp -> {
        if (resp.statusCode() == 201) {
          System.out.println("Created feed!");
          feedFut.complete();
        } else {
          feedFut.fail("Fail when creating feed");
        }
      });
      request.end(new JsonObject().put("id", feedId).encode());

      feedFut.compose(Void -> {
        // step 1: create vertx resource type
        HttpClientRequest req = reporter.getHttpRequest(host, resourceTypeURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created vertx resource type");
            vertxRTFut.complete();
          } else {
            vertxRTFut.fail("Fail when creating vertx resource type");
          }
        });
        JsonObject json = new JsonObject().put("id", "vertxRT");
        req.end(json.encode());
      }, vertxRTFut);

      vertxRTFut.compose(Void -> {
        // step 2: create a vertx resource
        HttpClientRequest req = reporter.getHttpRequest(host, resourceURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created vertx resource!");
            vertxRsrFut.complete();
          } else {
            vertxRsrFut.fail("Fail when creating resource vertx");
          }
        });
        JsonObject json = new JsonObject().put("id", vertxResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;vertxRT")
                .put("properties", new JsonObject().put("type", "standalone"));
        req.end(json.encode());
      }, vertxRsrFut);

      vertxRsrFut.compose(Void -> {
        // step 3: create a event bus resource type
        HttpClientRequest req = reporter.getHttpRequest(host, resourceTypeURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created event bus resource type");
            eventBusRTFut.complete();
          } else {
            eventBusRTFut.fail("Fail when creating event bus resource type");
          }
        });
        JsonObject json = new JsonObject().put("id", "eventBusRT");
        req.end(json.encode());
      }, eventBusRTFut);

      eventBusRTFut.compose(Void -> {
        // step 4: create a event bus resource
        HttpClientRequest req = reporter.getHttpRequest(host, resourceURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created event_bus resource!");
            eventBusRsrFut.complete();
          } else {
            eventBusRsrFut.fail("Fail when creating resource event bus");
          }
        });
        JsonObject json = new JsonObject().put("id", eventBusResourceId).put("resourceTypePath", "/f;" + feedId + "/rt;eventBusRT");
        req.end(json.encode());
      }, eventBusRsrFut);

      eventBusRsrFut.compose(Void -> {
        // step 5: create a metric type
        HttpClientRequest req = reporter.getHttpRequest(host, metricTypeURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created metric type COUNTER!");
            mTFut.complete();
          } else {
            mTFut.fail("Fail when metric type COUNTER");
          }
        });
        JsonObject json = new JsonObject().put("id", metricTypeId).put("type", "COUNTER").put("unit", "NONE").put("collectionInterval", 30);
        req.end(json.encode());
      }, mTFut);

      mTFut.compose(Void -> {
        // step 6: associate the metric type with event bus resource type
        HttpClientRequest req = reporter.getHttpRequest(host, resourceTypeURI+"eventBusRT/metricTypes", HttpMethod.POST)
                .handler(resp -> {
          if (resp.statusCode() == 204) {
            System.out.println("Associate metric type!");
            assFut.complete();
          } else {
            resp.handler(buffer -> {
              System.err.println(buffer.getBuffer(0,buffer.length()));
            });
            assFut.fail("Fail when associating  metric type COUNTER");
          }
        });
        String metricPath = String.format("/t;%s/f;%s/mt;%s", tenant, feedId, metricTypeId);
        JsonArray json = new JsonArray().add(metricPath);
        req.end(json.encode());
      }, assFut);

      assFut.compose(Void -> {
        // step 7: create a metric
        HttpClientRequest req = reporter.getHttpRequest(host, metricURI, HttpMethod.POST).handler(resp -> {
          if (resp.statusCode() == 201) {
            System.out.println("Created metric " + metricId);
            mtcFut.complete();
          } else {
            mtcFut.fail("Fail when creating metic " + metricId);
          }
        });
        JsonObject json = new JsonObject().put("id", metricId).put("metricTypePath", "/mt;" + metricTypeId);
        req.end(json.encode());
      }, mtcFut);

      mtcFut.setHandler(httpClientResponseAsyncResult -> {
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
