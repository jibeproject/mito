package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.io.input.readers.ModeChoiceCoefficientReader;
import de.tum.bgu.msm.modules.modeChoice.AbstractModeChoiceCalculator;
import de.tum.bgu.msm.resources.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;

import static de.tum.bgu.msm.data.Mode.*;

public class ModeChoiceCalculatorMEL extends AbstractModeChoiceCalculator {

    private static final Logger LOGGER = LogManager.getLogger(ModeChoiceCalculatorMEL.class);
    private final DataSet dataSet;

    public ModeChoiceCalculatorMEL(Purpose purpose, DataSet dataSet) {
        super();
        coef = new ModeChoiceCoefficientReader(dataSet, purpose, Resources.instance.getModeChoiceCoefficients(purpose)).readCoefficientsForThisPurpose();
        setNests();
        this.dataSet = dataSet;
    }

    private void setNests() {
        List<Tuple<EnumSet<Mode>, Double>> nests = new ArrayList<>();
        //TODO: currently the melbourne mode choice model is MNL, no nests
        nests = null;
//        nests.add(new Tuple<>(EnumSet.of(autoDriver,autoPassenger), 0.25));
//        nests.add(new Tuple<>(EnumSet.of(train, tramOrMetro, bus, taxi), 0.25));
//        nests.add(new Tuple<>(EnumSet.of(walk,bicycle), 1.));
        super.setNests(nests);
    }

    public EnumMap<Mode, Double> calculateUtilities(Purpose purpose, MitoHousehold household, MitoPerson person, MitoZone originZone, MitoZone destinationZone, TravelTimes travelTimes, double travelDistanceAuto, double travelDistanceNMT, double peakHour_s) {
        int age = person.getAge();
        boolean male = person.getMitoGender().equals(MitoGender.MALE);
        int hhincome = household.getMonthlyIncome();
        int hhAutos = household.getAutos();

        EnumMap<Mode, Double> generalizedCosts = calculateGeneralizedCosts(purpose, household, person,
                originZone, destinationZone, travelTimes, travelDistanceAuto, travelDistanceNMT, peakHour_s);


        EnumMap<Mode, Double> utilities = new EnumMap<>(Mode.class);

        Set<Mode> availableModes = Objects.requireNonNull(((MitoPerson7days) person).getModeSet().getModesMNL());
        availableModes.retainAll(coef.keySet());

        // Restrict availability in certain cases
        if(age < 15 || (purpose.equals(Purpose.NHBO) && hhAutos == 0)) {
            availableModes.remove(autoDriver);
        }

        // Compute utility
        for (Mode mode : availableModes){
            final Map<String, Double> modeCoef = coef.get(mode);

            // Intercept
            double utility = modeCoef.get("asc");

            // Age
            if(age < 15){
                utility += modeCoef.get("age_under16");
            } else if (age < 25) {
                utility += modeCoef.get("age_16_24");
            } else if (age < 40) {
                utility += 0;
            } else if (age < 55) {
                utility += modeCoef.get("age_45_54");
            } else if (age < 70) {
                utility += modeCoef.get("age_55_64");
            } else {
                utility += modeCoef.get("age_65up");
            }

            // gender
            if (!male) {
                utility += modeCoef.get("female");
            }

            // occupation
            if (MitoOccupationStatus.WORKER.equals(person.getMitoOccupationStatus())){
                utility += modeCoef.get("occupation_worker");
            }

            // Household income
            if (hhincome < 1500) {
                utility += modeCoef.get("income_low");
            } else if (hhincome > 5000) {
                utility += modeCoef.get("income_high");
            }

            // purpose
            if (purpose.equals(Purpose.HBR)) {
                utility += modeCoef.get("recreation_trip");
            } else if (purpose.equals(Purpose.HBO)) {
                utility += modeCoef.get("other_trip");
            }

            // Household cars
            if (hhAutos == 0) {
                utility += modeCoef.get("cars_0");
            } else if(hhAutos == 2) {
                utility += modeCoef.get("cars_2");
            } else if(hhAutos > 2) {
                utility += modeCoef.get("cars_3");
            }

            // Generalised cost
            double gc = generalizedCosts.get(mode);
            utility += gc * modeCoef.get("cost");

            // Utilities
            utilities.put(mode, utility);
        }

        return utilities;
    }

    public EnumMap<Mode, Double> calculateGeneralizedCosts(Purpose purpose, MitoHousehold household, MitoPerson person, MitoZone originZone,
                                                           MitoZone destinationZone, TravelTimes travelTimes,
                                                           double travelDistanceAuto, double travelDistanceNMT, double peakHour_s) {

        double timeAutoD = travelTimes.getTravelTime(originZone, destinationZone, peakHour_s, "car"); //min
        double timeAutoP = timeAutoD;

        double timePt = travelTimes.getTravelTime(originZone, destinationZone, peakHour_s, "pt");//min

        //TODO, check Inf travel time, intrazonal or missing PT connection?
        if (timePt == Double.POSITIVE_INFINITY) {
            timePt = 9999;
        }

        // Default case: use given walk/bike skim for purpose
        String walkSkimName = null;
        String bikeSkimName = null;

        switch(purpose) {
            case HBW:
                walkSkimName = "walk_HBW";
                if (person.getMitoGender().equals(MitoGender.FEMALE)) {
                    bikeSkimName = "bike_HBW_female";
                } else {
                    bikeSkimName = "bike_HBW";
                }
                break;
            case HBE:
                walkSkimName = "walk_HBE";
                bikeSkimName = "bike_HBE";
                break;
            case HBS:
                walkSkimName = "walk_HBS";
                bikeSkimName = "bike_HBS";
                break;
            case HBR:
                if (person.getAge()<16) {
                    walkSkimName = "walk_child";
                    bikeSkimName = "bike_child";
                } else if (person.getMitoGender().equals(MitoGender.FEMALE)) {
                    walkSkimName = "walk";
                    bikeSkimName = "bike_female";

                } else {
                    walkSkimName = "walk";
                    bikeSkimName = "bike";
                }
                break;
            case HBO:
                walkSkimName = "walk_HBO";
                bikeSkimName = "bike_HBO";
                break;
            case HBA:
                bikeSkimName = "bike_HBA";
                walkSkimName = "walk";
                break;
            case NHBO:
                walkSkimName = "walk_NHBO";
                bikeSkimName = "bike_NHBO";
                break;
            case NHBW:
                walkSkimName = "walk_NHBW";
                bikeSkimName = "bike_NHBW";
                break;
            default:
                LOGGER.error("Unknown purpose " + purpose);
        }

        // Get walk and bike cost from skims
        assert walkSkimName != null;
        assert bikeSkimName != null;


        double gcWalk = travelTimes.getTravelTime(originZone, destinationZone, peakHour_s, walkSkimName);
        double gcBicycle = travelTimes.getTravelTime(originZone, destinationZone, peakHour_s, bikeSkimName);

        EnumMap<Mode, Double> generalizedCosts = new EnumMap<>(Mode.class);
        generalizedCosts.put(autoDriver, timeAutoD);
        generalizedCosts.put(autoPassenger, timeAutoP);
        generalizedCosts.put(pt, timePt);
        generalizedCosts.put(bicycle, gcBicycle);
        generalizedCosts.put(walk, gcWalk);
        return generalizedCosts;

    }

}
