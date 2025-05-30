package de.tum.bgu.msm.scenarios.drtNoise;

import de.tum.bgu.msm.trafficAssignment.ConfigureMatsim;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class ReIterateCroppedPlans {

    public static final double SCALE = 1.7;

    public static void main(String[] args) {

        final Config config = ConfigUtils.createConfig();
        ConfigureMatsim.setDemandSpecificConfigSettings(config);
        String outputSubDirectory = "D:\\resultStorage\\moia-msm\\cleverShuttleOperationArea\\iterateCropped0107_70percent";
        config.controller().setOutputDirectory(outputSubDirectory);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        config.qsim().setFlowCapFactor(SCALE);
        config.qsim().setStorageCapFactor(SCALE);
        config.qsim().setNumberOfThreads(16);
        config.global().setNumberOfThreads(16);
        config.global().setNumberOfThreads(16);
//        config.qsim().setUsingThreadpool(true);

        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(100);
        config.controller().setMobsim("qsim");
        config.controller().setWritePlansInterval(25);
        config.controller().setWriteEventsInterval(25);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        config.qsim().setEndTime(28 * 3600);
        //config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
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
//            ReplanningConfigGroup.StrategySettings strategySettings = new ReplanningConfigGroup.StrategySettings();
//            strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute);
//            strategySettings.setWeight(0.05);
//            config.replanning().addStrategySettings(strategySettings);
//        }

//        config.timeAllocationMutator().setMutationRange(1200);


        config.replanning().setFractionOfIterationsToDisableInnovation(0.85);
        config.replanning().setMaxAgentPlanMemorySize(5);


        MutableScenario matsimScenario = ScenarioUtils.createMutableScenario(config);
        new PopulationReader(matsimScenario).readFile("D:\\resultStorage\\moia-msm\\cleverShuttleOperationArea\\50.plans.xml.gz");

        final Population oldPop = matsimScenario.getPopulation();
        Population newPop = PopulationUtils.createPopulation(config);

        final WorstPlanForRemovalSelector worstPlanForRemovalSelector = new WorstPlanForRemovalSelector();
        for (Person next : oldPop.getPersons().values()) {
            if (Math.random() < 1) {

                if (TripStructureUtils.getLegs(next.getSelectedPlan()).get(0).getMode().equals(TransportMode.car)) {
                    newPop.addPerson(next);

                }
            }
            /*if(next.getPlans().size() > 4) {
                final Plan plan = worstPlanForRemovalSelector.selectPlan(next);
                next.removePlan(plan);
            }*/
        }

        matsimScenario.setPopulation(newPop);

        Network network = NetworkUtils.createNetwork();
        //  new MatsimNetworkReader(network).readFile("D:\\resultStorage\\moia-msm\\cleverShuttleOperationArea\\croppedDenseNetworkExtended.xml.gz");
//        new MatsimNetworkReader(network).readFile("C:\\Users\\Nico\\tum\\fabilut\\gitproject\\muc\\input\\mito\\trafficAssignment\\studyNetworkDense.xml");
        new MatsimNetworkReader(network).readFile("D:\\resultStorage\\moia-msm\\cleverShuttleOperationArea\\networkUpdated.xml");

        matsimScenario.setNetwork(network);

        Controler controler = new Controler(matsimScenario);
        controler.run();


    }
}
