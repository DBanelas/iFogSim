package org.fog.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConsoleMetricsExporter implements MetricsExporter {
    @Override
    public void export(Metrics metrics) {
        System.out.println(metrics.getJsonString());
    }


}
