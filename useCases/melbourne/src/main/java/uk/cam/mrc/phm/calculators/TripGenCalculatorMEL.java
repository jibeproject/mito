package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.modules.tripGeneration.TripGenPredictor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TripGenCalculatorMEL implements TripGenPredictor {

    private final DataSet dataSet;

    public TripGenCalculatorMEL(DataSet dataSet) {
        this.dataSet = dataSet;
    }
    private static final Logger logger = LogManager.getLogger(TripGenCalculatorMEL.class);

    @Override
    public double getPredictor(MitoHousehold hh,MitoPerson pp, Map<String, Double> coefficients) {
        double predictor = 0.;

        // Intercept
        predictor += coefficients.get("(Intercept)");

        // Household size
        int householdSize = hh.getHhSize();
        if(householdSize == 1) {
            predictor += coefficients.get("hh.sized_1");
        }
        else if(householdSize == 2) {
            predictor += coefficients.get("hh.sized_2");
        }
        else if(householdSize == 3) {
            predictor += coefficients.get("hh.sized_3");
        }
        else if(householdSize == 4) {
            predictor += coefficients.get("hh.sized_4");
        }
        else  {
            assert(householdSize >= 5);
            predictor += coefficients.get("hh.sized_5");
        }

        // Number of children in household
        int householdChildren = hh.getChildrenForHousehold();
        if(householdChildren == 1) {
            predictor += coefficients.get("hh.childrend_1");
        }
        else if (householdChildren == 2) {
            predictor += coefficients.get("hh.childrend_2");
        }
        else if (householdChildren >= 3) {
            predictor += coefficients.get("hh.childrend_3");
        }

        // Household in urban region
        if(!(hh.getHomeZone().getAreaTypeR().equals(AreaTypes.RType.RURAL))) {
            predictor += coefficients.get("hh.urban");
        }

        // Household autos
        int householdAutos = hh.getAutos();
        if(householdAutos == 1) {
            predictor += coefficients.get("hh.carsd_1");
        }
        else if(householdAutos == 2) {
            predictor += coefficients.get("hh.carsd_2");
        }
        else if(householdAutos >= 3) {
            predictor += coefficients.get("hh.carsd_3");
        }

        // Autos per adult
        int householdAdults = householdSize - householdChildren;
        double autosPerAdult = Math.min((double) hh.getAutos() / (double) householdAdults , 1.0);
        predictor += autosPerAdult * coefficients.get("hh.cars_per_adult");

        // Age
        int age = pp.getAge();
        if (age <= 4) {
            predictor += coefficients.get("p.age_gr_1");
        }
        else if (age <= 10) {
            predictor += coefficients.get("p.age_gr_2");
        }
        else if (age <= 15) {
            predictor += coefficients.get("p.age_gr_3");
        }
        else if (age <= 17) {
            predictor += coefficients.get("p.age_gr_4");
        }
        else if (age <= 29) {
            predictor += coefficients.get("p.age_gr_5");
        }
        else if (age <= 49) {
            predictor += coefficients.get("p.age_gr_6");
        }
        else if (age <= 59){
            predictor += coefficients.get("p.age_gr_7");
        }
        else if (age <= 69){
            predictor += coefficients.get("p.age_gr_8");
        }
        else {
            predictor += coefficients.get("p.age_gr_9");
        }

        // Female
        if (pp.getMitoGender().equals(MitoGender.FEMALE)) {
            predictor += coefficients.get("p.female");
        }

        // Mito occupation Status
        MitoOccupationStatus occupationStatus = pp.getMitoOccupationStatus();
        if (occupationStatus.equals(MitoOccupationStatus.STUDENT)) {
            predictor += coefficients.get("p.status_student");
        } else if (occupationStatus.equals(MitoOccupationStatus.UNEMPLOYED)) {
            predictor += coefficients.get("p.status_unemployed");
        } else if (occupationStatus.equals(MitoOccupationStatus.RETIRED)) {
            predictor += coefficients.get("p.status_retired");
        }

        // Work trips & mean distance
        List<MitoTrip> workTrips = pp.getTrips().stream().filter(tt -> Purpose.HBW.equals(tt.getTripPurpose())).collect(Collectors.toList());
        int workTripCount = workTrips.size();
        if(workTripCount > 0) {
            if (workTripCount == 1) {
                predictor += coefficients.get("p.workTrips_1");
            } else if (workTripCount == 2) {
                predictor += coefficients.get("p.workTrips_2");
            } else if (workTripCount == 3) {
                predictor += coefficients.get("p.workTrips_3");
            } else if (workTripCount == 4) {
                predictor += coefficients.get("p.workTrips_4");
            } else {
                predictor += coefficients.get("p.workTrips_5");
            }
            int homeZoneId = pp.getHousehold().getZoneId();
            double meanWorkKm = workTrips.stream()
                    .mapToDouble(t -> {
                if (t.getTripDestination() == null) {
                    throw new RuntimeException("Trip destination is null for trip: " + t.toString() + ", person: " +; pp.getId());
                }
                return dataSet.getTravelDistancesNMT().getTravelDistance(homeZoneId, t.getTripDestination().getZoneId());
            })
                    .average()
                    .orElse(0.0);
            predictor += Math.log(meanWorkKm) * coefficients.get("p.log_km_mean_HBW");
        }

        // Education trips & mean distance
        List<MitoTrip> eduTrips = pp.getTrips().stream().filter(tt -> Purpose.HBE.equals(tt.getTripPurpose())).collect(Collectors.toList());
        int eduTripCount = eduTrips.size();
        if(eduTripCount > 0) {
            if (eduTripCount == 1) {
                predictor += coefficients.get("p.eduTrips_1");
            } else if (eduTripCount == 2) {
                predictor += coefficients.get("p.eduTrips_2");
            } else if (eduTripCount == 3) {
                predictor += coefficients.get("p.eduTrips_3");
            } else if (eduTripCount == 4) {
                predictor += coefficients.get("p.eduTrips_4");
            } else {
                predictor += coefficients.get("p.eduTrips_5");
            }
            int homeZoneId = pp.getHousehold().getZoneId();
            double meanEduKm = eduTrips.stream().
                    mapToDouble(t -> dataSet.getTravelDistancesNMT().
                            getTravelDistance(homeZoneId, t.getTripDestination().getZoneId())).average().getAsDouble();
            predictor += Math.log(meanEduKm) * coefficients.get("p.log_km_mean_HBE");
        }

        return predictor;
    }



}
