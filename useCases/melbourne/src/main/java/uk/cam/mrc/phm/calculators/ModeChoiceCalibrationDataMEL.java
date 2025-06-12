package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.modules.modeChoice.ModeChoiceCalibrationData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModeChoiceCalibrationDataMEL implements ModeChoiceCalibrationData {

    private Map<String, Map<Purpose, Map<Mode, Double>>> observedModalShare;
    private Map<String, Map<Purpose, Map<Mode, Integer>>> simulatedTripsByRegionPurposeAndMode;
    private Map<String, Map<Purpose, Map<Mode, Double>>> calibrationFactors;
    private Map<Integer, String> zoneToRegionMap;

    private PrintWriter pw = null;

    private static Logger logger = LogManager.getLogger(ModeChoiceCalibrationDataMEL.class);

    public ModeChoiceCalibrationDataMEL() {
        this.observedModalShare = new HashMap<>();
        this.calibrationFactors = new HashMap<>();
        this.simulatedTripsByRegionPurposeAndMode = new HashMap<>();
        zoneToRegionMap = new HashMap<>();
    }


    public double[] getCalibrationFactorsAsArray(Purpose tripPurpose, Location tripOrigin) {

        double[] factors = new double[Mode.values().length];
        int zoneId = tripOrigin.getZoneId();
        String region = zoneToRegionMap.get(zoneId);
        if (region == null) {
            logger.warn("No region found for zoneId {}. This may give rise to a NullPointerException.", zoneId);
        }
        Map<Purpose, Map<Mode, Double>> zoneCalibrationFactors = calibrationFactors.get(region);
        if (zoneCalibrationFactors == null) {
            logger.warn("Region {} not found within calibrationFactors. This may give rise to a NullPointerException.", region);
        }
        assert zoneCalibrationFactors != null;
        Map<Mode, Double> zoneTripPurpose = zoneCalibrationFactors.get(tripPurpose);
        if (zoneTripPurpose == null) {
            logger.warn("No purpose {} in region {}. This may give rise to a NullPointerException.", tripPurpose, region);
        }
        assert zoneTripPurpose != null;
        for (Mode mode : Mode.values()) {
            factors[mode.getId()] = zoneTripPurpose.getOrDefault(mode, 0.);
        }
        return factors;
    }

    public Map<String, Map<Purpose, Map<Mode, Double>>> getObservedModalShare() {
        return observedModalShare;
    }

    public Map<String, Map<Purpose, Map<Mode, Double>>> getCalibrationFactors() {
        return calibrationFactors;
    }

    public Map<Integer, String> getZoneToRegionMap() {
        return zoneToRegionMap;
    }

    public void updateCalibrationCoefficients(DataSet dataSet, int iteration, List<Purpose> purposes) {

        if (pw == null){
            try {
                pw = new PrintWriter(new File("mode_choice_calibration.csv"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            pw.println("iteration,region,purpose,mode,observed_share,sim_share,k,trips");
        }

        simulatedTripsByRegionPurposeAndMode.clear();

        for (MitoTrip trip : dataSet.getTrips().values()) {
            //Note that, TRADS survey not include kids under 5. so exclude kids under 5 when calibrating
            //TODO: calibration by age group
            if(trip.getPerson().getAge() < 5){
                continue;
            }

            if (trip.getTripMode() != null) {
                String region = zoneToRegionMap.get(trip.getTripOrigin().getZoneId());
                Purpose purpose = trip.getTripPurpose();
                Mode mode = trip.getTripMode();

                simulatedTripsByRegionPurposeAndMode.putIfAbsent(region, new HashMap<>());
                simulatedTripsByRegionPurposeAndMode.get(region).putIfAbsent(purpose, new HashMap<>());
                simulatedTripsByRegionPurposeAndMode.get(region).get(purpose).putIfAbsent(mode, 0);
                int newValue = simulatedTripsByRegionPurposeAndMode.get(region).get(purpose).get(mode) + 1;
                simulatedTripsByRegionPurposeAndMode.get(region).get(purpose).put(mode, newValue);
            }
        }


        for (String region : observedModalShare.keySet()) {
            for (Purpose purpose : purposes) {
                for (Mode mode : Mode.values()) {
                    double observedShare = observedModalShare.get(region).get(purpose).getOrDefault(mode, 0.);
                    Map<Purpose, Map<Mode, Integer>> regionMap = simulatedTripsByRegionPurposeAndMode.get(region);
                    if (regionMap == null) {
                        logger.warn("No simulated trips for region: {}", region);
                        continue;
                    }
                    Map<Mode, Integer> purposeMap = regionMap.get(purpose);
                    if (purposeMap == null) {
                        logger.warn("No simulated trips for purpose: {} in region: {}", purpose, region);
                        continue;
                    }
                    double tripAtRegionAndPurpose = purposeMap.values().stream().mapToInt(Integer::intValue).sum();
                    double simulatedShare;
                    if (tripAtRegionAndPurpose != 0) {
                        simulatedShare = simulatedTripsByRegionPurposeAndMode.get(region).get(purpose).getOrDefault(mode, 0) / tripAtRegionAndPurpose;
                    } else {
                        simulatedShare = 0.;
                    }

                    double difference = observedShare - simulatedShare;
                    double existingFactor = calibrationFactors.get(region).get(purpose).getOrDefault(mode, 0.);
                    double newFactor = existingFactor + difference;
                    calibrationFactors.get(region).get(purpose).put(mode, newFactor);

                    double tripCount = tripAtRegionAndPurpose * simulatedShare;
                    pw.println(iteration + "," + region + "," + purpose + "," + mode + "," +
                            observedShare + "," + simulatedShare + "," + newFactor + "," + tripCount);
                }
            }
        }


    }

    public void close() {
        pw.close();
        logger.info("Finished mode choice calibration");
    }


}
