package ai.apptuit.metrics.jinsight;

import io.opencensus.common.Scope;
import io.opencensus.exporter.trace.logging.LoggingTraceExporter;
import io.opencensus.exporter.trace.zipkin.ZipkinTraceExporter;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Service provide interface to create span and attach it to current context.
 */
public class TracingService {

  private static final Logger LOGGER = Logger.getLogger(TracingService.class.getName());

  private static final ThreadLocal<Scope> scopes = new ThreadLocal<>();
  private static final TracingService service = new TracingService();

  private TracingService() {
    this(ConfigService.getInstance());
  }

  private TracingService(ConfigService instance) {
    initTracingExporter(instance);
  }

  private void initTracingExporter(ConfigService instance) {
    if (instance.isTracingEnabled()) {
      switch (instance.getTraceExporter()) {
        case ZIPKIN:
          initZipKinExporter(instance);
          break;
        case LOG:
          initLogTraceExporter();
          break;
      }
    } else {
      LOGGER.fine("Distributed Tracing is disabled.");
    }
  }

  private void initZipKinExporter(ConfigService config) {
    String endPoint = config.getTraceExporterEndPoint();
    String serviceName = config.getServiceName();
    if (endPoint != null) {
      try {
        URL url = new URL(endPoint);
      } catch (MalformedURLException e) {
        LOGGER.severe("Invalid ZipKin endpoint [" + endPoint + "]. Traces will not be exported.");
        LOGGER.log(Level.FINE, e.toString(), e);
      }
      ZipkinTraceExporter.createAndRegister(endPoint, serviceName);
    } else {
      LOGGER.severe("ZipKin Endpoint is not provided. Traces will not be exported.");
    }
  }

  private void initLogTraceExporter() {
    LoggingTraceExporter.register();
  }

  public static TracingService getInstance() {
    return service;
  }

  public Scope currentScope() {
    return scopes.get();
  }

  private void pushScope(Scope scope) {
    scopes.set(scope);
  }

  public Span currentSpan() {
    return Tracing.getTracer().getCurrentSpan();
  }

  public void closeCurrentSpan() {
    Tracing.getTracer().getCurrentSpan().end();
  }

  public void closeCurrentScope() {
    Scope scope = currentScope();
    if (scope != null) {
      scope.close();
    } else {
      throw new IllegalStateException("No Active Tracing Span Scope");
    }
  }

  public Scope createSpanScope(String spanName, Span.Kind spanKind) {
    Scope scope = Tracing.getTracer().spanBuilder(spanName).setSpanKind(spanKind)
        .setSampler(Samplers.alwaysSample()).setRecordEvents(true).startScopedSpan();
    pushScope(scope);
    return scope;
  }

  public void addAttributeToCurrentSpan(String key, String value) {
    Span currentSpan = Tracing.getTracer().getCurrentSpan();
    currentSpan.putAttribute(key, AttributeValue.stringAttributeValue(value));
  }
}
