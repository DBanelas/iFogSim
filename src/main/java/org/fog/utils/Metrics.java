package org.fog.utils;
import java.util.Map;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Metrics {
    /**
     * Execution time of the simulation
     */
    double executionTime;

    /**
     * Network usage of the simulation
     */
    double networkUsage;

    /**
     * Total number of tuples sent by sensors
     */
    int tuplesSentBySensors;

    /**
     * Size of the future queue
     */
    int futureQueueSize;

    /**
     * Size of the deferred queue
     */
    int deferredQueueSize;

    /**
     * Latency per application loop
     * Key: Application Loop ID
     * Value: Latency
     */
    Map<String, Double> latencyPerAppLoop;

    /**
     * Latency per tuple type
     * Key: Tuple Type
     * Value: Latency
     */
    Map<String, Double> latencyPerTupleType;

    /**
     * Energy consumption per device
     * <br>
     * Key: Device name
     * <br>
     * Value: Energy Consumption
     */
    Map<String, Double> energyConsumptionPerDevice;

    /**
     * Tuples processed per module
     * <br>
     * Key: Module name
     * <br>
     * Value: Tuples Processed
     */
    Map<String, Integer> tuplesProcessedPerModule;

    /**
     * Throughput per module
     * <br>
     * Key: Module name
     * <br>
     * Value: Throughput
     */
    Map<String, Double> throughputPerModule;

    /**
     * Recommended tuples in per module
     * <br>
     * Key: Module name
     * <br>
     * Value: Avg records in per second
     */
    Map<String, Double> recsInPerModule;

    /**
     * Recommended tuples out per module
     * <br>
     * Key: Module name
     * <br>
     * Value: Avg records out per second
     */
    Map<String, Double> recsOutPerModule;

    /**
     * Remaining events per device
     * <br>
     * Key: Device name
     * <br>
     * Value: Remaining events
     */
    Map<String, Integer> remainingEventsPerDevice;

    /**
     * Placement of modules
     * <br>
     * Key: Module name
     * <br>
     * Value: Device name
     */
    Map<String, String> placement;

    public String getJsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = objectMapper.createObjectNode();
        JsonNode latencyPerAppLoop = objectMapper.valueToTree(this.latencyPerAppLoop);
        JsonNode latencyPerTupleType = objectMapper.valueToTree(this.latencyPerTupleType);
        JsonNode energyConsumptionPerDevice = objectMapper.valueToTree(this.energyConsumptionPerDevice);
        JsonNode tuplesProcessedPerModule = objectMapper.valueToTree(this.tuplesProcessedPerModule);
        JsonNode throughputPerModule = objectMapper.valueToTree(this.throughputPerModule);
        JsonNode recsInPerModule = objectMapper.valueToTree(this.recsInPerModule);
        JsonNode recsOutPerModule = objectMapper.valueToTree(this.recsOutPerModule);
        JsonNode remainingEventsPerDevice = objectMapper.valueToTree(this.remainingEventsPerDevice);
        JsonNode placement = objectMapper.valueToTree(this.placement);
        rootNode.set("placement", placement);
        rootNode.set("latencyPerAppLoop", latencyPerAppLoop);
        rootNode.set("latencyPerTupleType", latencyPerTupleType);
        rootNode.set("energyConsumptionPerDevice", energyConsumptionPerDevice);
        rootNode.set("tuplesProcessedPerModule", tuplesProcessedPerModule);
        rootNode.set("throughputPerModule", throughputPerModule);
        rootNode.set("recsInPerModule", recsInPerModule);
        rootNode.set("recsOutPerModule", recsOutPerModule);
        rootNode.set("remainingEventsPerDevice", remainingEventsPerDevice);
        rootNode.put("executionTime", executionTime);
        rootNode.put("networkUsage", networkUsage);
        rootNode.put("tuplesSentBySensors", tuplesSentBySensors);
        rootNode.put("futureQueueSize", futureQueueSize);
        rootNode.put("deferredQueueSize", deferredQueueSize);
        return rootNode.toPrettyString();
    }
}
