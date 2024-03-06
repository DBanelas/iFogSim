package org.fog.utils;

public class ConsoleMetricsExporter implements MetricsExporter {
    @Override
    public void export(Metrics metrics) {
        System.out.println(metrics.getJsonString());
    }


}
