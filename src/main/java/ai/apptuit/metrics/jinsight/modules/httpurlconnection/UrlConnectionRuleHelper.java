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

package ai.apptuit.metrics.jinsight.modules.httpurlconnection;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.TracingService;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import com.codahale.metrics.Clock;
import com.codahale.metrics.Timer;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.opencensus.trace.*;
import io.opencensus.trace.propagation.TextFormat;
import org.jboss.byteman.rule.Rule;

import static io.opencensus.trace.Span.Kind.CLIENT;

/**
 * @author Rajiv Shivane
 */
public class UrlConnectionRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("http.requests");
  private static final String START_TIME_PROPERTY_NAME =
      UrlConnectionRuleHelper.class + ".START_TIME";
  private static final Clock CLOCK = Clock.defaultClock();

  private static final String TRACE_STARTED = UrlConnectionRuleHelper.class + ".trace.started";
  private static final String TRACE_CLOSED = UrlConnectionRuleHelper.class + ".trace.closed";
  private static final TracingService tracer = TracingService.getInstance();
  private static final TextFormat.Setter<HttpURLConnection> injector = new TextFormat.Setter<HttpURLConnection>() {
    @Override
    public void put(HttpURLConnection connection, String header, String value) {
      connection.setRequestProperty(header, value);
    }
  };
  private static final TextFormat textFormat = Tracing.getPropagationComponent()
      .getTraceContextFormat();

  private Map<String, Timer> timers = new ConcurrentHashMap<>();


  public UrlConnectionRuleHelper(Rule rule) {
    super(rule);
  }

  public void onConnect(HttpURLConnection urlConnection) {
    setObjectProperty(urlConnection, START_TIME_PROPERTY_NAME, CLOCK.getTick());

  }

  public void trace(HttpURLConnection connection) {
    String spanName = connection.getRequestMethod() + "-" + connection.getURL().getPath();
    // TODO this will start a new span under existing parent, or it will become a parent itself if no parent span exist.
    // TODO How to handle cases when trigger method already injected the tracing.
    // TODO Should add the tracing config in the existing agent config. This looks super hacky, needs to find better way.
    String traceStarted = getObjectProperty(connection, TRACE_STARTED);
    String traceClosed = getObjectProperty(connection, TRACE_CLOSED);
    if (!Boolean.parseBoolean(traceStarted) && !Boolean.parseBoolean(traceClosed)) {
      setObjectProperty(connection, TRACE_STARTED, "true");
      tracer.createSpanScope(spanName, CLIENT);
      textFormat.inject(tracer.currentSpan().getContext(), connection, injector);
    }
  }

  public void onGetInputStream(HttpURLConnection urlConnection, int statusCode) {
    Long startTime = removeObjectProperty(urlConnection, START_TIME_PROPERTY_NAME);
    if (startTime == null) {
      return;
    }

    long t = Clock.defaultClock().getTick() - startTime;
    String method = urlConnection.getRequestMethod();
    String status = "" + statusCode;
    Timer timer = timers.computeIfAbsent(status + method, s -> {
      TagEncodedMetricName metricName = ROOT_NAME.withTags(
          "method", method,
          "status", status);
      return getTimer(metricName);
    });

    timer.update(t, TimeUnit.NANOSECONDS);
    String traceStarted = getObjectProperty(urlConnection, TRACE_STARTED);
    if (Boolean.parseBoolean(traceStarted)) {
      tracer.addAttributeToCurrentSpan("http.statusCode", status);
      tracer.addAttributeToCurrentSpan("http.method", method);
      tracer.closeCurrentScope();
      removeObjectProperty(urlConnection, TRACE_STARTED);
      //TODO this is required as at some
      // place getInputStream is getting called multiple times,
      // and to avoid creating multiple spans need to this indicator.
      setObjectProperty(urlConnection, TRACE_CLOSED, "true");
    }
  }
}
