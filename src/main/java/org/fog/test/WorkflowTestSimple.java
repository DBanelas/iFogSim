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

import java.util.*;
import java.util.stream.Collectors;

public class WorkflowTestSimple {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static HashMap<Integer, ArrayList<FogDevice>> levelToDeviceList = new HashMap<>();
    static HashMap<String, ArrayList<Sensor>> gatewayToSensorList = new HashMap<>();
    static double TRANSMISSION_RATE = 1;
    static int NUM_OF_SENSORS_PER_AREA = 10;
    static int NUM_OF_AREAS = 1;
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
            createFogDevices(userID, appId);

            //Create a controller and the module mapping (all cloud in this case)
            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
            ModuleMapping moduleMapping = createAllCloudModuleMapping(application);
            ModulePlacement placement = new ModulePlacementMapping(fogDevices, application, moduleMapping);

            //Submit application to be executed
            controller.submitApplication(application, placement);
            printTopology();

            //Start application
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static ModuleMapping createAllCloudModuleMapping(Application app) {
        // Create a new ModuleMapping instance
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

        // Iterate over all module names in the application
        app.getModuleNames().forEach(moduleName -> moduleMapping.addModuleToDevice(moduleName, "cloud"));

        // Return the populated ModuleMapping instance
        return moduleMapping;
    }

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

    private static void createFogDevices(int userID, String appID) {
        // Create the cloud "device"
        FogDevice cloud = createFogDevice("cloud", 10000, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
        cloud.setParentId(-1);
        fogDevices.add(cloud);
        levelToDeviceList.put(0, new ArrayList<FogDevice>(){{add(cloud);}});

        // Create the proxy server
        FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
        proxy.setParentId(cloud.getId());
        proxy.setUplinkLatency(20); // latency of connection between proxy server and cloud is 100 ms
        fogDevices.add(proxy);
        levelToDeviceList.put(1, new ArrayList<FogDevice>(){{add(proxy);}});

        // for each area, create a router, a gateway and NUM_OF_SENSORS_PER_AREA sensors
        for(int i = 0; i < NUM_OF_AREAS; i++) {
            addArea(i, userID, appID, proxy.getId());
        }
    }

    private static void addArea(int id, int userID, String appID, int parentId){
        FogDevice router = createFogDevice("router-"+id, 2800, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333);
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
        FogDevice gateway = createFogDevice("gateway-" + groupID, 2800, 4000, 100000, 10000, 3, 0.0, 107.339, 83.4333);
        levelToDeviceList.computeIfAbsent(3, k -> new ArrayList<>()).add(gateway);
        for (int i = 0; i < NUM_OF_SENSORS_PER_AREA; i++) {
            String sensorID = "s-" + groupID + "-" + i;
            int tupleGroupID = groupID + 1;
            Sensor sensor = new Sensor(sensorID, "SENSOR_TUPLE_" + tupleGroupID, userID, appID, new DeterministicDistribution(TRANSMISSION_RATE));
            sensor.setGatewayDeviceId(gateway.getId());
            sensor.setLatency(1.0);
            sensors.add(sensor);
            gatewayToSensorList.computeIfAbsent(gateway.getName(), k -> new ArrayList<>()).add(sensor);
        }

        return gateway;
    }

    private static Application createApplication(String appId, int userId){
        Application application = Application.createApplication(appId, userId);
        // Adding modules to the appliction graph
        application.addAppModule("source1", 10);
        application.addAppModule("filter1", 10);

        // Connecting the application modules (vertices) in the application model (directed graph) with edges
        application.addAppEdge("SENSOR_TUPLE_1", "source1", 1000, 20000, "SENSOR_TUPLE_1", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("source1", "filter1", 1000, 20000, "FILTER_TUPLE_1", Tuple.UP, AppEdge.MODULE);

        // Defining the input-output relationships (represented by selectivity) of the application modules.
        application.addTupleMapping("source1", "SENSOR_TUPLE_1", "FILTER_TUPLE_1", new FractionalSelectivity(1.0));
        application.addTupleMapping("filter1", "FILTER_TUPLE_1", "JOIN_TUPLE", new FractionalSelectivity(1.0));

        List<AppLoop> loops = getAppLoops();
        application.setLoops(loops);
        return application;
    }

    private static List<AppLoop> getAppLoops() {
        AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("s-0-1");add("source1");add("filter1");}});
        List<AppLoop> loops = new ArrayList<>();
        loops.add(loop1);
        return loops;
    }
}
