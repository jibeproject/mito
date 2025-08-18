package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.modules.tripDistribution.AbstractDestinationUtilityCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DestinationUtilityCalculatorMEL extends AbstractDestinationUtilityCalculator {

    private final static double[] TRAVEL_DISTANCE_PARAM_HBW = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private final static double IMPEDANCE_PARAM_HBW = 9;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private final static double IMPEDANCE_PARAM_HBE = 28.3;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBA = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private final static double IMPEDANCE_PARAM_HBA = 28.3;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private final static double IMPEDANCE_PARAM_HBS = 14.5;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBR = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private final static double IMPEDANCE_PARAM_HBR = 20;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBO = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private final static double IMPEDANCE_PARAM_HBO = 53;

    private final static double[] TRAVEL_DISTANCE_PARAM_RRT = {-0.12877572260971806}; //redundant?
    private final static double IMPEDANCE_PARAM_RRT = 20; //redundant?

    private final static double[] TRAVEL_DISTANCE_PARAM_NHBW = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private final static double IMPEDANCE_PARAM_NHBW = 15.1;

    private final static double[] TRAVEL_DISTANCE_PARAM_NHBO = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private final static double IMPEDANCE_PARAM_NHBO = 20;


    public DestinationUtilityCalculatorMEL(Purpose purpose) {

         switch (purpose) {
            case HBW:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBW;
                impedanceParam = IMPEDANCE_PARAM_HBW;
                break;
            case HBE:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBE;
                impedanceParam = IMPEDANCE_PARAM_HBE;
                break;
             case HBA:
                 distanceParams = TRAVEL_DISTANCE_PARAM_HBA;
                 impedanceParam = IMPEDANCE_PARAM_HBA;
                 break;
            case HBS:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBS;
                impedanceParam = IMPEDANCE_PARAM_HBS;
                break;
            case HBR:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBR;
                impedanceParam = IMPEDANCE_PARAM_HBR;
                break;
            case HBO:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBO;
                impedanceParam = IMPEDANCE_PARAM_HBO;
                break;
            case RRT:
                distanceParams = TRAVEL_DISTANCE_PARAM_RRT;
                impedanceParam = IMPEDANCE_PARAM_RRT;
                break;
            case NHBW:
                distanceParams = TRAVEL_DISTANCE_PARAM_NHBW;
                impedanceParam = IMPEDANCE_PARAM_NHBW;
                break;
            case NHBO:
                distanceParams = TRAVEL_DISTANCE_PARAM_NHBO;
                impedanceParam = IMPEDANCE_PARAM_NHBO;
                break;
            case AIRPORT:
            default:
                throw new RuntimeException("not implemented!");
        }
    }

    @Override
    public List<Predicate<MitoPerson>> getCategories() {
        Predicate<MitoPerson> hasNoCar    = p -> p.getHousehold().getAutos() == 0;
        Predicate<MitoPerson> hasCar      = p -> p.getHousehold().getAutos() > 0;

        Predicate<MitoPerson> Under18  = p -> p.getAge() < 18;

        Predicate<MitoPerson> isStudent    = p -> p.getMitoOccupationStatus() == MitoOccupationStatus.STUDENT && p.getAge() >= 18;
        Predicate<MitoPerson> isEmployed   = p -> p.getMitoOccupationStatus() == MitoOccupationStatus.WORKER && p.getAge() >= 18;
        Predicate<MitoPerson> isUnemployed = p -> p.getMitoOccupationStatus() == MitoOccupationStatus.UNEMPLOYED && p.getAge() >= 18;
        Predicate<MitoPerson> isRetired    = p -> p.getMitoOccupationStatus() == MitoOccupationStatus.RETIRED; // persons aged 65 and older

        List<Predicate<MitoPerson>> filters = new ArrayList<>(10);

        filters.add(0, Under18.and(hasNoCar));          // 0: <18 & No Car
        filters.add(1, Under18.and(hasCar));            // 1: <18 & Car
        filters.add(2, isStudent.and(hasNoCar));        // 2: Student & No Car
        filters.add(3, isStudent.and(hasCar));          // 3: Student & Car
        filters.add(4, isEmployed.and(hasNoCar));       // 4: Employed & No Car
        filters.add(5, isEmployed.and(hasCar));         // 5: Employed & Car
        filters.add(6, isUnemployed.and(hasNoCar));     // 6: Unemployed & No Car
        filters.add(7, isUnemployed.and(hasCar));       // 7: Unemployed & Car
        filters.add(8, isRetired.and(hasNoCar));        // 8: Retired & No Car
        filters.add(9, isRetired.and(hasCar));          // 9: Retired & Car
        return filters;
    }
}
