package de.tum.bgu.msm.scenarios.mito7days.calculators;

import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.modules.tripDistribution.AbstractDestinationUtilityCalculator;


public class DestinationUtilityCalculatorImplRrt7days extends AbstractDestinationUtilityCalculator {

    private final static double[] DISTANCE_PARAMS_RRT = {-0.05204};
    private final static double IMPEDANCE_PARAM_RRT = 12;

    public DestinationUtilityCalculatorImplRrt7days(Purpose purpose) {
        if (purpose != Purpose.RRT) {
            throw new RuntimeException("This calculator is for RRT trips only!");
        }
        distanceParams = DISTANCE_PARAMS_RRT;
        impedanceParam = IMPEDANCE_PARAM_RRT;
    }
}
