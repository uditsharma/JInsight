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

package ai.apptuit.metrics.jinsight;

import static ai.apptuit.metrics.jinsight.modules.servlet.ContextMetricsHelper.ROOT_CONTEXT_PATH;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Rajiv Shivane
 */
public class WebRequestContext {

  public static final String X_REQUEST_ID_HEADERNAME = "X-Request-ID";

  private static final String REQUEST_CONTEXT_ATTRIBUTENAME = "X-Request-Context";
  private static final ThreadLocal<Deque<WebRequestContext>> contexts =
      ThreadLocal.withInitial(ArrayDeque::new);

  private String contextPath;
  private String requestID;


  private WebRequestContext(String contextPath, String requestID) {
    if (contextPath == null) {
      throw new IllegalArgumentException("contextPath can not be null");
    }
    if (requestID == null) {
      throw new IllegalArgumentException("requestID can not be null");
    }
    this.contextPath = contextPath;
    this.requestID = requestID;
  }

  public static void beginRequest(HttpServletRequest request, HttpServletResponse response) {

    String contextPath = request.getContextPath();
    if (contextPath == null || contextPath.length() == 1) {
      contextPath = "";
    }
    if (contextPath.trim().equals("")) {
      contextPath = ROOT_CONTEXT_PATH;
    }

    //Pick up requestID from the attribute if we are re-entering for exception handling in Jetty
    WebRequestContext context = (WebRequestContext) request
        .getAttribute(REQUEST_CONTEXT_ATTRIBUTENAME);
    if (context == null) {
      String requestID = getRequestID(request);
      context = new WebRequestContext(contextPath, requestID);
      request.setAttribute(REQUEST_CONTEXT_ATTRIBUTENAME, context);
    }
    setContext(context);
    response.setHeader(X_REQUEST_ID_HEADERNAME, context.getRequestID());
  }

  private static String getRequestID(HttpServletRequest request) {
    String requestID = request.getHeader(X_REQUEST_ID_HEADERNAME);
    if (requestID == null) {
      requestID = UUID.randomUUID().toString();
    }
    return requestID;
  }

  public static WebRequestContext endRequest() {
    return unsetContext();
  }

  public static WebRequestContext getCurrentRequest() {
    return contexts.get().peek();
  }

  public static WebRequestContext pushContext(String contextPath, String requestId) {
    WebRequestContext context = new WebRequestContext(contextPath, requestId);
    setContext(context);
    return context;
  }

  private static void setContext(WebRequestContext context) {
    contexts.get().push(context);
  }

  private static WebRequestContext unsetContext() {
    return contexts.get().pop();
  }

  public String getContextPath() {
    return contextPath;
  }

  public String getRequestID() {
    return requestID;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebRequestContext that = (WebRequestContext) o;
    return Objects.equals(contextPath, that.contextPath) &&
        Objects.equals(requestID, that.requestID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contextPath, requestID);
  }

  @Override
  public String toString() {
    return "WebRequestContext{" +
        "contextPath='" + contextPath + '\'' +
        ", requestID='" + requestID + '\'' +
        '}';
  }
}
