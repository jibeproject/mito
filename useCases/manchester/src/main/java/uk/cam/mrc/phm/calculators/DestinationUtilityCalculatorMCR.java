package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.modules.tripDistribution.AbstractDestinationUtilityCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DestinationUtilityCalculatorMCR extends AbstractDestinationUtilityCalculator {

    private final static double[] TRAVEL_DISTANCE_PARAM_HBW = {-0.09591085352330427, -0.037053482912535886, -0.02446406710433703};
    private final static double IMPEDANCE_PARAM_HBW = 9;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = {-0.04672027031869003, -0.04037387259002789, -0.04146270861316951};
    private final static double IMPEDANCE_PARAM_HBE = 28.3;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBA = {-0.09955652447332028, -0.037233504112947166, -0.035675574009511046};
    private final static double IMPEDANCE_PARAM_HBA = 28.3;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = {-0.11253440389512925, -0.07031488190247467, -0.05712682692932565};
    private final static double IMPEDANCE_PARAM_HBS = 14.5;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBR = {-0.07069337826474964, -0.035671815381711686, -0.0290999064713141};
    private final static double IMPEDANCE_PARAM_HBR = 20;

    private final static double[] TRAVEL_DISTANCE_PARAM_HBO = {-0.023104475728384134, -0.01195144435070875, -0.009356073203574318};
    private final static double IMPEDANCE_PARAM_HBO = 53;

    private final static double[] TRAVEL_DISTANCE_PARAM_RRT = {-0.7226231467004061, -0.2135749210668592, -0.18329824253133586}; //redundant?
    private final static double IMPEDANCE_PARAM_RRT = 20; //redundant?

    private final static double[] TRAVEL_DISTANCE_PARAM_NHBW = {-0.17819636932512015, -0.04991314988925143, -0.02197323511688223};
    private final static double IMPEDANCE_PARAM_NHBW = 15.1;

    private final static double[] TRAVEL_DISTANCE_PARAM_NHBO = {-0.15327609981171803, -0.043799884050731555, -0.036039798680056925};
    private final static double IMPEDANCE_PARAM_NHBO = 20;


    public DestinationUtilityCalculatorMCR(Purpose purpose) {

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
        List<Predicate<MitoPerson>> filters = new ArrayList<>();
        filters.add(0,p -> p.getHousehold().getAutos() == 0);
        filters.add(1,p -> p.getHousehold().getAutosPerAdult() > 0 && p.getHousehold().getAutosPerAdult() < 1);
        filters.add(2,p -> p.getHousehold().getAutosPerAdult() >= 1);
        return filters;
    }
}
