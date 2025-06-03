package uk.cam.mrc.phm;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.io.output.*;
import de.tum.bgu.msm.modules.DayOfWeekChoice;
import de.tum.bgu.msm.modules.MatsimPopulationGenerator7days;
import de.tum.bgu.msm.modules.ModeSetChoice;
import de.tum.bgu.msm.modules.Module;
import de.tum.bgu.msm.modules.modeChoice.CalibratingModeChoiceCalculatorImpl;
import de.tum.bgu.msm.modules.modeChoice.ModeChoice;
import de.tum.bgu.msm.modules.modeChoice.ModeChoiceCalibrationData;
import de.tum.bgu.msm.modules.plansConverter.externalFlows.LongDistanceTraffic;
import de.tum.bgu.msm.modules.scaling.TripScaling;
import de.tum.bgu.msm.modules.tripDistribution.TripDistribution;
import de.tum.bgu.msm.modules.tripGeneration.TripGeneration;
import de.tum.bgu.msm.modules.tripGeneration.TripGeneratorType;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.cam.mrc.phm.calculators.*;
import uk.cam.mrc.phm.io.SummarizeData7daysMEL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.tum.bgu.msm.data.Purpose.RRT;
import static de.tum.bgu.msm.data.Purpose.getListedPurposes;

/**
 * Generates travel demand for the Microscopic Transport Orchestrator (MITO)
 *
 * @author Rolf Moeckel
 * Created on Sep 18, 2016 in Munich, Germany
 */
public final class TravelDemandGeneratorMEL {

    private static final Logger logger = LogManager.getLogger(TravelDemandGeneratorMEL.class);

    private final DataSet dataSet;

    private final Module tripGenerationMandatory;
    private final Module distributionMandatory;
    private final Module tripGenerationDiscretionary;
    private final Module modeSetChoice;
    private final Module distributionDiscretionary;
    private final Module modeChoice;
    private final Module dayOfWeekChoice;
    private final Module timeOfDayChoice;
    private final Module tripScaling;
    private final Module matsimPopulationGenerator;
    private final Module longDistanceTraffic;

    private TravelDemandGeneratorMEL(
            DataSet dataSet,
            Module tripGenerationMandatory,
            Module distributionMandatory,
            Module tripGenerationDiscretionary,
            Module modeSetChoice,
            Module distributionDiscretionary,
            Module modeChoice,
            Module dayOfWeekChoice,
            Module timeOfDayChoice,
            Module tripScaling,
            Module matsimPopulationGenerator,
            Module longDistanceTraffic) {

        this.dataSet = dataSet;
        this.tripGenerationMandatory = tripGenerationMandatory;
        this.distributionMandatory = distributionMandatory;
        this.tripGenerationDiscretionary = tripGenerationDiscretionary;
        this.distributionDiscretionary = distributionDiscretionary;
        this.modeChoice = modeChoice;
        this.dayOfWeekChoice = dayOfWeekChoice;
        this.timeOfDayChoice = timeOfDayChoice;
        this.tripScaling = tripScaling;
        this.matsimPopulationGenerator = matsimPopulationGenerator;
        this.longDistanceTraffic = longDistanceTraffic;
        this.modeSetChoice = modeSetChoice;
    }


    public static class Builder {

        private final DataSet dataSet;

        private Module tripGenerationMandatory;
        private Module personTripAssignmentMandatory;
        private Module travelTimeBudgetMandatory;
        private Module distributionMandatory;
        private Module tripGenerationDiscretionary;
        private Module modeSetChoice;
        private Module distributionDiscretionary;
        private Module modeChoice;
        private Module dayOfWeekChoice;
        private Module timeOfDayChoice;
        private Module tripScaling;
        private Module matsimPopulationGenerator;
        private Module longDistanceTraffic;


        public Builder(DataSet dataSet) {
            this.dataSet = dataSet;

            List<Purpose> purposes = Purpose.getListedPurposes(Resources.instance.getString(Properties.TRIP_PURPOSES));
            logger.info("Simulating trips for the following purposes: {}", purposes.stream().map(Enum::toString).collect(Collectors.joining(",")));

            List<Purpose> mandatoryPurposes = new ArrayList<>(purposes);
            mandatoryPurposes.retainAll(Purpose.getMandatoryPurposes());
            List<Purpose> discretionaryPurposes = new ArrayList<>(purposes);
            discretionaryPurposes.removeAll(mandatoryPurposes);

            //from here
            tripGenerationMandatory = new TripGeneration(dataSet, mandatoryPurposes, new MitoTripFactory7days());
            mandatoryPurposes.forEach(purpose -> ((TripGeneration) tripGenerationMandatory).registerTripGenerator(purpose, new MitoTripFactory7days(), TripGeneratorType.PersonBasedHurdlePolr,new TripGenCalculatorMEL(dataSet),
                    new AttractionCalculatorMEL(dataSet,purpose)));

            distributionMandatory = new TripDistribution(dataSet, mandatoryPurposes);
            mandatoryPurposes.forEach(purpose -> ((TripDistribution) distributionMandatory).registerDestinationUtilityCalculator(purpose, new DestinationUtilityCalculatorMEL(purpose)));

            tripGenerationDiscretionary = new TripGeneration(dataSet, discretionaryPurposes, new MitoTripFactory7days());
            discretionaryPurposes.forEach(purpose -> ((TripGeneration) tripGenerationDiscretionary).registerTripGenerator(purpose, new MitoTripFactory7days(),TripGeneratorType.PersonBasedHurdleNegBin,new TripGenCalculatorMEL(dataSet),
                    new AttractionCalculatorMEL(dataSet,purpose)));

            if(Resources.instance.getBoolean(Properties.RUN_MODESET,false)) {
                modeSetChoice = new ModeSetChoice(dataSet, purposes, new ModeSetCalculatorMEL(dataSet));
            }

            distributionDiscretionary = new TripDistribution(dataSet, discretionaryPurposes);
            // Register ALL purposes here, because we need the mandatory purpose matrices for NHBW / NHBO
            purposes.forEach(purpose -> ((TripDistribution) distributionDiscretionary).registerDestinationUtilityCalculator(purpose, new DestinationUtilityCalculatorMEL(purpose)));
            //Override the calculator for RRT, because the categorisePerson is different
            ((TripDistribution) distributionDiscretionary).registerDestinationUtilityCalculator(RRT, new DestinationUtilityCalculatorRrtMEL(RRT));

            modeChoice = new ModeChoice(dataSet, purposes);
            purposes.forEach(purpose -> ((ModeChoice) modeChoice).registerModeChoiceCalculator(purpose, new CalibratingModeChoiceCalculatorImpl(new ModeChoiceCalculatorMEL(purpose, dataSet), dataSet.getModeChoiceCalibrationData())));
            //Override the calculator for RRT
            ((ModeChoice) modeChoice).registerModeChoiceCalculator(RRT, new CalibratingModeChoiceCalculatorImpl(new ModeChoiceCalculatorRrtMEL(dataSet), dataSet.getModeChoiceCalibrationData()));

            dayOfWeekChoice = new DayOfWeekChoice(dataSet, purposes);

            timeOfDayChoice = new TimeOfDayChoiceMEL(dataSet, purposes);
            //until here it must be divided into two blocks - mandatory and discretionary

            tripScaling = new TripScaling(dataSet, purposes);
            matsimPopulationGenerator = new MatsimPopulationGenerator7days(dataSet, purposes);
            if (Resources.instance.getBoolean(Properties.ADD_EXTERNAL_FLOWS, false)) {
                longDistanceTraffic = new LongDistanceTraffic(dataSet, Double.parseDouble(Resources.instance.getString(Properties.TRIP_SCALING_FACTOR)), purposes);
            }
        }

        public TravelDemandGeneratorMEL build() {
            return new TravelDemandGeneratorMEL(dataSet,
                    tripGenerationMandatory,
                    distributionMandatory,
                    tripGenerationDiscretionary,
                    modeSetChoice,
                    distributionDiscretionary,
                    modeChoice,
                    dayOfWeekChoice,
                    timeOfDayChoice,
                    tripScaling,
                    matsimPopulationGenerator,
                    longDistanceTraffic);
        }

        public DataSet getDataSet() {
            return dataSet;
        }

        public Module getTripGeneration() {
            return tripGenerationMandatory;
        }

        public Module getPersonTripAssignment() {
            return personTripAssignmentMandatory;
        }

        public Module getTravelTimeBudget() {
            return travelTimeBudgetMandatory;
        }

        public Module getDistribution() {
            return distributionMandatory;
        }

        public Module getModeChoice() {
            return modeChoice;
        }

        public Module getTimeOfDayChoice() {
            return timeOfDayChoice;
        }

        public Module getTripScaling() {
            return tripScaling;
        }

        public Module getMatsimPopulationGenerator() {
            return matsimPopulationGenerator;
        }

        public Module getLongDistanceTraffic() {
            return longDistanceTraffic;
        }
    }

    public void generateTravelDemand(String scenarioName) {

        logger.info("Running Module: Microscopic Trip Generation");

        tripGenerationMandatory.run();

        logger.info("Running Module: Microscopic Trip Distribution");
        distributionMandatory.run();

        tripGenerationDiscretionary.run();

        if(Resources.instance.getBoolean(Properties.RUN_MODESET,false)) {
            logger.info("Running Module: Mode set choice");
            modeSetChoice.run();
        }

        logger.info("Running Module: Microscopic Trip Distribution");
        distributionDiscretionary.run();

        logger.info("Running Module: Trip to Mode Assignment (Mode Choice)");
        modeChoice.run();

        // MODE CHOICE CALIBRATION CODE
        if(Resources.instance.getBoolean(Properties.RUN_CALIBRATION_MC,false)) {
            int modeChoiceCalibrationIterations = Resources.instance.getInt(Properties.MC_CALIBRATION_ITERATIONS, 0);
            if (modeChoiceCalibrationIterations > 0) {
                ModeChoiceCalibrationData modeChoiceCalibrationData = dataSet.getModeChoiceCalibrationData();
                for (int i = 1; i <= modeChoiceCalibrationIterations; i++) {
                    modeChoiceCalibrationData.updateCalibrationCoefficients(dataSet, i,getListedPurposes(Resources.instance.getString(Properties.TRIP_PURPOSES)));
                    modeChoice.run();
                }
                modeChoiceCalibrationData.close();
            }
        }

        logger.info("Running day of week choice");
        dayOfWeekChoice.run();

        logger.info("Running time of day choice");
        timeOfDayChoice.run();

        logger.info("Running trip scaling");
        tripScaling.run();

        matsimPopulationGenerator.run();

        if (Resources.instance.getBoolean(Properties.ADD_EXTERNAL_FLOWS, false)) {
            longDistanceTraffic.run();
        }

        TripGenerationWriter.writeTripsByPurposeAndZone(dataSet, scenarioName);
        SummarizeDataToVisualize.writeFinalSummary(dataSet, scenarioName);

        if (Resources.instance.getBoolean(Properties.PRINT_MICRO_DATA, true)) {
            SummarizeData7daysMEL.writeOutSyntheticPopulationWithTrips(dataSet);
            SummarizeData7daysMEL.writeAllTrips(dataSet,scenarioName);
        }
        if (Resources.instance.getBoolean(Properties.CREATE_CHARTS, true)) {
            DistancePlots.writeDistanceDistributions(dataSet, scenarioName);
            ModeChoicePlots.writeModeChoice(dataSet, scenarioName);
            SummarizeData7daysMEL.writeCharts(dataSet, scenarioName);
        }
        if (Resources.instance.getBoolean(Properties.WRITE_MATSIM_POPULATION, true)) {
            SummarizeData7daysMEL.writeMatsimPlans(dataSet, scenarioName);
        }
    }
}
