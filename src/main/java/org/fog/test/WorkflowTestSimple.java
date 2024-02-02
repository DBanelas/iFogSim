package org.fog.test;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import sun.util.resources.cldr.nl.CalendarData_nl_NL;

import java.util.*;
import java.util.stream.Collectors;

public class WorkflowTestSimple {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static double TRANSMISSION_RATE = 60;
    static int NUM_SENSORS = 100;
    static int NUM_USERS = 1;
    static boolean TRACE_FLAG = false;

    public static void main(String[] args) {

        Log.printLine("Starting WorkflowTest...");
        Logger.ENABLED = true;

        try {
            Log.disable();
            // Init the CloudSim engine
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(NUM_USERS, calendar, TRACE_FLAG);

            // Set an identifier for the application and create a broker
            String appId = "workflowTest";
            FogBroker broker = new FogBroker("broker");
            int userID = broker.getId();

            //Create the application
            Application application = createApplication(appId, userID);
            application.setUserId(userID);

            // Create the physical topology
            createTestTopology(userID, appId);

            printDeviceIds(fogDevices);
            System.out.println("------------");
            printChildren(fogDevices);

            //Create a controller and the module mapping (all cloud in this case)
            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
            ModuleMapping moduleMapping = createCustomMapping(application);
            ModulePlacement placement = new ModulePlacementMapping(fogDevices, application, moduleMapping);

            //Submit application to be executed
            controller.submitApplication(application, placement);

            //Start application
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void printDeviceIds(List<FogDevice> fogDevices) {
        fogDevices.forEach(fogDevice -> System.out.println(fogDevice.getName() + " " + fogDevice.getId()));
    }

    private static void printChildren(List<FogDevice> fogDevices) {
        fogDevices.forEach(fogDevice -> {
            System.out.println("Children of " + fogDevice.getName() + ":");
            for (Integer child : fogDevice.getChildrenIds()) {
                System.out.print(child + " ");
            }
        });
    }

    private static ModuleMapping createCustomMapping(Application application) {
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
        moduleMapping.addModuleToDevice("source", "SourceDevice");
        moduleMapping.addModuleToDevice("map", "MapDevice");
        moduleMapping.addModuleToDevice("filter", "FilterDevice");
        moduleMapping.addModuleToDevice("sink", "SinkDevice");
        return moduleMapping;
    }

//    private static ModuleMapping createAllCloudModuleMapping(Application app) {
//        // Create a new ModuleMapping instance
//        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
//
//        // Iterate over all module names in the application
//        app.getModuleNames().forEach(moduleName -> moduleMapping.addModuleToDevice(moduleName, "cloud"));
//
//        // Return the populated ModuleMapping instance
//        return moduleMapping;
//    }

    private static FogDevice createFogDevice(String nodeName,
                                             long mips,
                                             int ram,
                                             long upBw,
                                             long downBw,
                                             int level,
                                             double ratePerMips,
                                             double busyPower,
                                             double idlePower) {

        List<Pe> peList = new ArrayList<Pe>();

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000; // host storage
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this resource
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN devices by now

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fogdevice.setLevel(level);
        return fogdevice;
    }

    /**
     * Method to print the topology per level
     */
//    private static void printTopology() {
//        System.out.println("------------ LEVEL 0: -------------");
//        for (FogDevice fogDevice : levelToDeviceList.get(0)) {
//            System.out.println(fogDevice.getName());
//        }
//        System.out.println("------------ LEVEL 1: -------------");
//        for (FogDevice fogDevice : levelToDeviceList.get(1)) {
//            System.out.println(fogDevice.getName());
//        }
//        System.out.println("------------ LEVEL 2: -------------");
//        for (FogDevice fogDevice : levelToDeviceList.get(2)) {
//            System.out.println(fogDevice.getName());
//        }
//        System.out.println("------------ LEVEL 3: -------------");
//        for (FogDevice fogDevice : levelToDeviceList.get(3)) {
//            List<String> sensorNames = gatewayToSensorList.get(fogDevice.getName())
//                    .stream()
//                    .map(Sensor::getName)
//                    .collect(Collectors.toList());
//            System.out.println(fogDevice.getName() + " with connected sensors: " + sensorNames);
//        }
//
//    }

    private static void createTestTopology(int userID, String appID) {
        FogDevice cloud = createFogDevice("cloud", 100000, 40000, 10000, 10000, 0, 0.01, 16*103, 16*83.25);
        fogDevices.add(cloud);
        cloud.addParentID(-1);

        FogDevice sinkDevice = createFogDevice("SinkDevice", 100000, 40000, 100000, 100000, 0, 0.01, 16*103, 16*83.25);
        fogDevices.add(sinkDevice);

        FogDevice mapDevice = createFogDevice("MapDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
        fogDevices.add(mapDevice);

        FogDevice filterDevice = createFogDevice("FilterDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
        fogDevices.add(filterDevice);

        FogDevice sourceDevice = createFogDevice("SourceDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
        fogDevices.add(sourceDevice);

        sinkDevice.addParentID(-1);

        mapDevice.addParentID(sinkDevice.getId());
        mapDevice.addLatencyForParent(sinkDevice.getId(), 2.0);

        filterDevice.addParentID(sinkDevice.getId());
        filterDevice.addLatencyForParent(sinkDevice.getId(), 2.0);

        sourceDevice.addParentID(mapDevice.getId());
        sourceDevice.addLatencyForParent(mapDevice.getId(), 2.0);

        sourceDevice.addParentID(filterDevice.getId());
        sourceDevice.addLatencyForParent(filterDevice.getId(), 2.0);

        // Create sensors  to gateway device
        for (int i = 0; i < NUM_SENSORS; i++) {
            String sensorID = "sensor-" + i;
            Sensor sensor = new Sensor(sensorID, "SENSOR_TUPLE", userID, appID, new DeterministicDistribution(TRANSMISSION_RATE));
            sensor.setGatewayDeviceId(sourceDevice.getId());
            sensor.setLatency(1.0);
            sensors.add(sensor);
        }
    }


//    private static void createTestTopology(int userID, String appID) {
//        FogDevice cloud = createFogDevice("cloud", 100000, 40000, 10000, 10000, 0, 0.01, 16*103, 16*83.25);
//        fogDevices.add(cloud);
//        levelToDeviceList.put(0, new ArrayList<FogDevice>(){{add(cloud);}});
//
//        FogDevice proxy = createFogDevice("proxy", 28000, 4000, 100000, 10000, 1, 0.0, 107.339, 83.4333);
//        fogDevices.add(proxy);
//
//        FogDevice router = createFogDevice("router", 28000, 4000, 100000, 10000, 2, 0.0, 107.339, 83.4333);
//        fogDevices.add(router);
//
//        // Create a cycle with the above devices proxy -> cloud, cloud -> router, router -> proxy
//        proxy.setParentId(cloud.getId());
//        cloud.setParentId(router.getId());
//        router.setParentId(proxy.getId());
//
//        proxy.setUplinkLatency(20); // latency of connection between proxy server and cloud is 100 ms
//        cloud.setUplinkLatency(2); // latency of connection between cloud and router is 2 ms
//        router.setUplinkLatency(2); // latency of connection between router and proxy server is 2 ms
//
//        // Create sensors  to router device
//        for (int i = 0; i < NUM_SENSORS; i++) {
//            String sensorID = "sensor-" + i;
//            Sensor sensor = new Sensor(sensorID, "SENSOR_TUPLE", userID, appID, new DeterministicDistribution(TRANSMISSION_RATE));
//            sensor.setGatewayDeviceId(router.getId());
//            sensor.setLatency(1.0);
//            sensors.add(sensor);
//        }
//    }

//    private static void createFogDevices(int userID, String appID) {
//        // Create the cloud "device"
//        FogDevice cloud = createFogDevice("cloud", 10000, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
//        cloud.setParentId(-1);
//        fogDevices.add(cloud);
//        levelToDeviceList.put(0, new ArrayList<FogDevice>(){{add(cloud);}});
//
//        // Create the proxy server
//        FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 100000, 10000, 1, 0.0, 107.339, 83.4333);
//        proxy.setParentId(cloud.getId());
//        proxy.setUplinkLatency(20); // latency of connection between proxy server and cloud is 100 ms
//        fogDevices.add(proxy);
//        levelToDeviceList.put(1, new ArrayList<FogDevice>(){{add(proxy);}});
//
//        // for each area, create a router, a gateway and NUM_OF_SENSORS_PER_AREA sensors
//        for(int i = 0; i < NUM_OF_AREAS; i++) {
//            addArea(i, userID, appID, proxy.getId());
//        }
//    }

//    private static void addArea(int id, int userID, String appID, int parentId){
//        FogDevice router = createFogDevice("router-"+id, 2800, 4000, 100000, 10000, 2, 0.0, 107.339, 83.4333);
//        router.setParentId(parentId);
//        router.setUplinkLatency(2); // latency of connection between router and proxy server is 2 ms
//        fogDevices.add(router);
//        levelToDeviceList.computeIfAbsent(2, k -> new ArrayList<>()).add(router);
//        // adding a fog device for every Gateway in physical topology
//        // Each gateway has one or more sensors attached to it
//        // The parent of each gateway is the proxy server
//        FogDevice gateway = addSensorGroup(id, appID, userID);
//        gateway.setParentId(router.getId());
//        gateway.setUplinkLatency(2); // latency of connection between the gateway and proxy server is 2 ms
//        fogDevices.add(gateway);
//
//    }

//    private static FogDevice addSensorGroup(int groupID, String appID, int userID) {
//        FogDevice gateway = createFogDevice("gateway-" + groupID, 2800, 4000, 100000, 10000, 3, 0.0, 107.339, 83.4333);
//        levelToDeviceList.computeIfAbsent(3, k -> new ArrayList<>()).add(gateway);
//        for (int i = 0; i < NUM_OF_SENSORS_PER_AREA; i++) {
//            String sensorID = "s-" + groupID + "-" + i;
//            int tupleGroupID = groupID + 1;
//            Sensor sensor = new Sensor(sensorID, "SENSOR_TUPLE_" + tupleGroupID, userID, appID, new DeterministicDistribution(TRANSMISSION_RATE));
//            sensor.setGatewayDeviceId(gateway.getId());
//            sensor.setLatency(1.0);
//            sensors.add(sensor);
//            gatewayToSensorList.computeIfAbsent(gateway.getName(), k -> new ArrayList<>()).add(sensor);
//        }
//
//        return gateway;
//    }

    private static Application createApplication(String appId, int userId){
        Application application = Application.createApplication(appId, userId);
        // Adding modules to the appliction graph
        application.addAppModule("source", 10);
        application.addAppModule("map", 10);
        application.addAppModule("filter", 10);
        application.addAppModule("sink", 10);

        // Connecting the application modules (vertices) in the application model (directed graph) with edges
        application.addAppEdge("SENSOR_TUPLE", "source", 1000, 2000, "SENSOR_TUPLE", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("source", "map", 1000, 2000, "MAP_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("source", "filter", 1000, 2000, "FILTER_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("map", "sink", 1000, 2000, "SINK_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("filter", "sink", 1000, 2000, "SINK_TUPLE", Tuple.UP, AppEdge.MODULE);

        // Defining the input-output relationships (represented by selectivity) of the application modules.
        application.addTupleMapping("source", "SENSOR_TUPLE", "MAP_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("source", "SENSOR_TUPLE", "FILTER_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("map", "MAP_TUPLE", "SINK_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("filter", "FILTER_TUPLE", "SINK_TUPLE", new FractionalSelectivity(1.0));

        List<AppLoop> loops = getAppLoops();
        application.setLoops(loops);
        return application;
    }

    private static List<AppLoop> getAppLoops() {
        ArrayList<String> loop1Modules = new ArrayList<String>() {{
            add("source");
            add("map");
            add("sink");
        }};

        ArrayList<String> loop2Modules = new ArrayList<String>() {{
            add("source");
            add("filter");
            add("sink");
        }};

        AppLoop loop1 = new AppLoop(loop1Modules);
        AppLoop loop2 = new AppLoop(loop2Modules);
        List<AppLoop> loops = new ArrayList<>();
        loops.add(loop1);
        loops.add(loop2);
        return loops;
    }
}
