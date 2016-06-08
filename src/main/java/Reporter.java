import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;

/**
 * Created by akuo on 6/8/16.
 */
public class Reporter {
  public static void main(String [] argv) {
    Reporter reporter = new Reporter();
    // TODO : Generate feed ID
    String feedId = "autogenerate";
    reporter.reportFeed(feedId);
  }

  private void reportFeed(String feedId) {
    HttpClient c = getHttpClient("localhost");
    // TODO : http "post" to feed URI to report a vertx instance
  }

  private HttpClient getHttpClient(String host) {
    HttpClientOptions options = new HttpClientOptions().setDefaultHost(host);
    Vertx vertx = Vertx.vertx();
    return vertx.createHttpClient(options);
  }
}
