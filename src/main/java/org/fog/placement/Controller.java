package org.fog.placement;

import java.util.*;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.utils.*;

public class Controller extends SimEntity{
	
	public static boolean ONLY_CLOUD = false;
		
	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	
	private Map<String, Application> applications;
	private Map<String, Integer> appLaunchDelays;

	private Map<String, ModulePlacement> appModulePlacementPolicy;
	private final Metrics metrics;
	
	public Controller(String name, List<FogDevice> fogDevices,
					  List<Sensor> sensors, List<Actuator> actuators) {
		super(name);
		this.applications = new HashMap<String, Application>();
		setAppLaunchDelays(new HashMap<String, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
		for(FogDevice fogDevice : fogDevices){
			fogDevice.setControllerId(getId());
		}
		setFogDevices(fogDevices);
		setActuators(actuators);
		setSensors(sensors);
		connectWithLatencies();
		this.metrics = new Metrics();
	}

	public Metrics getMetrics() {
		return metrics;
	}

	private FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices()){
			if(id == fogDevice.getId())
				return fogDevice;
		}
		return null;
	}
	
	private void connectWithLatencies(){
		for(FogDevice fogDevice : getFogDevices()){
			for(int parentID : fogDevice.getParentIds()) {
				FogDevice parent = getFogDeviceById(parentID);
				if(parent == null) {
					continue;
				}
				double latency = fogDevice.getUplinkLatency();
				parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				parent.getChildrenIds().add(fogDevice.getId());
			}
		}
	}
	
	@Override
	public void startEntity() {
		for(String appId : applications.keySet()){
			if(getAppLaunchDelays().get(appId)==0)
				processAppSubmit(applications.get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
		}

		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
		
		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
		
		for(FogDevice dev : getFogDevices())
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);

	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.TUPLE_FINISHED:
			processTupleFinished(ev);
			break;
		case FogEvents.CONTROLLER_RESOURCE_MANAGE:
			manageResources();
			break;
		case FogEvents.STOP_SIMULATION:
			CloudSim.terminateSimulation();
			setPlacement();
			setExecutionTime();
			setTupleTypeLatencies();
			setAppLoopLatency();
			setEnergyConsumptionPerDevice();
			setNetworkUsage();
			setTotalTuplesSentBySensorDevices();
			setTuplesProcessedPerAppModule();
			setRecordsInPerModule();
			setRecordsOutPerModule();
			setUnexecutedEventsForEachOperator();
			break;
		}
	}

	private void setPlacement() {
		Map<String, String> placement = new HashMap<>();
		String appID = "";
		for (String id : getApplications().keySet()) appID = id;

		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(appID);
		for (Integer deviceId : modulePlacement.getDeviceToModuleMap().keySet()) {
			for (AppModule module : modulePlacement.getDeviceToModuleMap().get(deviceId)) {
				placement.put(module.getName(), getFogDeviceById(deviceId).getName());
			}
		}

		metrics.setPlacement(placement);
	}

	private void setRecordsOutPerModule() {
		Map<String, Double> recordsOutPerModule = new HashMap<>();
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppModule module : app.getModules()){
				recordsOutPerModule.put(module.getName(), module.getAvgTuplesSentPerSec());
			}
		}
		metrics.setRecsOutPerModule(recordsOutPerModule);
	}

	private void setRecordsInPerModule() {
		Map<String, Double> recordsInPerModule = new HashMap<>();
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppModule module : app.getModules()){
				recordsInPerModule.put(module.getName(), module.getAvgTuplesRecievedPerSec());
			}
		}
		metrics.setRecsInPerModule(recordsInPerModule);
	}

	private void setThroughputPerAppModule() {
		Map<String, Double> throughputPerModule = new HashMap<>();
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppModule module : app.getModules()){
				throughputPerModule.put(module.getName(), (double) module.getTotalTuplesProcessed()/CloudSim.clock());
			}
		}
		metrics.setThroughputPerModule(throughputPerModule);
	}

	private void setUnexecutedEventsForEachOperator() {
		HashMap<String, Long> unexecutedEvents = new HashMap<>();
		for (String appID : getApplications().keySet()) {
			Application app = getApplications().get(appID);
			for (AppModule module : app.getModules()) {
				unexecutedEvents.put(module.getName(), 0L);
			}
		}
		getFutureQueueEvents(unexecutedEvents);
		getNorthTupleQueueEvents(unexecutedEvents);
		getSouthTupleQueueEvents(unexecutedEvents);
		metrics.setRemainingDataPerOperator(unexecutedEvents);
	}

	private void getNorthTupleQueueEvents(HashMap<String, Long> unexecutedEvents) {
		for(FogDevice fogDevice : getFogDevices()){
			Queue<Pair<Tuple, Integer>> northQueue = fogDevice.getNorthTupleQueue();
			for(Pair<Tuple, Integer> pair : northQueue){
				Tuple tuple = pair.getFirst();
				String moduleName = tuple.getSrcModuleName();
				if (!unexecutedEvents.containsKey(moduleName)) continue;
				long dataSize = tuple.getCloudletFileSize();
				unexecutedEvents.put(moduleName, unexecutedEvents.get(moduleName) + dataSize);
			}
		}
	}

	private void getSouthTupleQueueEvents(HashMap<String, Long> unexecutedEvents) {
		for(FogDevice fogDevice : getFogDevices()){
			Queue<Pair<Tuple, Integer>> southQueue = fogDevice.getSouthTupleQueue();
			for(Pair<Tuple, Integer> pair : southQueue){
				Tuple tuple = pair.getFirst();
				String moduleName = tuple.getSrcModuleName();
				long dataSize = tuple.getCloudletFileSize();
				unexecutedEvents.put(moduleName, unexecutedEvents.get(moduleName) + dataSize);
			}
		}
	}

	private void getFutureQueueEvents(HashMap<String, Long> unexecutedEvents) {
		FutureQueue futureQueue = CloudSim.getFutureQueue();
		Iterator<SimEvent> it = futureQueue.iterator();
		PredicateType predicate = new PredicateType(FogEvents.TUPLE_ARRIVAL);
		while(it.hasNext()){
			SimEvent event = it.next();
			if (predicate.match(event)) {
				Tuple tuple = (Tuple) event.getData();
				String moduleName = tuple.getSrcModuleName();
				long dataSize = tuple.getCloudletFileSize();
				if (moduleName.contains("sensor")) continue;
				unexecutedEvents.put(moduleName, unexecutedEvents.get(moduleName) + dataSize);
			}
		}
	}

	private void setTuplesProcessedPerAppModule() {
		Map<String, Integer> tuplesProcessedPerModule = new HashMap<>();
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppModule module : app.getModules()){
				tuplesProcessedPerModule.put(module.getName(), module.getTotalTuplesProcessed());
			}
		}
		metrics.setTuplesProcessedPerModule(tuplesProcessedPerModule);
	}

	private void setTotalTuplesSentBySensorDevices() {
		int sumTuples = sensors.stream()
				.mapToInt(Sensor::getTotalTuplesSent)
				.sum();
		metrics.setTuplesSentBySensors(sumTuples);
	}

	private void setNetworkUsage() {
		metrics.setNetworkUsage(NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME);
	}

	private FogDevice getCloud(){
		for(FogDevice dev : getFogDevices())
			if(dev.getName().equals("cloud"))
				return dev;
		return null;
	}


	private void setEnergyConsumptionPerDevice() {
		Map<String, Double> energyConsumptionPerDevice = new HashMap<>();
		for(FogDevice fogDevice : getFogDevices()){
			energyConsumptionPerDevice.put(fogDevice.getName(), fogDevice.getEnergyConsumption());
		}
		metrics.setEnergyConsumptionPerDevice(energyConsumptionPerDevice);
	}

	private String getStringForLoopId(int loopId){
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
	}

	private void setExecutionTime() {
		metrics.setExecutionTime(Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime());
	}

	private void setTupleTypeLatencies() {
		Map<String, Double> tupleTypeLatencies = new HashMap<>();
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()){
			tupleTypeLatencies.put(tupleType, TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
		}
		metrics.setLatencyPerTupleType(tupleTypeLatencies);
	}

	// In this implementation of the system only one AppLoop will exist
	private void setAppLoopLatency() {
		double loopLatency = 0.0;
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()){
			loopLatency = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
		}
		metrics.setAppLoopLatency(loopLatency);
	}

	protected void manageResources(){
		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
	}
	
	private void processTupleFinished(SimEvent ev) {
	}
	
	@Override
	public void shutdownEntity() {	
	}
	
	public void submitApplication(Application application, int delay, ModulePlacement modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(Sensor sensor : sensors){
			sensor.setApp(getApplications().get(sensor.getAppId()));
		}
		for(Actuator ac : actuators){
			ac.setApp(getApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(Actuator actuator : getActuators()){
					if(actuator.getActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(Application application, ModulePlacement modulePlacement){
		submitApplication(application, 0, modulePlacement);
	}
	
	
	private void processAppSubmit(SimEvent ev){
		Application app = (Application) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(Application application){
		System.out.println(CloudSim.clock()+" Submitted application "+ application.getAppId());
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		
		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		for(FogDevice fogDevice : fogDevices){
			sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
		}
		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
		for(Integer deviceId : deviceToModuleMap.keySet()){
			for(AppModule module : deviceToModuleMap.get(deviceId)){
				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
			}
		}
	}

	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, Application> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		for(Sensor sensor : sensors)
			sensor.setControllerId(getId());
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}
}