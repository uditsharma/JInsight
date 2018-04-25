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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;

/**
 * @author Rajiv Shivane
 */
public class WebRequestContext {

  private static final ThreadLocal<Deque<WebRequestContext>> contexts =
      ThreadLocal.withInitial(ArrayDeque::new);
  private static volatile RequestContextChangeListener[] listeners =
      new RequestContextChangeListener[0];

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

  public static WebRequestContext endRequest() {
    return unsetContext();
  }

  public static WebRequestContext getCurrentContext() {
    return contexts.get().peek();
  }

  public static WebRequestContext pushContext(String contextPath, String requestId) {
    WebRequestContext context = new WebRequestContext(contextPath, requestId);
    setContext(context);
    return context;
  }

  public static void setContext(WebRequestContext context) {
    contexts.get().push(context);
    for (RequestContextChangeListener listener : listeners) {
      listener.beginContext(context);
    }
  }

  private static WebRequestContext unsetContext() {
    WebRequestContext context = contexts.get().pop();
    for (RequestContextChangeListener listener : listeners) {
      listener.endContext(context);
    }
    return context;
  }

  public static synchronized void addRequestContextChangeListener(
      RequestContextChangeListener listener) {
    Collection<RequestContextChangeListener> list = toCollection(listeners);
    list.add(listener);
    listeners = list.toArray(new RequestContextChangeListener[0]);
  }

  public static synchronized boolean removeRequestContextChangeListener(
      RequestContextChangeListener listener) {
    Collection<RequestContextChangeListener> list = toCollection(listeners);
    boolean removed = list.remove(listener);
    listeners = list.toArray(new RequestContextChangeListener[0]);
    return removed;
  }

  private static <T> Collection<T> toCollection(T[] array) {
    return new ArrayList<T>(Arrays.asList(array));
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
    return Objects.equals(contextPath, that.contextPath)
        && Objects.equals(requestID, that.requestID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contextPath, requestID);
  }

  @Override
  public String toString() {
    return "WebRequestContext{"
        + "contextPath='" + contextPath + '\''
        + ", requestID='" + requestID + '\''
        + '}';
  }


  public interface RequestContextChangeListener {

    void beginContext(WebRequestContext context);

    void endContext(WebRequestContext context);
  }
}
