/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.jinsight.modules.servlet;

import static ai.apptuit.metrics.jinsight.modules.servlet.ContextMetricsHelper.X_REQUEST_ID_HEADERNAME;
import static ai.apptuit.metrics.jinsight.modules.servlet.BaseTestServlet.RUNTIME_REQUEST_ID_COOKIENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
abstract class AbstractWebServerTest {

  private CountTracker tracker;

  @Before
  public void setup() throws Exception {
    TagEncodedMetricName requestMetricRoot = getRootMetric().submetric("requests");
    tracker = new CountTracker(RegistryService.getMetricRegistry(), requestMetricRoot, "method",
        "status") {
      @Override
      public void validate(Snapshot snapshot) {
        waitForMetricsUpdate();
        super.validate(snapshot);
      }
    };

    tracker.registerTimer("GET", "200");
    tracker.registerTimer("GET", "500");
    tracker.registerTimer("POST", "200");
    tracker.registerTimer("POST", "500");
  }


  @Test
  public void testPingPong() throws IOException, InterruptedException {
    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("GET", "200");

    URL url = pathToURL(PingPongServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(PingPongServlet.PONG, getText(connection));

    tracker.validate(snapshot);
  }

  @Test
  public void testAsync() throws IOException, InterruptedException {
    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("GET", "200");

    String uuid = UUID.randomUUID().toString();
    URL url = pathToURL(AsyncServlet.PATH + "?" + AsyncServlet.UUID_PARAM + "=" + uuid);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(uuid, getText(connection));
    tracker.validate(snapshot);
  }

  @Test
  public void testAsyncWithError() throws IOException, InterruptedException {
    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("GET", "500");

    URL url = pathToURL(AsyncServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, connection.getResponseCode());
    tracker.validate(snapshot);
  }

  @Test
  public void testPost() throws IOException, InterruptedException {
    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("POST", "200");

    String content = UUID.randomUUID().toString();

    URL url = pathToURL(PingPongServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.getOutputStream().write(content.getBytes());
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(content, getText(connection));

    tracker.validate(snapshot);
  }


  @Test
  public void testExceptionResponse() throws IOException, InterruptedException {
    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("GET", "500");

    URL url = pathToURL(ExceptionServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, connection.getResponseCode());
    tracker.validate(snapshot);
  }

  @Test
  public void testRetainRequestIdHeader() throws IOException, InterruptedException {
    UUID requestID = UUID.randomUUID();

    URL url = pathToURL(PingPongServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty(X_REQUEST_ID_HEADERNAME, requestID.toString());
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(PingPongServlet.PONG, getText(connection));
    assertTrue(matchRequestIds(requestID, connection));
  }

  @Test
  public void testGenerateRequestIdHeader() throws IOException, InterruptedException {

    URL url = pathToURL(PingPongServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(PingPongServlet.PONG, getText(connection));
    assertTrue(matchRequestIds(null, connection));
  }

  @Test
  public void testRetainRequestIdOnInternalError() throws IOException, InterruptedException {
    UUID requestID = UUID.randomUUID();

    URL url = pathToURL(ExceptionServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty(X_REQUEST_ID_HEADERNAME, requestID.toString());
    connection.connect();

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, connection.getResponseCode());
    assertTrue(matchRequestIds(requestID, connection));
  }

  @Test
  public void testRequestIdOnInternalError() throws IOException, InterruptedException {
    URL url = pathToURL(ExceptionServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, connection.getResponseCode());
    assertTrue(matchRequestIds(null, connection));
  }

  @Test
  public void testRequestIdInAsync() throws IOException, InterruptedException {
    String uuid = UUID.randomUUID().toString();
    URL url = pathToURL(AsyncServlet.PATH + "?" + AsyncServlet.UUID_PARAM + "=" + uuid);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(uuid, getText(connection));
    assertTrue(matchRequestIds(null, connection));
  }

  @Test

  public void testRequestIdInAsyncWithError() throws IOException, InterruptedException {
    URL url = pathToURL(AsyncServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, connection.getResponseCode());
    assertTrue(matchRequestIds(null, connection));
  }

  private boolean matchRequestIds(UUID requestID, HttpURLConnection connection) {
    try {
      UUID uuidHeader = getRequestIdHeader(connection);
      if (requestID != null) {
        assertEquals(requestID, uuidHeader);
      } else {
        assertNotNull(uuidHeader);
      }
      assertEquals(uuidHeader, getRuntimeRequestIdCookie(connection));
      return true;
    } finally {
      waitForMetricsUpdate(); //Wait for metrics to update before running the next test
    }
  }

  private UUID getRequestIdHeader(HttpURLConnection connection) {
    String headerField = connection.getHeaderField(X_REQUEST_ID_HEADERNAME);
    if (headerField == null) {
      return null;
    }
    return UUID.fromString(headerField);
  }

  private UUID getRuntimeRequestIdCookie(HttpURLConnection connection) {
    String headerField = connection.getHeaderField("Set-Cookie");
    if (headerField != null) {
      String[] split = headerField.split("=");
      if (split.length == 2 && RUNTIME_REQUEST_ID_COOKIENAME.equals(split[0])) {
        return UUID.fromString(split[1]);
      }
    }
    return null;
  }

  private void waitForMetricsUpdate() {
    try {
      Thread.sleep(250);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  protected String getText(HttpURLConnection connection) throws IOException {
    return new Scanner(connection.getInputStream()).useDelimiter("\0").next();
  }

  protected abstract TagEncodedMetricName getRootMetric();

  protected abstract URL pathToURL(String path) throws MalformedURLException;
}
