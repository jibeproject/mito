package de.tum.bgu.msm.scenarios.mito7days.calculators;

import de.tum.bgu.msm.data.MitoPerson;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.modules.tripDistribution.AbstractDestinationUtilityCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DestinationUtilityCalculatorImpl7days extends AbstractDestinationUtilityCalculator {

    private final static double[] TRAVEL_DISTANCE_PARAM_HBW = {-0.03404, -0.02821, -0.01712};
    private final static double IMPEDANCE_PARAM_HBW = 9;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = {-0.006599, -0.006599, -0.006599};
    private final static double IMPEDANCE_PARAM_HBE = 28.3;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = {-0.08805, -0.03948, -0.03362};
    private final static double IMPEDANCE_PARAM_HBS = 14.5;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBR = {-0.011585, -0.010204, -0.0096598};
    private final static double IMPEDANCE_PARAM_HBR = 20;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBO = {-0.006823, -0.004451, -0.004141};
    private final static double IMPEDANCE_PARAM_HBO = 53;

    private final static double[] TRAVEL_DISTANCE_PARAM_NHBW = {-0.027959,  -0.015731, -0.010706}; // -0.012747;
    private final static double IMPEDANCE_PARAM_NHBW = 15.1;

    private final static double[] TRAVEL_DISTANCE_PARAM_NHBO = {-0.022910, -0.013904, -0.011814}; // -0.0130997;
    private final static double IMPEDANCE_PARAM_NHBO = 20;


    public DestinationUtilityCalculatorImpl7days(Purpose purpose) {

         switch (purpose) {
            case HBW:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBW;
                impedanceParam = IMPEDANCE_PARAM_HBW;
                break;
            case HBE:
                distanceParams = TRAVEL_DISTANCE_PARAM_HBE;
                impedanceParam = IMPEDANCE_PARAM_HBE;
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

    public List<Predicate<MitoPerson>> getCategories() {
        List<Predicate<MitoPerson>> filters = new ArrayList<>();
        filters.add(0,p -> p.getHousehold().getAutos() == 0);
        filters.add(1,p -> p.getHousehold().getAutosPerAdult() > 0 && p.getHousehold().getAutosPerAdult() < 1);
        filters.add(2,p -> p.getHousehold().getAutosPerAdult() >= 1);
        return filters;
    }
}
