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
import org.apache.catalina.core.AsyncContextImpl;
import org.apache.catalina.core.StandardContext;

/**
 * @author Rajiv Shivane
 */
public class TomcatRuleSet extends AbstractRuleSet {

  private static final String HELPER_NAME =
      "ai.apptuit.metrics.jinsight.modules.servlet.TomcatRuleHelper";
  private static final String ASYNC_CONTEXT_IMPL = "org.apache.catalina.core.AsyncContextImpl$RunnableWrapper";

  private final List<RuleInfo> rules = new ArrayList<>();

  public TomcatRuleSet() {
    addRule(StandardContext.class, "<init>",
        RuleInfo.AT_EXIT, "instrument($0)");

    addRule(AsyncContextImpl.class, "start(Runnable)",
        RuleInfo.AT_ENTRY, "registerAsync($1)");
    addRule(AsyncContextImpl.class, "start(Runnable)",
        RuleInfo.AT_EXCEPTION_EXIT, "unregisterAsync($1)");

    addRule(ASYNC_CONTEXT_IMPL, "run", RuleInfo.AT_ENTRY, "asyncBegin($0.wrapped)");
    addRule(ASYNC_CONTEXT_IMPL, "run", RuleInfo.AT_EXIT, "asyncEnd($0.wrapped)");
    addRule(ASYNC_CONTEXT_IMPL, "run", RuleInfo.AT_EXCEPTION_EXIT, "asyncErr($0.wrapped)");
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
