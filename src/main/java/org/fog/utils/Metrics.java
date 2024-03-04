package org.fog.utils;
import java.util.Map;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

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

    public double getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }

    public double getNetworkUsage() {
        return networkUsage;
    }

    public void setNetworkUsage(double networkUsage) {
        this.networkUsage = networkUsage;
    }

    public int getTuplesSentBySensors() {
        return tuplesSentBySensors;
    }

    public void setTuplesSentBySensors(int tuplesSentBySensors) {
        this.tuplesSentBySensors = tuplesSentBySensors;
    }

    public Map<String, Double> getLatencyPerAppLoop() {
        return latencyPerAppLoop;
    }

    public void setLatencyPerAppLoop(Map<String, Double> latencyPerAppLoop) {
        this.latencyPerAppLoop = latencyPerAppLoop;
    }

    public Map<String, Double> getLatencyPerTupleType() {
        return latencyPerTupleType;
    }

    public void setLatencyPerTupleType(Map<String, Double> latencyPerTupleType) {
        this.latencyPerTupleType = latencyPerTupleType;
    }

    public Map<String, Double> getEnergyConsumptionPerDevice() {
        return energyConsumptionPerDevice;
    }

    public void setEnergyConsumptionPerDevice(Map<String, Double> energyConsumptionPerDevice) {
        this.energyConsumptionPerDevice = energyConsumptionPerDevice;
    }

    public Map<String, Integer> getTuplesProcessedPerModule() {
        return tuplesProcessedPerModule;
    }

    public void setTuplesProcessedPerModule(Map<String, Integer> tuplesProcessedPerModule) {
        this.tuplesProcessedPerModule = tuplesProcessedPerModule;
    }

    public void setThroughputPerModule(Map<String, Double> throughputPerModule) {
        this.throughputPerModule = throughputPerModule;
    }

    public Map<String, Double> getRecsInPerModule() {
        return recsInPerModule;
    }

    public void setRecsInPerModule(Map<String, Double> recsInPerModule) {
        this.recsInPerModule = recsInPerModule;
    }

    public Map<String, Double> getRecsOutPerModule() {
        return recsOutPerModule;
    }

    public void setRecsOutPerModule(Map<String, Double> recsOutPerModule) {
        this.recsOutPerModule = recsOutPerModule;
    }

    public Map<String, Long> getRemainingDataPerOperator() {
        return remainingDataPerOperator;
    }

    public void setRemainingDataPerOperator(Map<String, Long> remainingDataPerOperator) {
        this.remainingDataPerOperator = remainingDataPerOperator;
    }

    public Map<String, String> getPlacement() {
        return placement;
    }

    public void setPlacement(Map<String, String> placement) {
        this.placement = placement;
    }

    /**
     * Remaining events per operator
     * <br>
     * Key: Operator name
     * <br>
     * Value: Remaining events
     */
    Map<String, Long> remainingDataPerOperator;

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
        JsonNode recsInPerModule = objectMapper.valueToTree(this.recsInPerModule);
        JsonNode recsOutPerModule = objectMapper.valueToTree(this.recsOutPerModule);
        JsonNode remainingDataPerOperator = objectMapper.valueToTree(this.remainingDataPerOperator);
        JsonNode placement = objectMapper.valueToTree(this.placement);
        rootNode.set("placement", placement);
        rootNode.set("latencyPerAppLoop", latencyPerAppLoop);
        rootNode.set("latencyPerTupleType", latencyPerTupleType);
        rootNode.set("energyConsumptionPerDevice", energyConsumptionPerDevice);
        rootNode.set("tuplesProcessedPerModule", tuplesProcessedPerModule);
        rootNode.set("recsInPerModule", recsInPerModule);
        rootNode.set("recsOutPerModule", recsOutPerModule);
        rootNode.set("remainingDataPerDevice", remainingDataPerOperator);
        rootNode.put("executionTime", executionTime);
        rootNode.put("networkUsage", networkUsage);
        rootNode.put("tuplesSentBySensors", tuplesSentBySensors);
        return rootNode.toPrettyString();
    }
}
