package de.tum.bgu.msm.scenarios.drtNoise;

import de.tum.bgu.msm.TravelDemandGenerator;
import de.tum.bgu.msm.data.DataSetImpl;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.data.travelTimes.SkimTravelTimes;
import de.tum.bgu.msm.io.input.readers.*;
import de.tum.bgu.msm.modules.modeChoice.ModeChoice;
import de.tum.bgu.msm.modules.modeChoice.CalibratingModeChoiceCalculatorImpl;
import de.tum.bgu.msm.modules.ModeChoiceCalculatorImpl;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.ImplementationConfig;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;

import java.util.Random;

public class MitoModelDrt {

    private static final Logger logger = LogManager.getLogger(MitoModelDrt.class);
    private final String scenarioName;

    private DataSetImpl dataSet;
    private Geometry serviceArea;

    private MitoModelDrt(DataSetImpl dataSet, String scenarioName, Geometry serviceArea) {
        this.dataSet = dataSet;
        this.scenarioName = scenarioName;
        this.serviceArea = serviceArea;
        MitoUtil.initializeRandomNumber();
    }

    public static MitoModelDrt standAloneModel(String propertiesFile, ImplementationConfig config, Geometry serviceArea) {
        logger.info(" Creating standalone version of MITO ");
        Resources.initializeResources(propertiesFile);
        MitoModelDrt model = new MitoModelDrt(new DataSetImpl(), Resources.instance.getString(Properties.SCENARIO_NAME), serviceArea);
        model.readStandAlone(config);
        return model;
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        logger.info("Started the Microsimulation Transport Orchestrator (MITO)");

        final TravelDemandGenerator.Builder builder = new TravelDemandGenerator.Builder(dataSet);
        final ModeChoice modeChoice = (ModeChoice) builder.getModeChoice();
        for(Purpose purpose: Purpose.values()) {
            if(purpose != Purpose.AIRPORT) {
                modeChoice.registerModeChoiceCalculator(purpose,
                        new DrtTopNestModeChoiceCalculatorImpl(new CalibratingModeChoiceCalculatorImpl(new ModeChoiceCalculatorImpl(),
                                dataSet.getModeChoiceCalibrationData()), serviceArea));
//                modeChoice.registerModeChoiceCalculator(purpose, new ModeChoiceCalculatorImpl());
            }
        }
        TravelDemandGenerator ttd = builder.build();
        ttd.generateTravelDemand(scenarioName);
        printOutline(startTime);
    }

    private void readStandAlone(ImplementationConfig config) {
        dataSet.setYear(Resources.instance.getInt(Properties.SCENARIO_YEAR));
        new ZonesReader(dataSet).read();
        if (Resources.instance.getBoolean(Properties.REMOVE_TRIPS_AT_BORDER)) {
            new BorderDampersReader(dataSet).read();
        }
        new JobReader(dataSet, config.getJobTypeFactory()).read();
        new SchoolsReader(dataSet).read();
        new HouseholdsReader(dataSet).read();
        new HouseholdsCoordReader(dataSet).read();
        new PersonsReader(dataSet).read();
        dataSet.setTravelTimes(new SkimTravelTimes());
        new OmxSkimsReader(dataSet).read();
        readAdditionalData();
    }

    private void readAdditionalData() {
        new ModeChoiceInputReader(dataSet).read();
        new EconomicStatusReader(dataSet).read();
        new CalibrationDataReader(dataSet).read();
        new CalibrationRegionMapReader(dataSet).read();

    }

    private void printOutline(long startTime) {
        String trips = MitoUtil.customFormat("  " + "###,###", dataSet.getTrips().size());
        logger.info("A total of " + trips.trim() + " microscopic trips were generated");
        logger.info("Completed the Microsimulation Transport Orchestrator (MITO)");
        float endTime = MitoUtil.rounder(((System.currentTimeMillis() - startTime) / 60000.f), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes.");
    }

    public DataSetImpl getData() {
        return dataSet;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setRandomNumberGenerator(Random random) {
        MitoUtil.initializeRandomNumber(random);
    }
}


