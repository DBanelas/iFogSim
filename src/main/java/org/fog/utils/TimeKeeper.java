package org.fog.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Tuple;

/**
 * Singleton class used to keep track of time in the simulation.
 */
public class TimeKeeper {
	private static TimeKeeper instance;
	private long simulationStartTime;
	private int count; //Variable to create unique IDs wherever needed (e.g. TupleID, AppLoopID)
	private Map<Integer, Double> emitTimes;
	private Map<Integer, Double> endTimes;
	private Map<Integer, Double> tupleIdToCpuStartTime;
	private Map<String, Double> tupleTypeToAverageCpuTime;
	private Map<String, Integer> tupleTypeToExecutedTupleCount;
	private Map<Integer, List<Integer>> loopIdToTupleIds;
	private Map<Integer, Double> loopIdToCurrentAverage;
	private Map<Integer, Integer> loopIdToCurrentNum;
	private Map<Integer, Integer> loopIdToLatencyQoSSuccessCount = new HashMap<>();
	// loopID -> < Microservice -> < deviceID, <requestCount,totalExecutionTime > >
	private Map<Integer, Map<String, Map<Integer, Pair<Integer, Double>>>> costCalcData = new HashMap<>();
	// last execution time
	private Map<Integer, Double> tupleIdToExecutionTime = new HashMap<>();

	/**
	 * Method to create and return the singleton instance of the TimeKeeper
	 * @return the singleton instance of the TimeKeeper
	 */
	public static TimeKeeper getInstance(){
		if(instance == null)
			instance = new TimeKeeper();
		return instance;
	}

	/**
	 * Method to clear the data in the TimeKeeper instance.
	 * Only to be called when an object already exists and needs to be cleared.
	 */
	public void clearData(){
		instance = new TimeKeeper();
	}

	/**
	 * Private method to create a TimeKeeper instance.
	 */
	private TimeKeeper(){
		count = 1;
		this.emitTimes = new HashMap<>();
		this.endTimes = new HashMap<>();
		this.loopIdToTupleIds = new HashMap<>();
		this.tupleTypeToAverageCpuTime = new HashMap<>();
		this.tupleTypeToExecutedTupleCount = new HashMap<>();
		this.tupleIdToCpuStartTime = new HashMap<>();
		this.loopIdToCurrentAverage = new HashMap<>();
		this.loopIdToCurrentNum = new HashMap<>();
	}
	
	public void tupleStartedExecution(Tuple tuple){
		tupleIdToCpuStartTime.put(tuple.getCloudletId(), CloudSim.clock());
	}
	
	public void tupleEndedExecution(Tuple tuple){
		if(!tupleIdToCpuStartTime.containsKey(tuple.getCloudletId()))
			return;
		double executionTime = CloudSim.clock() - tupleIdToCpuStartTime.get(tuple.getCloudletId());
		if(!tupleTypeToAverageCpuTime.containsKey(tuple.getTupleType())){
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), executionTime);
			tupleTypeToExecutedTupleCount.put(tuple.getTupleType(), 1);
		} else{
			double currentAverage = tupleTypeToAverageCpuTime.get(tuple.getTupleType());
			int currentCount = tupleTypeToExecutedTupleCount.get(tuple.getTupleType());
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), (currentAverage*currentCount+executionTime)/(currentCount+1));
		}
	}
	
	public Map<Integer, List<Integer>> loopIdToTupleIds(){
		return getInstance().getLoopIdToTupleIds();
	}

	public int getUniqueId(){
		return count++;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}

	public Map<Integer, Double> getEmitTimes() {
		return emitTimes;
	}

	public void setEmitTimes(Map<Integer, Double> emitTimes) {
		this.emitTimes = emitTimes;
	}

	public Map<Integer, Double> getEndTimes() {
		return endTimes;
	}

	public void setEndTimes(Map<Integer, Double> endTimes) {
		this.endTimes = endTimes;
	}

	public Map<Integer, List<Integer>> getLoopIdToTupleIds() {
		return loopIdToTupleIds;
	}

	public void setLoopIdToTupleIds(Map<Integer, List<Integer>> loopIdToTupleIds) {
		this.loopIdToTupleIds = loopIdToTupleIds;
	}

	public Map<String, Double> getTupleTypeToAverageCpuTime() {
		return tupleTypeToAverageCpuTime;
	}

	public void setTupleTypeToAverageCpuTime(
			Map<String, Double> tupleTypeToAverageCpuTime) {
		this.tupleTypeToAverageCpuTime = tupleTypeToAverageCpuTime;
	}

	public Map<String, Integer> getTupleTypeToExecutedTupleCount() {
		return tupleTypeToExecutedTupleCount;
	}

	public void setTupleTypeToExecutedTupleCount(Map<String, Integer> tupleTypeToExecutedTupleCount) {
		this.tupleTypeToExecutedTupleCount = tupleTypeToExecutedTupleCount;
	}

	public Map<Integer, Double> getTupleIdToCpuStartTime() {
		return tupleIdToCpuStartTime;
	}

	public void setTupleIdToCpuStartTime(Map<Integer, Double> tupleIdToCpuStartTime) {
		this.tupleIdToCpuStartTime = tupleIdToCpuStartTime;
	}

	public long getSimulationStartTime() {
		return simulationStartTime;
	}

	public void setSimulationStartTime(long simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
	}

	public Map<Integer, Double> getLoopIdToCurrentAverage() {
		return loopIdToCurrentAverage;
	}

	public void setLoopIdToCurrentAverage(Map<Integer, Double> loopIdToCurrentAverage) {
		this.loopIdToCurrentAverage = loopIdToCurrentAverage;
	}

	public Map<Integer, Integer> getLoopIdToCurrentNum() {
		return loopIdToCurrentNum;
	}

	public void setLoopIdToCurrentNum(Map<Integer, Integer> loopIdToCurrentNum) {
		this.loopIdToCurrentNum = loopIdToCurrentNum;
	}

	public Map<Integer, Integer> getLoopIdToLatencyQoSSuccessCount() {
		return loopIdToLatencyQoSSuccessCount;
	}

	public void addCostCalcData(List<Integer> loopIds, String microserviceName, int deviceId, int tupleId) {
//		for (Integer loopid : loopIds) {
//			if (costCalcData.containsKey(loopid)) {
//				if (costCalcData.get(loopid).containsKey(microserviceName)) {
//					if (costCalcData.get(loopid).get(microserviceName).containsKey(deviceId)) {
//						double totalExecutionTime = tupleIdToExecutionTime.get(tupleId) + costCalcData.get(loopid).get(microserviceName).get(deviceId).getSecond();
//						int totalRequestCount = costCalcData.get(loopid).get(microserviceName).get(deviceId).getFirst() + 1;
//						costCalcData.get(loopid).get(microserviceName).put(deviceId, new Pair<>(totalRequestCount, totalExecutionTime));
//					} else {
//						costCalcData.get(loopid).get(microserviceName).put(deviceId, new Pair<>(1, tupleIdToExecutionTime.get(tupleId)));
//					}
//				} else {
//					Map<Integer, Pair<Integer, Double>> m1 = new HashMap<>();
//					m1.put(deviceId, new Pair<>(1, tupleIdToExecutionTime.get(tupleId)));
//
//					costCalcData.get(loopid).put(microserviceName, m1);
//				}
//			} else {
//				Map<Integer, Pair<Integer, Double>> m1 = new HashMap<>();
//				m1.put(deviceId, new Pair<>(1, tupleIdToExecutionTime.get(tupleId)));
//
//				Map<String, Map<Integer, Pair<Integer, Double>>> m2 = new HashMap<>();
//				m2.put(microserviceName, m1);
//
//				costCalcData.put(loopid, m2);
//			}
//		}
	}
	
	
}
