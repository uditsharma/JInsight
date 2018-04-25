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

import ai.apptuit.metrics.jinsight.WebRequestContext;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import java.util.logging.Logger;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class WebServerRuleHelper extends RuleHelper {

  private static final String REQUEST_ID_PROPERTYNAME = "web.requestcontext.request-id";
  private static final String CONTEXT_ROOT_PROPERTYNAME = "web.requestcontext.context-root";
  private static final Logger LOGGER = Logger.getLogger(WebServerRuleHelper.class.getName());
  private static final boolean DEBUG = false;

  public WebServerRuleHelper(Rule rule) {
    super(rule);
  }

  public void registerAsync(Runnable async) {
    WebRequestContext currentRequest = WebRequestContext.getCurrentContext();
    setObjectProperty(async, REQUEST_ID_PROPERTYNAME, currentRequest.getRequestID());
    setObjectProperty(async, CONTEXT_ROOT_PROPERTYNAME, currentRequest.getContextPath());
    logdebug("WebServerRuleHelper.registerAsync", currentRequest);
  }

  public void unregisterAsync(Runnable async) {
    removeObjectProperty(async, REQUEST_ID_PROPERTYNAME);
    removeObjectProperty(async, CONTEXT_ROOT_PROPERTYNAME);
    logdebug("WebServerRuleHelper.unregisterAsync");
  }

  public void asyncBegin(Runnable async) {
    String requestId = getObjectProperty(async, CONTEXT_ROOT_PROPERTYNAME);
    if (requestId == null) {
      logdebug("WebServerRuleHelper.asyncBegin: SKIP");
      return;
    }
    String context = getObjectProperty(async, REQUEST_ID_PROPERTYNAME);
    WebRequestContext requestContext = WebRequestContext.pushContext(requestId, context);
    logdebug("WebServerRuleHelper.asyncBegin", requestContext);
  }

  public void asyncEnd(Runnable async) {
    String requestId = removeObjectProperty(async, CONTEXT_ROOT_PROPERTYNAME);
    if (requestId == null) {
      logdebug("WebServerRuleHelper.asyncEnd: SKIP");
      return;
    }
    removeObjectProperty(async, REQUEST_ID_PROPERTYNAME);
    WebRequestContext requestContext = WebRequestContext.endRequest();
    logdebug("WebServerRuleHelper.asyncEnd", requestContext);
  }

  public void asyncErr(Runnable async) {
    String requestId = removeObjectProperty(async, CONTEXT_ROOT_PROPERTYNAME);
    if (requestId == null) {
      logdebug("WebServerRuleHelper.asyncErr: SKIP");
      return;
    }
    removeObjectProperty(async, REQUEST_ID_PROPERTYNAME);
    WebRequestContext requestContext = WebRequestContext.endRequest();
    logdebug("WebServerRuleHelper.asyncErr", requestContext);
  }

  private void logdebug(String message) {
    logdebug(message, null);
  }

  private void logdebug(String message, WebRequestContext context) {
    if (DEBUG) {
      if (context == null) {
        LOGGER.info(message);
      } else {
        LOGGER.info(message + ":" + context);
      }
    }
  }

}
