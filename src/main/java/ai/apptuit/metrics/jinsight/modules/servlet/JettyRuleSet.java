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

import ai.apptuit.metrics.jinsight.modules.common.AbstractRuleSet;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.server.AsyncContextState;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;

/**
 * @author Rajiv Shivane
 */
public class JettyRuleSet extends AbstractRuleSet {

  private static final String HELPER_NAME =
      "ai.apptuit.metrics.jinsight.modules.servlet.JettyRuleHelper";

  private final List<RuleInfo> rules = new ArrayList<>();

  public JettyRuleSet() {
    addRule(Server.class, "<init>(org.eclipse.jetty.util.thread.ThreadPool)",
        RuleInfo.AT_EXIT, "instrument($0)");

    addRule(AsyncContextState.class, "start(Runnable)",
        RuleInfo.AT_ENTRY, "registerAsync($1)");
    addRule(AsyncContextState.class, "start(Runnable)",
        RuleInfo.AT_EXCEPTION_EXIT, "unregisterAsync($1)");

    addRule(ContextHandler.class, "handle(org.eclipse.jetty.server.Request, Runnable)", RuleInfo.AT_ENTRY, "asyncBegin($2)");
    addRule(ContextHandler.class, "handle(org.eclipse.jetty.server.Request, Runnable)", RuleInfo.AT_EXIT, "asyncEnd($2)");
    addRule(ContextHandler.class, "handle(org.eclipse.jetty.server.Request, Runnable)", RuleInfo.AT_EXCEPTION_EXIT, "asyncErr($2)");
  }

  private void addRule(Class clazz, String methodName, String whereClause, String action) {
    addRule(clazz.getName(), methodName, whereClause, action);
  }

  private void addRule(String className, String methodName, String whereClause, String action) {
    String ruleName = className + " " + methodName + " " + whereClause;
    RuleInfo rule = new RuleInfo(ruleName, className, false, false,
        methodName, HELPER_NAME, whereClause, null, null, action, null, null);
    rules.add(rule);
  }

  @Override
  public List<RuleInfo> getRules() {
    return rules;
  }
}
