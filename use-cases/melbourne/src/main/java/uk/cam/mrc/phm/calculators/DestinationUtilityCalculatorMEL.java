package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.modules.tripDistribution.AbstractDestinationUtilityCalculator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class DestinationUtilityCalculatorMEL extends AbstractDestinationUtilityCalculator {
    // Initial travel distance parameters for Melbourne are based on the average of three categories from the Manchester model
    // For impedance, using VISTA 2012-20 based estimates for Melbourne.  See https://github.com/jibeproject/mito/issues/76.
    // Subsequent calibration (see TravelDemandGeneratorMEL) updates the initial values based on sociodemographic categories from the VISTA 2012-20 dataset
    // HBW pre-calibration mean: (-0.10565083158495737 + -0.04366834773482148 + -0.029824447609604468) / 3 = -0.05971454230979444
//     private final static double[] TRAVEL_DISTANCE_PARAM_HBW = fillArray(-0.05971454230979444, 10);
    // HBW calibrated (median):
    private final static double[] TRAVEL_DISTANCE_PARAM_HBW = {-0.05971454230979444, -0.05971454230979444, -0.05971454230979444, -0.05971454230979444, -0.0079316348436468, -0.017398406505078023, -0.01277054483184539, -0.04814931467120163, -0.02524272927425683, -0.03675716650244677};
//    -0.034253120570654445, -2.162799920685698, -0.011650873380692275, -0.026560445185797414, -0.007025871525319614, -0.01845873425288566, -0.027594496534689046, -0.03545157043972543, -0.026904023438933302, -0.03476126092809079]
    private final static double IMPEDANCE_PARAM_HBW = 16.3;
    // HBE mean: (-0.05660650656401385 + -0.04730781708082465 + -0.048208428932957896) / 3 = -0.05004058485955447
    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = fillArray(-0.05004058485955447, 10);
    private final static double IMPEDANCE_PARAM_HBE = 6.66;
    // HBA mean: (-0.12740481694077424 + -0.03845635376344552 + -0.03688687152506809) / 3 = -0.06791668040942928
    private final static double[] TRAVEL_DISTANCE_PARAM_HBA = fillArray(-0.06791668040942928, 10);
    private final static double IMPEDANCE_PARAM_HBA = 6.03;
    // HBS mean: (-0.1302481725479954 + -0.0716719631663481 + -0.06272342097190081) / 3 = -0.08888185289574877
    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = fillArray(-0.08888185289574877, 10);
    private final static double IMPEDANCE_PARAM_HBS = 5.35;
    // HBR mean: (-0.09341006840787956 + -0.046114237946687356 + -0.03142021825706931) / 3 = -0.05764850820354541
    private final static double[] TRAVEL_DISTANCE_PARAM_HBR = fillArray(-0.05764850820354541, 10);
    private final static double IMPEDANCE_PARAM_HBR = 6.48;
    // HBO mean: (-0.028762939944400743 + -0.013710304239040036 + -0.010043876974845265) / 3 = -0.017172707052428348
    private final static double[] TRAVEL_DISTANCE_PARAM_HBO = fillArray(-0.017172707052428348, 10);
    private final static double IMPEDANCE_PARAM_HBO = 11.34;
    // RRT
    private final static double[] TRAVEL_DISTANCE_PARAM_RRT = {-0.12877572260971806}; //redundant?
    private final static double IMPEDANCE_PARAM_RRT = 20; //redundant?
    // NHBW mean: (-0.38036997226527924 + -0.05575584617535517 + -0.02997957948287176) / 3 = -0.15570113264116872
    private final static double[] TRAVEL_DISTANCE_PARAM_NHBW = fillArray(-0.15570113264116872, 10);
    private final static double IMPEDANCE_PARAM_NHBW = 8.07;
    // NHBO mean: (-0.26157597149372586 + -0.04848404658490696 + -0.042259625478948036) / 3 = -0.11743988151986029
    private final static double[] TRAVEL_DISTANCE_PARAM_NHBO = fillArray(-0.11743988151986029, 10);
    private final static double IMPEDANCE_PARAM_NHBO = 7.52;

    private static double[] fillArray(double value, int length) {
        double[] arr = new double[length];
        Arrays.fill(arr, value);
        return arr;
    }

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
