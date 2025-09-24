package uk.cam.mrc.phm;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import io.SpeedsReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;
import routing.*;
import routing.components.Gradient;
import routing.components.JctStress;
import routing.components.LinkAmbience;
import routing.components.LinkStress;
import uk.cam.mrc.phm.util.MelbourneImplementationConfig;
import uk.cam.mrc.phm.util.CoefficientLookup;
import static uk.cam.mrc.phm.util.CoefficientLookup.CoefficientSet;

import java.util.*;
import java.util.function.ToDoubleFunction;

import static uk.cam.mrc.phm.util.MelbourneImplementationConfig.getMelbourneProperties;
import static uk.cam.mrc.phm.util.parseMEL.getHoursAsSeconds;

public class RunMatsimActiveMode {

    private static final Logger logger = LogManager.getLogger(RunMatsimActiveMode.class);
    static java.util.Properties properties = getMelbourneProperties();

    static String ACTIVE_SPEEDS = properties.getProperty("ACTIVE_SPEEDS");
    static String MATSIM_NETWORK = properties.getProperty("MATSIM_NETWORK");
    static String MATSIM_PLAN =  properties.getProperty("MATSIM_PLAN");

    private static final List<Day> MATSIM_DAY =new ArrayList<>(Collections.singleton(Day.thursday));

    public static void main(String[] args) {
        logger.info("Started the Microsimulation Transport Orchestrator (MITO) based on 2017 models");

        // Initialize coefficient lookup table once at startup
        logger.info("Initialising coefficient lookup table for efficient processing...");
        CoefficientLookup.initialise();
        logger.info("Coefficient lookup initialised: {}", CoefficientLookup.getStatistics());

        MitoModelMEL model = MitoModelMEL.standAloneModel(args[0], MelbourneImplementationConfig.get());
        //model.run();
        final DataSet dataSet = model.getData();

        // boolean runAssignment = Resources.instance.getBoolean(Properties.RUN_TRAFFIC_ASSIGNMENT, false);
        boolean runAssignment = true;
        if (runAssignment) {
            Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
            new PopulationReader(scenario).readFile(MATSIM_PLAN);

            //Load population by mode by day
            Map<Day, Population> populationBikePedByDay = new HashMap<>();
            Map<Day, Population> populationCarByDay = new HashMap<>();

            MainModeIdentifierImpl mainModeIdentifier = new MainModeIdentifierImpl();
            for (Person person : scenario.getPopulation().getPersons().values()){
                Day day = Day.valueOf((String)person.getAttributes().getAttribute("day"));
                String mode = mainModeIdentifier.identifyMainMode(TripStructureUtils.getLegs(person.getSelectedPlan()));
                switch (mode) {
                    case "car":
                        populationCarByDay.computeIfAbsent(day, p -> PopulationUtils.createPopulation(ConfigUtils.createConfig())).addPerson(person);
                        break;
                    case "bike":
                    case "walk":
                        populationBikePedByDay.computeIfAbsent(day, p -> PopulationUtils.createPopulation(ConfigUtils.createConfig())).addPerson(person);
                        break;
                    default:
                }
            }


            //initial bike, ped simulation config
            Config bikePedConfig = ConfigUtils.createConfig();
            bikePedConfig.addModule(new BicycleConfigGroup());
            bikePedConfig.addModule(new WalkConfigGroup());
            fillBikePedConfig(bikePedConfig);


            //simulate bikePed by day
            for (Day day : MATSIM_DAY) {
                logger.info("Starting {} MATSim simulation", day.toString().toUpperCase());
                String outputSubDirectory = "scenOutput/" + model.getScenarioName() + "/" + dataSet.getYear();
                bikePedConfig.controller().setOutputDirectory(Resources.instance.getBaseDirectory().toString() + "/" + outputSubDirectory + "/trafficAssignment/" + day + "/bikePed/");
                bikePedConfig.controller().setRunId(String.valueOf(dataSet.getYear()));


                //initialize scenario
                MutableScenario matsimScenario = (MutableScenario) ScenarioUtils.loadScenario(bikePedConfig);
                matsimScenario.setPopulation(populationBikePedByDay.get(day));
                logger.info("total population {} | Bike Walk: {}", day, populationBikePedByDay.get(day).getPersons().size());

                // set vehicles
                EnumMap<Mode, EnumMap<MitoGender, Map<Integer,Double>>> allSpeeds = SpeedsReader.readData(ACTIVE_SPEEDS);
                VehiclesFactory fac = VehicleUtils.getFactory();
                for(MitoGender gender : MitoGender.values()) {
                    for(int age = 0 ; age <= 100 ; age++) {
                        VehicleType walk = fac.createVehicleType(Id.create(TransportMode.walk + gender + age, VehicleType.class));
                        walk.setMaximumVelocity(allSpeeds.get(Mode.walk).get(gender).get(age));
                        walk.setNetworkMode(TransportMode.walk);
                        walk.setPcuEquivalents(0.);
                        matsimScenario.getVehicles().addVehicleType(walk);

                        VehicleType bicycle = fac.createVehicleType(Id.create(TransportMode.bike + gender + age, VehicleType.class));
                        bicycle.setMaximumVelocity(allSpeeds.get(Mode.bicycle).get(gender).get(age));
                        bicycle.setNetworkMode(TransportMode.bike);
                        bicycle.setPcuEquivalents(0.);
                        matsimScenario.getVehicles().addVehicleType(bicycle);
                    }
                }

                // Create vehicle for each person (i.e., trip)
                for(Person person : matsimScenario.getPopulation().getPersons().values()) {
                    MitoGender gender = (MitoGender) person.getAttributes().getAttribute("sex");
                    int age = (int) person.getAttributes().getAttribute("age");
                    String mode = (String) person.getAttributes().getAttribute("mode");
                    Id<Vehicle> vehicleId = Id.createVehicleId(person.getId().toString());
                    VehicleType vehicleType = matsimScenario.getVehicles().getVehicleTypes().get(Id.create(mode + gender + age, VehicleType.class));
                    Vehicle veh = fac.createVehicle(vehicleId,vehicleType);
                    Map<String,Id<Vehicle>> modeToVehicle = new HashMap<>();
                    modeToVehicle.put(mode,vehicleId);
                    VehicleUtils.insertVehicleIdsIntoPersonAttributes(person,modeToVehicle);
                    matsimScenario.getVehicles().addVehicle(veh);
                }

                matsimScenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
                matsimScenario.getConfig().qsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.teleport);
                matsimScenario.getConfig().qsim().setUsePersonIdForMissingVehicleId(true);
//                VehicleUtils.writeVehicles(matsimScenario.getVehicles(),"testActiveVehicles.xml");

                // Create active mode networks
                Network activeNetwork = extractModeSpecificNetwork(MATSIM_NETWORK,new HashSet<>(Arrays.asList(TransportMode.bike, TransportMode.walk)));

                matsimScenario.setNetwork(activeNetwork);
//                NetworkUtils.writeNetwork(activeNetwork, "F:\\models\\silo_manchester\\input/mito/trafficAssignment/network_active_cleaned.xml");
                //ConfigUtils.writeMinimalConfig(matsimScenario.getConfig(),"F:\\models\\silo_manchester\\input/mito/trafficAssignment/config_min.xml");

                //set up controler
                final Controler controlerBikePed = new Controler(matsimScenario);
                controlerBikePed.addOverridingModule(new WalkModule());
                controlerBikePed.addOverridingModule(new BicycleModule());


                controlerBikePed.run();
            }
        }
    }

    private static void fillBikePedConfig(Config bikePedConfig) {
        // set input file and basic controler settings
        bikePedConfig.controller().setLastIteration(0);
        bikePedConfig.controller().setWritePlansInterval(Math.max(bikePedConfig.controller().getLastIteration(), 1));
        bikePedConfig.controller().setWriteEventsInterval(Math.max(bikePedConfig.controller().getLastIteration(), 1));
        bikePedConfig.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        // set qsim - passingQ
        bikePedConfig.qsim().setFlowCapFactor(1.);
        bikePedConfig.qsim().setStorageCapFactor(1.);
        bikePedConfig.qsim().setEndTime(24*60*60);
        bikePedConfig.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

        // set routing modes
        List<String> mainModeList = new ArrayList<>();
        mainModeList.add(TransportMode.bike);
        mainModeList.add(TransportMode.walk);
        bikePedConfig.qsim().setMainModes(mainModeList);
        bikePedConfig.routing().setNetworkModes(mainModeList);
        bikePedConfig.routing().removeTeleportedModeParams("bike");
        bikePedConfig.routing().removeTeleportedModeParams("walk");
        bikePedConfig.routing().removeTeleportedModeParams("pt");


        // BIKE ATTRIBUTES
        List<ToDoubleFunction<Link>> bikeAttributes = new ArrayList<>();
        bikeAttributes.add(l -> Math.max(Math.min(Gradient.getGradient(l),0.5),0.));
        bikeAttributes.add(l -> LinkStress.getStress(l,TransportMode.bike));

        // Bicycle config group
        BicycleConfigGroup bicycle = (BicycleConfigGroup) bikePedConfig.getModules().get(BicycleConfigGroup.GROUP_NAME);
        bicycle.setAttributes(bikeAttributes);
        bicycle.setWeights(RunMatsimActiveMode::calculateBikeWeights);

        // WALK ATTRIBUTES
        List<ToDoubleFunction<Link>> walkAttributes = new ArrayList<>();
        walkAttributes.add(l -> Math.max(0.,0.81 - LinkAmbience.getVgviFactor(l)));
        walkAttributes.add(l -> Math.min(1.,l.getFreespeed() / 22.35));
        walkAttributes.add(l -> JctStress.getStressProp(l,TransportMode.walk));

        // Walk config group
        WalkConfigGroup walkConfigGroup = (WalkConfigGroup) bikePedConfig.getModules().get(WalkConfigGroup.GROUP_NAME);
        walkConfigGroup.setAttributes(walkAttributes);
        walkConfigGroup.setWeights(RunMatsimActiveMode::calculateWalkWeights);

        ActivityParams homeActivity = new ActivityParams("home").setTypicalDuration(getHoursAsSeconds(12));
        bikePedConfig.scoring().addActivityParams(homeActivity);

        ActivityParams workActivity = new ActivityParams("work").setTypicalDuration(getHoursAsSeconds(8));
        bikePedConfig.scoring().addActivityParams(workActivity);

        ActivityParams educationActivity = new ActivityParams("education").setTypicalDuration(getHoursAsSeconds(8));
        bikePedConfig.scoring().addActivityParams(educationActivity);

        ActivityParams shoppingActivity = new ActivityParams("shopping").setTypicalDuration(getHoursAsSeconds(1));
        bikePedConfig.scoring().addActivityParams(shoppingActivity);

        ActivityParams recreationActivity = new ActivityParams("recreation").setTypicalDuration(getHoursAsSeconds(1));
        bikePedConfig.scoring().addActivityParams(recreationActivity);

        ActivityParams otherActivity = new ActivityParams("other").setTypicalDuration(getHoursAsSeconds(1));
        bikePedConfig.scoring().addActivityParams(otherActivity);

        ActivityParams airportActivity = new ActivityParams("airport").setTypicalDuration(getHoursAsSeconds(1));
        bikePedConfig.scoring().addActivityParams(airportActivity);

        bikePedConfig.transit().setUsingTransitInMobsim(false);
        bikePedConfig.controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.Dijkstra);

    }

    public static double[] calculateActiveModeWeights(String mode, Person person) {
        double grad = 0.0;
        double stressLink = 0.0;
        double vgvi = 0.0;
        double speed = 0.0;

        MitoGender gender = (MitoGender) person.getAttributes().getAttribute("sex");
        int age = (int) person.getAttributes().getAttribute("age");
        Purpose purpose = (Purpose) person.getAttributes().getAttribute("purpose");
        CoefficientSet coeffs = CoefficientLookup.getCoefficients(purpose, mode);

        // Base coefficients
        grad += coeffs.grad;
        stressLink += coeffs.stressLink;
        vgvi += coeffs.vgvi;
        speed += coeffs.speed;

        if (age >= 16 && gender.equals(MitoGender.FEMALE)) {
            grad += coeffs.grad_f;
            stressLink += coeffs.stressLink_f;
            vgvi += coeffs.vgvi_f;
            speed += coeffs.speed_f;
        }

        if (age < 16) {
            grad += coeffs.grad_c;
            stressLink += coeffs.stressLink_c;
            vgvi += coeffs.vgvi_c;
            speed += coeffs.speed_c;
        }

        return new double[] {grad, stressLink, vgvi, speed};
    }

    public static double[] calculateBikeWeights(Person person) {
        return calculateActiveModeWeights("bike", person);
    }

    public static double[] calculateWalkWeights(Person person) {
        return calculateActiveModeWeights("walk", person);
    }

    public static Network extractModeSpecificNetwork(String networkFile, Set<String> transportModes) {

        Network network = NetworkUtils.readNetwork(networkFile);
        Network modeSpecificNetwork = NetworkUtils.createNetwork();

        new TransportModeNetworkFilter(network).filter(modeSpecificNetwork, transportModes);
        NetworkUtils.runNetworkCleaner(modeSpecificNetwork);
        return modeSpecificNetwork;
    }
}
