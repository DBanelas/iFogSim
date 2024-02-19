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
import org.fog.utils.*;
import org.fog.utils.distribution.DeterministicDistribution;
import java.util.*;

public class WorkflowTestSimple {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static double TRANSMISSION_RATE = 60;
    static int NUM_SENSORS = 100;
    static int NUM_USERS = 1;
    static boolean TRACE_FLAG = false;

    public static void main(String[] args) {
        Config.setMaxSimulationTime(1000);
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

            MetricsExporter exporter = new ConsoleMetricsExporter();
            //Create a controller and the module mapping (all cloud in this case)
            Controller controller = new Controller("master-controller", fogDevices,
                    sensors, actuators, exporter);
            ModuleMapping moduleMapping = createAllCloudModuleMapping(application);
            ModulePlacement placement = new ModulePlacementMapping(fogDevices, application, moduleMapping);

            //Submit application to be executed
            controller.submitApplication(application, placement);

            //Start application
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            CloudSim.startSimulation();
//            CloudSim.stopSimulation();

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

    private static ModuleMapping createAllCloudModuleMapping(Application app) {
        // Create a new ModuleMapping instance
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

        // Iterate over all module names in the application
        app.getModuleNames().forEach(moduleName -> moduleMapping.addModuleToDevice(moduleName, "cloud"));

        // Return the populated ModuleMapping instance
        return moduleMapping;
    }

//    private static ModuleMapping createCustomMapping(Application application) {
//        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
//        moduleMapping.addModuleToDevice("source", "SourceDevice");
//        moduleMapping.addModuleToDevice("senML", "SenMLDevice");
//        moduleMapping.addModuleToDevice("rangeFilter", "RangeFilterDevice");
//        moduleMapping.addModuleToDevice("bloomFilter", "CsVDevice");
//        moduleMapping.addModuleToDevice("interpolation", "InterpolationDevice");
//        moduleMapping.addModuleToDevice("join", "JoinDevice");
//        moduleMapping.addModuleToDevice("annotate", "AnnotateDevice");
//        moduleMapping.addModuleToDevice("azure", "AzureDevice");
//        moduleMapping.addModuleToDevice("csv", "CsVDevice");
//        moduleMapping.addModuleToDevice("mqtt", "MQTTDevice");
//        moduleMapping.addModuleToDevice("sink", "SinkDevice");
//        return moduleMapping;
//    }

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
        int bw = 100000;

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

    private static void createTestTopology(int userID, String appID) {
        FogDevice cloud = createFogDevice("cloud", 1000000, 40000, 10000, 10000, 0, 0.01, 16*103, 16*83.25);
        fogDevices.add(cloud);
        cloud.addParentIdWithLatency(-1, -1);

//        FogDevice sinkDevice = createFogDevice("SinkDevice", 100000, 40000, 100000, 100000, 0, 0.01, 16*103, 16*83.25);
//        fogDevices.add(sinkDevice);
//
//        FogDevice mqttDevice = createFogDevice("MQTTDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(mqttDevice);
//
//        FogDevice csvDevice = createFogDevice("CsVDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(csvDevice);
//
//        FogDevice azureDevice = createFogDevice("AzureDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(azureDevice);
//
//        FogDevice annotateDevice = createFogDevice("AnnotateDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(annotateDevice);
//
//        FogDevice joinDevice = createFogDevice("JoinDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(joinDevice);
//
//        FogDevice interpolationDevice = createFogDevice("InterpolationDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(interpolationDevice);

        //bloom filter is in the same device as senML
//        FogDevice bloomFilterDevice = createFogDevice("BloomFilterDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(bloomFilterDevice);

//        FogDevice rangeFilterDevice = createFogDevice("RangeFilterDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(rangeFilterDevice);
//
//        FogDevice senMLDevice = createFogDevice("SenMLDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(senMLDevice);
//
//        FogDevice sourceDevice = createFogDevice("SourceDevice", 60000, 4000, 100000, 100000, 3, 0.0, 107.339, 83.4333);
//        fogDevices.add(sourceDevice);


//        sourceDevice.addParentIdWithLatency(senMLDevice.getId(), 2.0);
//
//
//
//        senMLDevice.addParentIdWithLatency(rangeFilterDevice.getId(), 1.0);
//        rangeFilterDevice.addParentIdWithLatency(csvDevice.getId(), 1.0);
//
//        csvDevice.addParentIdWithLatency(mqttDevice.getId(), 1.0);
//        csvDevice.addParentIdWithLatency(annotateDevice.getId(), 1.0);
//        csvDevice.addParentIdWithLatency(interpolationDevice.getId(), 1.0);
//
//        interpolationDevice.addParentIdWithLatency(joinDevice.getId(), 1.0);
//        joinDevice.addParentIdWithLatency(annotateDevice.getId(), 1.0);
//        annotateDevice.addParentIdWithLatency(azureDevice.getId(), 1.0);
//        annotateDevice.addParentIdWithLatency(csvDevice.getId(), 1.0);
//        mqttDevice.addParentIdWithLatency(sinkDevice.getId(), 1.0);
//        azureDevice.addParentIdWithLatency(sinkDevice.getId(), 1.0);
//
//        sinkDevice.addParentIdWithLatency(-1, -1);

        // Create sensors  to gateway device
        for (int i = 0; i < NUM_SENSORS; i++) {
            String sensorID = "sensor-" + i;
            Sensor sensor = new Sensor(sensorID, "SENSOR_TUPLE", userID, appID, new DeterministicDistribution(TRANSMISSION_RATE));
            sensor.setGatewayDeviceId(cloud.getId());
            sensor.setLatency(1.0);
            sensors.add(sensor);
        }
    }

    private static Application createApplication(String appId, int userId){
        Application application = Application.createApplication(appId, userId);
        // Adding modules to the appliction graph
        application.addAppModule("source", 10);
        application.addAppModule("senML", 10);
        application.addAppModule("rangeFilter", 10);
        application.addAppModule("bloomFilter", 10);
        application.addAppModule("interpolation", 10);
        application.addAppModule("join", 10);
        application.addAppModule("annotate", 10);
        application.addAppModule("azure", 10);
        application.addAppModule("csv", 10);
        application.addAppModule("mqtt", 10);
        application.addAppModule("sink", 10);

        // Connecting the application modules (vertices) in the application model (directed graph) with edges
        application.addAppEdge("SENSOR_TUPLE", "source", 1000, 2000, "SENSOR_TUPLE", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("source", "senML", 1000, 2000, "SENML_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("senML", "rangeFilter", 1000, 2000, "RANGE_FILTER_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("rangeFilter", "bloomFilter", 1000, 2000, "BLOOM_FILTER_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("bloomFilter", "interpolation", 1000, 2000, "INTERPOLATION_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("interpolation", "join", 1000, 2000, "JOIN_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("join", "annotate", 1000, 2000, "ANNOTATE_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("annotate", "azure", 1000, 2000, "AZURE_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("annotate", "csv", 1000, 2000, "CSV_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("csv", "mqtt", 1000, 2000, "MQTT_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mqtt", "sink", 1000, 2000, "SINK_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("azure", "sink", 1000, 2000, "SINK_TUPLE", Tuple.UP, AppEdge.MODULE);

        // Defining the input-output relationships (represented by selectivity) of the application modules.
        application.addTupleMapping("source", "SENSOR_TUPLE", "SENML_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("senML", "SENML_TUPLE", "RANGE_FILTER_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("rangeFilter", "RANGE_FILTER_TUPLE", "BLOOM_FILTER_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("bloomFilter", "BLOOM_FILTER_TUPLE", "INTERPOLATION_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("interpolation", "INTERPOLATION_TUPLE", "JOIN_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("join", "JOIN_TUPLE", "ANNOTATE_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("annotate", "ANNOTATE_TUPLE", "AZURE_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("annotate", "ANNOTATE_TUPLE", "CSV_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("csv", "CSV_TUPLE", "MQTT_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("mqtt", "MQTT_TUPLE", "SINK_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("azure", "AZURE_TUPLE", "SINK_TUPLE", new FractionalSelectivity(1.0));

        List<AppLoop> loops = getAppLoops();
        application.setLoops(loops);
        return application;
    }

    private static List<AppLoop> getAppLoops() {
        ArrayList<String> loop1Modules = new ArrayList<String>() {{
            add("source");
            add("senML");
            add("rangeFilter");
            add("bloomFilter");
            add("interpolation");
            add("join");
            add("annotate");
            add("azure");
            add("csv");
            add("mqtt");
            add("sink");
        }};

        ArrayList<String> loop2Modules = new ArrayList<String>() {{
            add("source");
            add("senML");
            add("rangeFilter");
            add("bloomFilter");
            add("interpolation");
            add("join");
            add("annotate");
            add("azure");
            add("sink");
        }};

        AppLoop loop1 = new AppLoop(loop2Modules);
        AppLoop loop2 = new AppLoop(loop1Modules);
        List<AppLoop> loops = new ArrayList<>();
        loops.add(loop1);
        loops.add(loop2);
        return loops;
    }
}
