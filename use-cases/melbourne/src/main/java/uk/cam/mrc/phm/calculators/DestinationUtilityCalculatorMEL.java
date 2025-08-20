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
    // HBW calibrated (mean); 2025-08-20:
    private final static double[] TRAVEL_DISTANCE_PARAM_HBW = {-0.014498363345160612, -0.02166297217958154, -0.005688024888241202, -0.011084086697820301, -0.003588055363552403, -0.007924277239485482, -0.012032929116838407, -0.013324079821017833, -0.0118146793539948, -0.012739215635141792};
    private final static double IMPEDANCE_PARAM_HBW = 16.3;
    // HBE mean: (-0.05660650656401385 + -0.04730781708082465 + -0.048208428932957896) / 3 = -0.05004058485955447
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = fillArray(-0.05004058485955447, 10);
    // HBE calibrated (mean); 2025-08-20 (replaced NaN values with -0.05004058485955447
    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = {-0.10008116971910894, -0.10008116971910894, -0.05004058485955447, -0.05004058485955447, -0.07529487260192196, -0.08267269997602315, -0.09017464749323888, -0.10008116971910894, -0.08786103638236557, -0.07384966444712061};
    private final static double IMPEDANCE_PARAM_HBE = 6.66;
    // HBA mean: (-0.12740481694077424 + -0.03845635376344552 + -0.03688687152506809) / 3 = -0.06791668040942928
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBA = fillArray(-0.06791668040942928, 10);
    // HBE calibrated (mean); 2025-08-20
    private final static double[] TRAVEL_DISTANCE_PARAM_HBA = {-0.0401677743803288, -0.048782854229724275, -0.032931235656713964, -0.038897267876700815, -0.03697260260342743, -0.0380978447440113, -0.05277795067936258, -0.05504614474785929, -0.018419898765729376, -0.030636013480613105};
    private final static double IMPEDANCE_PARAM_HBA =  12.06; // Vista-based 6.03 failed to calibrate;
    // HBS mean: (-0.1302481725479954 + -0.0716719631663481 + -0.06272342097190081) / 3 = -0.08888185289574877
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = fillArray(-0.08888185289574877, 10);
    // HBS semi-calibrated (mean); 2025-08-20 - all groups except for last (retired with car) failed to calibrate, so a custom tweak used for last group.  See https://github.com/jibeproject/mito/issues/76
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = {-0.0380812569074194, -0.05409985284216312, -0.03535140547605545, -0.05670814672404346, -0.04367856309857014, -0.06847359315942239, -0.050870231263036664, -0.0813413293498617, -0.06459280156588387, -0.06};
    // HBS calibrated (mean); 2025-08-20
    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = {-0.03102653277137382, -0.04046191348789098, -0.029842068377971112, -0.04337614022154041, -0.0355947336094127, -0.047333515387482594, -0.040862219086009066, -0.052208950471959774, -0.04968845296493172, -0.07401201003773145};
    private final static double IMPEDANCE_PARAM_HBS = 12; // Vista-based 5.35 failed to calibrate;
    // HBR mean: (-0.09341006840787956 + -0.046114237946687356 + -0.03142021825706931) / 3 = -0.05764850820354541
    private final static double[] TRAVEL_DISTANCE_PARAM_HBR = fillArray(-0.05764850820354541, 10);
    private final static double IMPEDANCE_PARAM_HBR = 13; // Vista-based 5.35 failed to calibrate;
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
