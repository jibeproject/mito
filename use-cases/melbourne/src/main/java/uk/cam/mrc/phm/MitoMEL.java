package uk.cam.mrc.phm;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.Day;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.trafficAssignment.CarSkimUpdater;
import de.tum.bgu.msm.trafficAssignment.ConfigureMatsim;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import uk.cam.mrc.phm.util.MelbourneImplementationConfig;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static uk.cam.mrc.phm.util.MelbourneImplementationConfig.getMelbournePropertiesFile;

public class MitoMEL {

    private static final Logger logger = LogManager.getLogger(MitoMEL.class);

    public static void main(String[] args) {
        logger.info("Started the Microsimulation Transport Orchestrator (MITO) based on 2017 models");
        java.util.Properties mitoMelbourneProperties = getMelbournePropertiesFile(args[0]);
        String scenarioName = mitoMelbourneProperties.getProperty(Properties.SCENARIO_NAME);
        String scenarioYear = mitoMelbourneProperties.getProperty(Properties.SCENARIO_YEAR);
        logger.info("Scenario: {}; Year: {}", scenarioName, scenarioYear);
        MitoModelMEL model = MitoModelMEL.standAloneModel(args[0], MelbourneImplementationConfig.get());
        String outputSubDirectory = "scenOutput/" + scenarioName + "/" + scenarioYear;
        model.run();
        final DataSet dataSet = model.getData();

        boolean runAssignment = Resources.instance.getBoolean(Properties.RUN_TRAFFIC_ASSIGNMENT, false);

        if (runAssignment) {
            logger.info("Running traffic assignment in MATsim");

            Config config;
            if (args.length > 1 && args[1] != null) {
                config = ConfigUtils.loadConfig(args[1]);
                ConfigureMatsim.setDemandSpecificConfigSettings(config);
            } else {
                logger.warn("Using a fallback config with default values as no initial config has been provided.");
                config = ConfigureMatsim.configureMatsim();
            }

            final EnumMap<Day, Controler> controlers = new EnumMap<>(Day.class);

            Map<Day, Population> populationByDay = new HashMap<>();

            for (Person person : dataSet.getPopulation().getPersons().values()){
                Day day = Day.valueOf((String)person.getAttributes().getAttribute("day"));
                populationByDay.computeIfAbsent(day, p -> PopulationUtils.createPopulation(ConfigUtils.createConfig())).addPerson(person);
            }

            ExecutorService executor = Executors.newFixedThreadPool(Day.values().length);
            Map<Day, Future<Controler>> futures = new EnumMap<>(Day.class);

            for (Day day : Day.values()) {
                String outputDirectory = Resources.instance.getBaseDirectory().toString() + "/" + outputSubDirectory + "/trafficAssignment/" + day.toString();
                File outputLinks = new File(outputDirectory, "output_links.csv.gz");
                File itersDir = new File(outputDirectory, "ITERS");
                int iterations = Resources.instance.getInt(Properties.MATSIM_ITERATIONS, 100);
                File iterDir = new File(itersDir, "it." + String.valueOf(iterations));

                try {
                    if (outputLinks.exists() && iterDir.exists() && iterDir.isDirectory()) {
                        logger.info("Skipping {}: output already exists ({}).", day, outputLinks.getAbsolutePath());
                        continue;
                    }
                } catch (SecurityException e) {
                    logger.error("Permission denied while checking files for {}: {}", day, e.getMessage());
                    continue;
                } catch (Exception e) {
                    logger.error("Unexpected error while checking files for {}: {}", day, e.getMessage());
                    continue;
                }
                futures.put(day, executor.submit(() -> {
                    logger.info("Starting {} MATSim simulation", day.toString().toUpperCase());
                    Config dayConfig = ConfigUtils.loadConfig(args[1]);
                    ConfigureMatsim.setDemandSpecificConfigSettings(dayConfig);
                    dayConfig.controller().setOutputDirectory(outputDirectory);
                    MutableScenario matsimScenario = (MutableScenario) ScenarioUtils.loadScenario(dayConfig);
                    matsimScenario.setPopulation(populationByDay.get(day));
                    Controler controler = new Controler(matsimScenario);
                    controler.run();
                    return controler;
                }));
            }

            // Wait for all tasks to complete and collect results
            for (Day day : futures.keySet()) {
                try {
                    controlers.put(day, futures.get(day).get());
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error running simulation for {}", day, e);
                }
            }
            executor.shutdown();

            //TODO: print seperate skim for each day of week?
            if (Resources.instance.getBoolean(Properties.PRINT_OUT_SKIM, false)) {
                CarSkimUpdater skimUpdater = new CarSkimUpdater(
                        controlers.get(Day.monday),
                        model.getData(),
                        model.getScenarioName()
                );
                skimUpdater.run();
            }
        }
    }
}

