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

package ai.apptuit.metrics.jinsight.testing;

import io.opencensus.trace.Tracing;
import io.opencensus.trace.export.SpanData;
import io.opencensus.trace.export.SpanExporter.Handler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LocalTraceExporter extends Handler {

  private static final String REGISTER_NAME = LocalTraceExporter.class.getName();

  private List<SpanData> spans = new ArrayList<>();

  public static void createAndRegister() {
    Tracing.getExportComponent().getSpanExporter()
        .registerHandler(REGISTER_NAME, new LocalTraceExporter());
  }

  public static void unRegister() {
    Tracing.getExportComponent().getSpanExporter().unregisterHandler(REGISTER_NAME);
  }

  @Override
  public void export(Collection<SpanData> spanDataList) {
    spans.addAll(spanDataList);
  }

  public List<SpanData> getSpans() {
    return spans;
  }
}
