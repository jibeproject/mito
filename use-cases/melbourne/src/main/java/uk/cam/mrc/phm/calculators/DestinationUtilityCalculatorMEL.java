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
//     private final static double[] TRAVEL_DISTANCE_PARAM_HBW = fillArray(-0.05971454230979444, 5);
    // HBW calibrated (mean); 2025-09-24:
    private final static double[] TRAVEL_DISTANCE_PARAM_HBW = {-0.020783505403729884, -0.010510436321682146, -0.007639555195306878, -0.01327277482767267, -0.01298129308114014};
    private final static double IMPEDANCE_PARAM_HBW = 16.3; // vista-based
    // HBE mean: (-0.05660650656401385 + -0.04730781708082465 + -0.048208428932957896) / 3 = -0.05004058485955447
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = fillArray(-0.05004058485955447, 5);
    // HBE calibrated (mean); 2025-09-24: Filled NaN with mean of other 4 categories
    private final static double[] TRAVEL_DISTANCE_PARAM_HBE = {-0.10008116971910894, -0.09034951516159911, -0.08330343096898854, -0.10008116971910894, -0.07793229023919002};
    private final static double IMPEDANCE_PARAM_HBE = 6.66; // vista-based
    // HBA mean: (-0.12740481694077424 + -0.03845635376344552 + -0.03688687152506809) / 3 = -0.06791668040942928
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBA = fillArray(-0.06791668040942928, 5);
    // HBE calibrated (mean); 2025-m09-24: NaN for students; filled with mean of other 4 categories
    private final static double[] TRAVEL_DISTANCE_PARAM_HBA = {-0.10008116971910894, -0.09, -0.08330343096898854, -0.10008116971910894, -0.07793229023919002};
    private final static double IMPEDANCE_PARAM_HBA =  12;  // vista-based 6.03 failed to calibrate;
    // HBS mean: (-0.1302481725479954 + -0.0716719631663481 + -0.06272342097190081) / 3 = -0.08888185289574877
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = fillArray(-0.08888185289574877, 5);
    // HBS semi-calibrated (mean); 2025-09-24:
    private final static double[] TRAVEL_DISTANCE_PARAM_HBS = {-0.03964389377487448, -0.04192528228123542, -0.046451341749660376, -0.05120119215460288, -0.0699352512509255};
    private final static double IMPEDANCE_PARAM_HBS = 12; // Vista-based 5.35 failed to calibrate;
    // HBR mean: (-0.09341006840787956 + -0.046114237946687356 + -0.03142021825706931) / 3 = -0.05764850820354541
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBR = fillArray(-0.05764850820354541, 5);
    // HBR calibrated (mean); 2025-09-24:
    private final static double[] TRAVEL_DISTANCE_PARAM_HBR = {-0.02936753512780351, -0.02942293554267954, -0.03353305138779052, -0.03234631417603561, -0.035779668664114715};
    private final static double IMPEDANCE_PARAM_HBR = 13; // Vista-based 5.35 failed to calibrate;
    // HBO mean: (-0.028762939944400743 + -0.013710304239040036 + -0.010043876974845265) / 3 = -0.017172707052428348
//    private final static double[] TRAVEL_DISTANCE_PARAM_HBO = fillArray(-0.017172707052428348, 5);
    // HBO calibrated (mean); 2025-09-24:
    private final static double[] TRAVEL_DISTANCE_PARAM_HBO = {-0.02066889101185145, -0.018586481663333085, -0.019117554306155905, -0.020217423406922206, -0.023928014614250565};
    private final static double IMPEDANCE_PARAM_HBO = 11.34; // vista-based
    // RRT
    private final static double[] TRAVEL_DISTANCE_PARAM_RRT = {-0.12877572260971806}; //redundant?
    private final static double IMPEDANCE_PARAM_RRT = 20; //redundant?
    // NHBW mean: (-0.38036997226527924 + -0.05575584617535517 + -0.02997957948287176) / 3 = -0.15570113264116872
//    private final static double[] TRAVEL_DISTANCE_PARAM_NHBW = fillArray(-0.15570113264116872, 5);
    private final static double[] TRAVEL_DISTANCE_PARAM_NHBW = {-0.02458062928399578, -0.015219334000855478, -0.019922182615973665, -0.029107105288329882, -0.04545489174853249};
    private final static double IMPEDANCE_PARAM_NHBW = 14; // vista-based 8.07 failed to calibrate
    // NHBO mean: (-0.26157597149372586 + -0.04848404658490696 + -0.042259625478948036) / 3 = -0.11743988151986029
//    private final static double[] TRAVEL_DISTANCE_PARAM_NHBO = fillArray(-0.11743988151986029, 5);
    // NHBO calibrated (mean);2025-09-24:
    private final static double[] TRAVEL_DISTANCE_PARAM_NHBO = {-0.03334355948043289, -0.02424962334491562, -0.026225960782632923, -0.03168234036826033, -0.031003268399765204};
    private final static double IMPEDANCE_PARAM_NHBO = 12; // vista-based 7.52 failed to calibrate

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

        Predicate<MitoPerson> Under18  = p -> p.getAge() < 18;

        Predicate<MitoPerson> isStudent    = p -> p.getMitoOccupationStatus() == MitoOccupationStatus.STUDENT && p.getAge() >= 18;
        Predicate<MitoPerson> isEmployed   = p -> p.getMitoOccupationStatus() == MitoOccupationStatus.WORKER && p.getAge() >= 18;
        Predicate<MitoPerson> isUnemployed = p -> p.getMitoOccupationStatus() == MitoOccupationStatus.UNEMPLOYED && p.getAge() >= 18;
        Predicate<MitoPerson> isRetired    = p -> p.getMitoOccupationStatus() == MitoOccupationStatus.RETIRED; // persons aged 65 and older

        List<Predicate<MitoPerson>> filters = new ArrayList<>(5);

        filters.add(0, Under18);      // 0: <18
        filters.add(1, isStudent);    // 1: Student
        filters.add(2, isEmployed);   // 2: Employed
        filters.add(3, isUnemployed); // 3: Unemployed
        filters.add(4, isRetired);    // 4: Retired
        return filters;
    }
}
