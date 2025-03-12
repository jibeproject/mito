package de.tum.bgu.msm.trafficAssignment;

import de.tum.bgu.msm.data.Mode;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.util.HashSet;
import java.util.Set;

public class ConfigureMatsim {

    public static Config configureMatsim() {



        //String outputDirectory = outputDirectoryRoot + "/" + runId + "/";
        //matsimConfig.controller().setRunId(runId);
        //matsimConfig.controller().setOutputDirectory(outputDirectory);
        Config config = ConfigUtils.createConfig();
        config.controller().setFirstIteration(0);
        config.controller().setMobsim("qsim");
        config.controller().setWritePlansInterval(config.controller().getLastIteration());
        config.controller().setWriteEventsInterval(config.controller().getLastIteration());
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        config.qsim().setEndTime(26 * 3600);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.vspExperimental().setWritingOutputEvents(true); // writes final events into toplevel directory

        {
            ReplanningConfigGroup.StrategySettings strategySettings = new ReplanningConfigGroup.StrategySettings();
            strategySettings.setStrategyName("ChangeExpBeta");
            strategySettings.setWeight(0.8);
            config.replanning().addStrategySettings(strategySettings);
        }
        {
            ReplanningConfigGroup.StrategySettings strategySettings = new ReplanningConfigGroup.StrategySettings();
            strategySettings.setStrategyName("ReRoute");
            strategySettings.setWeight(0.2);
            config.replanning().addStrategySettings(strategySettings);
        }
//        {
//            config.timeAllocationMutator().setMutationRange(1800);
//            config.timeAllocationMutator().setAffectingDuration(true);
//            ReplanningConfigGroup.StrategySettings strategySettings = new ReplanningConfigGroup.StrategySettings();
//            strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute);
//            strategySettings.setWeight(0.1);
//            config.replanning().addStrategySettings(strategySettings);
//        }

        config.replanning().setFractionOfIterationsToDisableInnovation(0.8);
        config.replanning().setMaxAgentPlanMemorySize(4);

        ScoringConfigGroup.ActivityParams homeActivity = new ScoringConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 60 * 60);
        config.scoring().addActivityParams(homeActivity);

        ScoringConfigGroup.ActivityParams workActivity = new ScoringConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(8 * 60 * 60);
        config.scoring().addActivityParams(workActivity);

        ScoringConfigGroup.ActivityParams educationActivity = new ScoringConfigGroup.ActivityParams("education");
        educationActivity.setTypicalDuration(8 * 60 * 60);
        config.scoring().addActivityParams(educationActivity);

        ScoringConfigGroup.ActivityParams shoppingActivity = new ScoringConfigGroup.ActivityParams("shopping");
        shoppingActivity.setTypicalDuration(1 * 60 * 60);
        config.scoring().addActivityParams(shoppingActivity);

        ScoringConfigGroup.ActivityParams recreationActivity = new ScoringConfigGroup.ActivityParams("recreation");
        recreationActivity.setTypicalDuration(1 * 60 * 60);
        config.scoring().addActivityParams(recreationActivity);

        ScoringConfigGroup.ActivityParams otherActivity = new ScoringConfigGroup.ActivityParams("other");
        otherActivity.setTypicalDuration(1 * 60 * 60);
        config.scoring().addActivityParams(otherActivity);

        ScoringConfigGroup.ActivityParams airportActivity = new ScoringConfigGroup.ActivityParams("airport");
        airportActivity.setTypicalDuration(1 * 60 * 60);
        config.scoring().addActivityParams(airportActivity);

        RoutingConfigGroup.ModeRoutingParams carPassengerParams = new RoutingConfigGroup.ModeRoutingParams("car_passenger");
        carPassengerParams.setTeleportedModeFreespeedFactor(1.0);
        config.routing().addModeRoutingParams(carPassengerParams);

        RoutingConfigGroup.ModeRoutingParams ptParams = new RoutingConfigGroup.ModeRoutingParams("pt");
        ptParams.setBeelineDistanceFactor(1.5);
        ptParams.setTeleportedModeSpeed(50 / 3.6);
        config.routing().addModeRoutingParams(ptParams);

        RoutingConfigGroup.ModeRoutingParams bicycleParams = new RoutingConfigGroup.ModeRoutingParams("bike");
        bicycleParams.setBeelineDistanceFactor(1.3);
        bicycleParams.setTeleportedModeSpeed(15 / 3.6);
        config.routing().addModeRoutingParams(bicycleParams);

        RoutingConfigGroup.ModeRoutingParams walkParams = new RoutingConfigGroup.ModeRoutingParams("walk");
        walkParams.setBeelineDistanceFactor(1.3);
        walkParams.setTeleportedModeSpeed(5 / 3.6);
        config.routing().addModeRoutingParams(walkParams);

        String runId = "mito_assignment";
        config.controller().setRunId(runId);
        config.network().setInputFile(Resources.instance.getBaseDirectory().toString() + "/" + Resources.instance.getString(Properties.MATSIM_NETWORK_FILE));

        config.qsim().setNumberOfThreads(16);
        config.global().setNumberOfThreads(16);
        config.global().setNumberOfThreads(16);
        //config.qsim().setUsingThreadpool(false); removed for compatibility with 14.0

        config.controller().setLastIteration(Resources.instance.getInt(Properties.MATSIM_ITERATIONS));
        config.controller().setWritePlansInterval(config.controller().getLastIteration());
        config.controller().setWriteEventsInterval(config.controller().getLastIteration());

        config.qsim().setStuckTime(10);

        double siloSamplingFactor = Resources.instance.getDouble(Properties.SP_SCALING_FACTOR, 1.0) *
                Resources.instance.getDouble(Properties.SCALE_FACTOR_FOR_TRIP_GENERATION, 1.0);
        config.qsim().setFlowCapFactor(siloSamplingFactor * Double.parseDouble(Resources.instance.getString(Properties.TRIP_SCALING_FACTOR)));
        config.qsim().setStorageCapFactor(siloSamplingFactor * Double.parseDouble(Resources.instance.getString(Properties.TRIP_SCALING_FACTOR)));


        String[] networkModes = Resources.instance.getArray(Properties.MATSIM_NETWORK_MODES, new String[]{"autoDriver"});
        Set<String> networkModesSet = new HashSet<>();

        for (String mode : networkModes) {
            String matsimMode = Mode.getMatsimMode(Mode.valueOf(mode));
            if (!networkModesSet.contains(matsimMode)) {
                networkModesSet.add(matsimMode);
            }
        }

        config.routing().setNetworkModes(networkModesSet);

        return config;
    }



    public static void setDemandSpecificConfigSettings(Config config) {

        double siloSamplingFactor = Resources.instance.getDouble(Properties.SP_SCALING_FACTOR, 1.0) *
                Resources.instance.getDouble(Properties.SCALE_FACTOR_FOR_TRIP_GENERATION, 1.0);
        config.qsim().setFlowCapFactor(siloSamplingFactor * Double.parseDouble(Resources.instance.getString(Properties.TRIP_SCALING_FACTOR)));
        config.qsim().setStorageCapFactor(siloSamplingFactor * Double.parseDouble(Resources.instance.getString(Properties.TRIP_SCALING_FACTOR)));

        ScoringConfigGroup.ActivityParams homeActivity = new ScoringConfigGroup.ActivityParams("home");
        homeActivity.setTypicalDuration(12 * 60 * 60);
        config.scoring().addActivityParams(homeActivity);

        ScoringConfigGroup.ActivityParams workActivity = new ScoringConfigGroup.ActivityParams("work");
        workActivity.setTypicalDuration(8 * 60 * 60);
        config.scoring().addActivityParams(workActivity);

        ScoringConfigGroup.ActivityParams educationActivity = new ScoringConfigGroup.ActivityParams("education");
        educationActivity.setTypicalDuration(8 * 60 * 60);
        config.scoring().addActivityParams(educationActivity);

        ScoringConfigGroup.ActivityParams shoppingActivity = new ScoringConfigGroup.ActivityParams("shopping");
        shoppingActivity.setTypicalDuration(1 * 60 * 60);
        config.scoring().addActivityParams(shoppingActivity);

        ScoringConfigGroup.ActivityParams recreationActivity = new ScoringConfigGroup.ActivityParams("recreation");
        recreationActivity.setTypicalDuration(1 * 60 * 60);
        config.scoring().addActivityParams(recreationActivity);

        ScoringConfigGroup.ActivityParams otherActivity = new ScoringConfigGroup.ActivityParams("other");
        otherActivity.setTypicalDuration(1 * 60 * 60);
        config.scoring().addActivityParams(otherActivity);

        ScoringConfigGroup.ActivityParams airportActivity = new ScoringConfigGroup.ActivityParams("airport");
        airportActivity.setTypicalDuration(1 * 60 * 60);
        config.scoring().addActivityParams(airportActivity);
    }
}
