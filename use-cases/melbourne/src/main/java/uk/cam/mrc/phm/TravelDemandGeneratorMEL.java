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


        //      Running trip distribution calibration
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBW,
                new double[] {3.44,4.43,9.61,8.19,12.54,13.18,11.31,5.98,8.61,7.51},false);
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBE,
                new double[] {2.43,2.70,9.55,12.38,3.68,4.56,3.10,4.11,5.10,6.70},false);
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBS,
                new double[] {3.62,3.54,2.26,2.91,2.64,2.96,3.04,2.97,2.43,2.46},false);
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBO,
                new double[] {6.88,5.69,8.32,6.10,7.35,6.47,6.91,5.91,4.29,5.00},false);
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBR,
                new double[] {3.35,3.23,2.54,2.64,1.85,2.22,2.18,2.26,1.91,2.22},false);
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.NHBO,
                new double[] {3.18,3.36,1.96,3.98,3.25,3.74,2.91,3.31,2.83,3.27},false);
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.NHBW,
                new double[] {4.24,4.97,3.20,4.50,2.95,3.51,3.00,3.87,4.20,2.71},false);
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.RRT,
                new double[] {3.35,3.23,2.54,2.64,1.85,2.22,2.18,2.26,1.91,2.22},false);
        ((TripDistribution) distributionDiscretionary).calibrate(Purpose.HBA,
                new double[] {2.86,2.86,3.44,3.55,3.08,3.47,2.56,2.75,5.27,4.24},false);

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
