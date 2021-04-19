package de.tum.bgu.msm.modules.tripDistribution;

import de.tum.bgu.msm.data.Purpose;

public class DestinationUtilityCalculatorImpl implements DestinationUtilityCalculator {

    private final static double TRAVEL_DISTANCE_PARAM_HBS = -0.01;
    private final static double IMPEDANCE_PARAM_HBS = -10;

    private final static double TRAVEL_DISTANCE_PARAM_HBO = -0.0107;
    private final static double IMPEDANCE_PARAM_HBO = 53;

    private final static double TRAVEL_DISTANCE_PARAM_HBW = -0.07;
    private final static double IMPEDANCE_PARAM_HBW = 9;

    private final static double TRAVEL_DISTANCE_PARAM_HBE = -0.149;
    private final static double IMPEDANCE_PARAM_HBE = 28.3;

    private final static double travelDistanceParamNhbw = -0.0071;
    private final static double impedanceParamNhbw = 16.1;

    private final static double travelDistanceParamNhbo = -0.0147;
    private final static double impedanceParamNhbo = 16.6;

    private final double distanceParam;
    private final double impedanceParam;

    DestinationUtilityCalculatorImpl(Purpose purpose) {
        switch (purpose) {
            case HBW:
                distanceParam = TRAVEL_DISTANCE_PARAM_HBW;
                impedanceParam = IMPEDANCE_PARAM_HBW;
                break;
            case HBE:
                distanceParam = TRAVEL_DISTANCE_PARAM_HBE;
                impedanceParam = IMPEDANCE_PARAM_HBE;
                break;
            case HBS:
                distanceParam = TRAVEL_DISTANCE_PARAM_HBS;
                impedanceParam = IMPEDANCE_PARAM_HBS;
                break;
            case HBO:
                distanceParam = TRAVEL_DISTANCE_PARAM_HBO;
                impedanceParam = IMPEDANCE_PARAM_HBO;
                break;
            case NHBW:
                distanceParam = travelDistanceParamNhbw;
                impedanceParam = impedanceParamNhbw;
                break;
            case NHBO:
                distanceParam = travelDistanceParamNhbo;
                impedanceParam = impedanceParamNhbo;
                break;
            case AIRPORT:
            default:
                throw new RuntimeException("not implemented!");
        }
    }

    @Override
    public double calculateUtility(double attraction, double travelDistance) {
        if(attraction == 0) {
            return 0.;
        }
        double impedance = impedanceParam * Math.exp(distanceParam * travelDistance);
        return Math.exp(impedance) * attraction;
    }
}
