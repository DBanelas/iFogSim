package org.fog.test;

import java.util.*;
import java.util.stream.Collectors;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.*;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class WorkflowTestComplex {
    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static HashMap<Integer, ArrayList<FogDevice>> levelToDeviceList = new HashMap<>();
    static HashMap<String, ArrayList<Sensor>> gatewayToSensorList = new HashMap<>();
    static double TRANSMISSION_RATE = 30;
    static int NUM_SENSORS_PER_AREA = 50;
    static int NUM_AREAS = 3;
    static int USER_ID = 1;
    static boolean TRACE_FLAG = false;
    static String APP_ID = "workflowTestComplex";

    public static void main(String[] args) throws Exception {
        initializeSimulation(true, false);
        executeSimulation();
    }

    private static void executeSimulation() {
        //Create the application
        Application application = createApplication(APP_ID, USER_ID);

        // Create the physical topology
        createFogDevices(USER_ID, APP_ID);

        //Create a controller and the module mapping (all cloud in this case)
        Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
        ModuleMapping moduleMapping = createAllCloudModuleMapping(application);
        ModulePlacement placement = new ModulePlacementMapping(fogDevices, application, moduleMapping);

        //Submit application to be executed
        controller.submitApplication(application, placement);
        printTopology();
        printPlacement(moduleMapping);

        //Start application
        TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
        CloudSim.startSimulation();
        CloudSim.stopSimulation();
    }

    private static void initializeSimulation(boolean enableiFogSimLogger,
                                       boolean enableCloudSimLogger) {
        Logger.ENABLED = enableiFogSimLogger;
        Log.setDisabled(!enableCloudSimLogger);

        // Init the CloudSim engine
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(USER_ID, calendar, TRACE_FLAG);
    }

    private static void printPlacement(ModuleMapping moduleMapping) {
        for (String deviceName : moduleMapping.getModuleMapping().keySet()) {
            System.out.println(deviceName + "contains modules: " + moduleMapping.getModuleMapping().get(deviceName));
        }
    }

    private static ModuleMapping createAllCloudModuleMapping(Application app) {
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
        app.getModules().forEach(module -> {
            moduleMapping.addModuleToDevice(module.getName(), "cloud");
        });

        return moduleMapping;
    }

//    private static ModuleMapping createAllCloudModuleMapping(Application app) {
//        // Create a new ModuleMapping instance
//        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
//        moduleMapping.addModuleToDevice("source1", "gateway-0");
//        moduleMapping.addModuleToDevice("filter1", "gateway-0");
//
//        moduleMapping.addModuleToDevice("source2", "gateway-1");
//        moduleMapping.addModuleToDevice("filter2", "gateway-1");
//
//        moduleMapping.addModuleToDevice("source3", "gateway-2");
//        moduleMapping.addModuleToDevice("filter3", "gateway-2");
//
//        moduleMapping.addModuleToDevice("join", "proxy-server");
//        moduleMapping.addModuleToDevice("union", "proxy-server");
//        moduleMapping.addModuleToDevice("sink", "proxy-server");
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

        List<Pe> peList = new ArrayList<>();

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
        LinkedList<Storage> storageList = new LinkedList<>(); // we are not adding SAN devices by now

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
    private static void printTopology() {
        System.out.println("------------ LEVEL 0: -------------");
        for (FogDevice fogDevice : levelToDeviceList.get(0)) {
            System.out.println(fogDevice.getName());
        }
        System.out.println("------------ LEVEL 1: -------------");
        for (FogDevice fogDevice : levelToDeviceList.get(1)) {
            System.out.println(fogDevice.getName());
        }
        System.out.println("------------ LEVEL 2: -------------");
        for (FogDevice fogDevice : levelToDeviceList.get(2)) {
            System.out.println(fogDevice.getName());
        }
        System.out.println("------------ LEVEL 3: -------------");
        for (FogDevice fogDevice : levelToDeviceList.get(3)) {
            List<String> sensorNames = gatewayToSensorList.get(fogDevice.getName())
                    .stream()
                    .map(Sensor::getName)
                    .collect(Collectors.toList());
            System.out.println(fogDevice.getName() + " with connected sensors: " + sensorNames);
        }

    }

    /**
     * Method to create the physical topology
     * Creates the cloud at level 0
     * Creates the proxy server at level 1
     * Creates the routers at level 2
     * Creates the gateways at level 3
     * Creates the sensors at level 4
     */
    private static void createFogDevices(int userID, String appID) {
        // Create the cloud at level 0
        FogDevice cloud = createFogDevice("cloud", 100000, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
        cloud.setParentId(-1);
        fogDevices.add(cloud);
        levelToDeviceList.put(0, new ArrayList<FogDevice>(){{add(cloud);}});

        // Create the proxy server at level 1
        FogDevice proxy = createFogDevice("proxy-server", 100000, 4000, 100000, 10000, 1, 0.0, 107.339, 83.4333);
        proxy.setParentId(cloud.getId());
        proxy.setUplinkLatency(20); // latency of connection between proxy server and cloud is 20 ms
        fogDevices.add(proxy);
        levelToDeviceList.put(1, new ArrayList<FogDevice>(){{add(proxy);}});

        // for each area, create a router, a gateway and NUM_OF_SENSORS_PER_AREA sensors
        for(int i = 0; i < NUM_AREAS; i++) {
            addArea(i, userID, appID, proxy.getId());
        }
    }

    private static void addArea(int id, int userID, String appID, int parentId){
        FogDevice router = createFogDevice("router-"+id, 100000, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333);
        router.setParentId(parentId);
        router.setUplinkLatency(2); // latency of connection between router and proxy server is 2 ms
        fogDevices.add(router);
        levelToDeviceList.computeIfAbsent(2, k -> new ArrayList<>()).add(router);

        // adding a fog device for every Gateway in physical topology
        // Each gateway has one or more sensors attached to it
        // The parent of each gateway is the proxy server
        FogDevice gateway = addSensorGroup(id, appID, userID);
        gateway.setParentId(router.getId());
        gateway.setUplinkLatency(2); // latency of connection between the gateway and proxy server is 2 ms
        fogDevices.add(gateway);

    }

    private static FogDevice addSensorGroup(int groupID, String appID, int userID) {
        FogDevice gateway = createFogDevice("gateway-" + groupID,
                100000,
                4000,
                1000,
                10000,
                3,
                0.0,
                107.339,
                83.4333);

        levelToDeviceList.computeIfAbsent(3, k -> new ArrayList<>()).add(gateway);
        for (int i = 0; i < NUM_SENSORS_PER_AREA; i++) {
            String sensorID = "s-" + groupID + "-" + i;
            int tupleGroupID = groupID + 1;
            Sensor sensor = new Sensor(sensorID, "SENSOR_TUPLE_" + tupleGroupID, userID, appID, new DeterministicDistribution(TRANSMISSION_RATE));
            sensor.setGatewayDeviceId(gateway.getId());
            sensor.setLatency(0.001);
            sensors.add(sensor);
            gatewayToSensorList.computeIfAbsent(gateway.getName(), k -> new ArrayList<>()).add(sensor);
        }
        return gateway;
    }

    private static Application createApplication(String appId, int userId){
        Application application = Application.createApplication(appId, userId);
        application.setUserId(userId);

        // Adding modules (vertices) to the application model (directed graph)
        application.addAppModule("source1", 10);
        application.addAppModule("source2", 10);
        application.addAppModule("source3", 10);
        application.addAppModule("filter1", 10);
        application.addAppModule("filter2", 10);
        application.addAppModule("filter3", 10);
        application.addAppModule("join", 100);
        application.addAppModule("union", 10);
        application.addAppModule("sink", 10);

        // Connecting the application modules (vertices) in the application model (directed graph) with edges
        application.addAppEdge("SENSOR_TUPLE_1", "source1", 1000, 2000, "SENSOR_TUPLE_1", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("SENSOR_TUPLE_2", "source2", 1000, 2000, "SENSOR_TUPLE_2", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("SENSOR_TUPLE_3", "source3", 1000, 2000, "SENSOR_TUPLE_3", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("source1", "filter1", 1000, 2000, "FILTER_TUPLE_1", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("source2", "filter2", 1000, 2000, "FILTER_TUPLE_2", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("source3", "filter3", 1000, 2000, "FILTER_TUPLE_3", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("filter1", "join", 1000, 2000, "JOIN_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("filter2", "join", 1000, 2000, "JOIN_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("join", "union", 1000, 2000, "UNION_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("filter3", "union", 1000, 2000, "UNION_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("union", "sink", 1000, 2000, "SINK_TUPLE", Tuple.UP, AppEdge.MODULE);

        // Defining the input-output relationships (represented by selectivity) of the application modules.
        application.addTupleMapping("source1", "SENSOR_TUPLE_1", "FILTER_TUPLE_1", new FractionalSelectivity(1.0));
        application.addTupleMapping("source2", "SENSOR_TUPLE_2", "FILTER_TUPLE_2", new FractionalSelectivity(1.0));
        application.addTupleMapping("source3", "SENSOR_TUPLE_3", "FILTER_TUPLE_3", new FractionalSelectivity(1.0));

        application.addTupleMapping("filter1", "FILTER_TUPLE_1", "JOIN_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("filter2", "FILTER_TUPLE_2", "JOIN_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("filter3", "FILTER_TUPLE_3", "UNION_TUPLE", new FractionalSelectivity(1.0));

        application.addTupleMapping("join", "JOIN_TUPLE", "UNION_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("union", "UNION_TUPLE", "SINK_TUPLE", new FractionalSelectivity(1.0));


        List<AppLoop> loops = getAppLoops();
        application.setLoops(loops);
        return application;
    }

    private static List<AppLoop> getAppLoops() {
        AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("s-0-1");add("source1");add("filter1");add("join");add("union");add("sink");}});
        List<AppLoop> loops = new ArrayList<>();
        loops.add(loop1);
        return loops;
    }
}