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
import uk.cam.mrc.phm.modules.CyclingShareAdjustment;

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
    private final Module personTripAssignmentMandatory;
    private final Module travelTimeBudgetMandatory;
    private final Module distributionMandatory;
    private final Module tripGenerationDiscretionary;
    private final Module personTripAssignmentDiscretionary;
    private final Module travelTimeBudgetDiscretionary;
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
            Module personTripAssignmentMandatory,
            Module travelTimeBudgetMandatory,
            Module distributionMandatory,
            Module tripGenerationDiscretionary,
            Module personTripAssignmentDiscretionary,
            Module travelTimeBudgetDiscretionary,
            Module modeSetChoice,
            Module distributionDiscretionary,
            Module modeChoice,
            Module dayOfWeekChoice,
            Module timeOfDayChoice,
            Module tripScaling,
            Module matsimPopulationGenerator,
            Module longDistanceTraffic){

        this.dataSet = dataSet;
        this.tripGenerationMandatory = tripGenerationMandatory;
        this.personTripAssignmentMandatory = personTripAssignmentMandatory;
        this.travelTimeBudgetMandatory = travelTimeBudgetMandatory;
        this.distributionMandatory = distributionMandatory;
        this.tripGenerationDiscretionary = tripGenerationDiscretionary;
        this.personTripAssignmentDiscretionary = personTripAssignmentDiscretionary;
        this.travelTimeBudgetDiscretionary = travelTimeBudgetDiscretionary;
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
        private Module personTripAssignmentDiscretionary;
        private Module travelTimeBudgetDiscretionary;
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
            tripGenerationMandatory = new TripGeneration(
                    dataSet,
                    mandatoryPurposes,
                    new MitoTripFactory7days()
            );
            mandatoryPurposes.forEach(
        purpose -> (
                    (TripGeneration) tripGenerationMandatory
                ).registerTripGenerator(
                    purpose, new
                    MitoTripFactory7days(),
                    TripGeneratorType.PersonBasedHurdlePolr,
                    new TripGenCalculatorMEL(dataSet),
                    new AttractionCalculatorMEL(dataSet,purpose)
                )
            );

            distributionMandatory = new TripDistribution(dataSet, mandatoryPurposes);
            mandatoryPurposes.forEach(purpose -> (
                    (TripDistribution) distributionMandatory
            ).registerDestinationUtilityCalculator(
                    purpose,
                    new DestinationUtilityCalculatorMEL(purpose)
                )
            );

            tripGenerationDiscretionary = new TripGeneration(dataSet, discretionaryPurposes, new MitoTripFactory7days());
            discretionaryPurposes.forEach(
        purpose -> (
                        (TripGeneration) tripGenerationDiscretionary
                ).registerTripGenerator(
                        purpose,
                        new MitoTripFactory7days(),
                        TripGeneratorType.PersonBasedHurdleNegBin,
                        new TripGenCalculatorMEL(dataSet),
                        new AttractionCalculatorMEL(dataSet,purpose)
                )
            );

            if(Resources.instance.getBoolean(Properties.RUN_MODESET,false)) {
                modeSetChoice = new ModeSetChoice(dataSet, purposes, new ModeSetCalculatorMEL(dataSet));
            }

            distributionDiscretionary = new TripDistribution(dataSet, discretionaryPurposes);
            // Register ALL purposes here, because we need the mandatory purpose matrices for NHBW / NHBO
            purposes.forEach(purpose -> ((TripDistribution) distributionDiscretionary).registerDestinationUtilityCalculator(purpose, new DestinationUtilityCalculatorMEL(purpose)));
            //Override the calculator for RRT, because the categorisePerson is different
            ((TripDistribution) distributionDiscretionary).registerDestinationUtilityCalculator(RRT, new DestinationUtilityCalculatorRrtMEL(RRT));

            modeChoice = new ModeChoice(dataSet, purposes);
            ModeChoiceCalibrationData calibrationData = dataSet.getModeChoiceCalibrationData();
            purposes.forEach(purpose -> (
                    (ModeChoice) modeChoice).registerModeChoiceCalculator(
                            purpose,
                            new CalibratingModeChoiceCalculatorImpl(
                                    new ModeChoiceCalculatorMEL(purpose, dataSet),
                                    calibrationData
                            )
                    )
            );
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
                    personTripAssignmentMandatory,
                    travelTimeBudgetMandatory,
                    distributionMandatory,
                    tripGenerationDiscretionary,
                    personTripAssignmentDiscretionary,
                    travelTimeBudgetDiscretionary,
                    modeSetChoice,
                    distributionDiscretionary,
                    modeChoice,
                    dayOfWeekChoice,
                    timeOfDayChoice,
                    tripScaling,
                    matsimPopulationGenerator,
                    longDistanceTraffic);
        }

        public void setTripGeneration(Module tripGeneration) {
            this.tripGenerationMandatory = tripGeneration;
        }

        public void setPersonTripAssignment(Module personTripAssignment) {
            this.personTripAssignmentMandatory = personTripAssignment;
        }

        public void setTravelTimeBudget(Module travelTimeBudget) {
            this.travelTimeBudgetMandatory = travelTimeBudget;
        }

        public void setDistribution(Module distribution) {
            this.distributionMandatory = distribution;
        }

        public void setModeChoice(Module modeChoice) {
            this.modeChoice = modeChoice;
        }

        public void setTimeOfDayChoice(Module timeOfDayChoice) {
            this.timeOfDayChoice= timeOfDayChoice;
        }

        public void setTripScaling(Module tripScaling) {
            this.tripScaling = tripScaling;
        }

        public void setMatsimPopulationGenerator(Module matsimPopulationGenerator) {
            this.matsimPopulationGenerator = matsimPopulationGenerator;
        }

        public void setLongDistanceTraffic(Module longDistanceTraffic) {
            this.longDistanceTraffic = longDistanceTraffic;
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

        calibrateDiscretionaryDistanceDistribution(); // Completed 2025-08-25

        checkForNullDestinations(dataSet);

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


        // If enabled, increase the overall cycling share by converting the highest propensity trips to bicycle
        if (Resources.instance.getBoolean(Properties.RUN_CYCLING_SHARE_ADJUSTMENT, false)) {
            double targetShare = Resources.instance.getDouble(Properties.CYCLING_SHARE_TARGET, 0.20);
            logger.info("Running Module: Cycling Share Adjustment");
            new CyclingShareAdjustment(dataSet, targetShare).run();
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

    private void calibrateDiscretionaryDistanceDistribution() {
        // Updates the initial values (see DestinationUtilityCalculatorMEL) based on sociodemographic categories from the VISTA 2012-20 dataset
        // Trip distribution median calibration restricts adjustment range from 0.5 to 2 (if median is below this range, it is set to 0.5, if above to 2, else the median is used
        // To allow greater flexibility across categories, the mean calibration is used instead
        logger.info("Calibrating discretionary trip distribution using mean values");


//        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBW,
//                new double[]{7.72, 12.22, 16.70, 11.09, 11.23}, false);
//
//        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBE,
//                new double[]{4.97, 15.70, 8.41, 5.61, 9.60}, false);

//        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBA,
//                new double[]{5.35, 6.25, 6.46, 4.84, 8.12}, false);

//        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBS,
//                new double[]{6.37, 5.74, 5.66, 5.21, 4.35}, false);

//        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBR,
//                new double[]{7.17, 6.81, 6.35, 6.51, 6.03}, false);

//        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBO,
//                new double[]{11.44, 11.33, 12.01, 11.46, 9.96}, false);

//        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.NHBW,
//                new double[]{6.85, 9.63, 8.02, 5.75, 3.93}, false);

//        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.NHBO,
//                new double[]{6.78, 8.13, 8.08, 6.89, 7.08}, false);

    }

    public void checkForNullDestinations(DataSet dataSet) {
        long nullDestCount = dataSet.getTrips().values().stream()
                .filter(trip -> trip.getTripDestination() == null)
                .count();
        if (nullDestCount > 0) {
            logger.warn("Found {} trips with null destinations.", nullDestCount);
            dataSet.getTrips().values().stream()
                    .filter(trip -> trip.getTripDestination() == null)
                    .forEach(trip -> logger.warn("Null destination for trip: Purpose={}, Origin={}, PersonId={}",
                            trip.getTripPurpose(),
                            trip.getTripOrigin(),
                            trip.getPerson().getId()));
        }
    }

}
