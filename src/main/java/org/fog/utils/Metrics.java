package org.fog.utils;
import java.util.Map;


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
}
