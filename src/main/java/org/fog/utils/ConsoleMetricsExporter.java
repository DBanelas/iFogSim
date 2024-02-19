package org.fog.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConsoleMetricsExporter implements MetricsExporter {
    @Override
    public void export(Metrics metrics) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("executionTime", metrics.getExecutionTime());
        objectNode.put("networkUsage", metrics.getNetworkUsage());
        objectNode.put("tuplesSentBySensors", metrics.getTuplesSentBySensors());
        objectNode.put("futureQueueSize", metrics.getFutureQueueSize());
        objectNode.put("deferredQueueSize", metrics.getDeferredQueueSize());
        JsonNode latencyPerAppLoop = objectMapper.valueToTree(metrics.getLatencyPerAppLoop());
        JsonNode latencyPerTupleType = objectMapper.valueToTree(metrics.getLatencyPerTupleType());
        JsonNode energyConsumptionPerDevice = objectMapper.valueToTree(metrics.getEnergyConsumptionPerDevice());
        JsonNode tuplesProcessedPerModule = objectMapper.valueToTree(metrics.getTuplesProcessedPerModule());
//        JsonNode throughputPerModule = objectMapper.valueToTree(metrics.getThroughputPerModule());
        JsonNode recsInPerModule = objectMapper.valueToTree(metrics.getRecsInPerModule());
        JsonNode recsOutPerModule = objectMapper.valueToTree(metrics.getRecsOutPerModule());
        JsonNode remainingEventsPerDevice = objectMapper.valueToTree(metrics.getRemainingEventsPerDevice());
        objectNode.set("latencyPerAppLoop", latencyPerAppLoop);
        objectNode.set("latencyPerTupleType", latencyPerTupleType);
        objectNode.set("energyConsumptionPerDevice", energyConsumptionPerDevice);
        objectNode.set("tuplesProcessedPerModule", tuplesProcessedPerModule);
//        objectNode.set("throughputPerModule", throughputPerModule);
        objectNode.set("recsInPerModule", recsInPerModule);
        objectNode.set("recsOutPerModule", recsOutPerModule);
        objectNode.set("remainingEventsPerDevice", remainingEventsPerDevice);
        System.out.println(objectNode.toPrettyString());
    }


}
