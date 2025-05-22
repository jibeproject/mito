package uk.cam.mrc.phm.modules;

import de.tum.bgu.msm.data.Mode;
import de.tum.bgu.msm.modules.Module;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoTrip;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.util.LogitTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CyclingShareAdjustment extends Module {
    private static final Logger logger = LogManager.getLogger(CyclingShareAdjustment.class);

    // NOTE: These values are currently hard coded for prototyping.
    // TODO: Replace with a CSV-based reader to load the coefficients at runtime.
    // ----------------------------------------------------------------
    private static final double beta0      = 1;
    private static final double beta1      = 1;
    private static final double beta2      = 1;
    private static final double beta31     = 1;   // age < 18
    private static final double beta32     = 1;   // 18 ≤ age < 30
    private static final double beta33     = 1;   // 30 ≤ age < 60
    private static final double beta34     = 1;   // age ≥ 60
    private static final double beta4_HBM  = 1;   // home-based mandatory
    private static final double beta4_HBD  = 1;   // home-based discretionary
    private static final double beta4_NHBM = 1;   // non-home-based mandatory
    private static final double beta4_NHBD = 1;   // non-home-based discretionary
    //–––––––––––––––––––––––––––––––––––––––––––––––––––

    private final double targetShare;

    public CyclingShareAdjustment(DataSet dataSet, double targetShare) {
        super(dataSet,
                Purpose.getListedPurposes(Resources.instance.getString(Properties.TRIP_PURPOSES))
        );
        this.targetShare = targetShare;
    }

    @Override
    public void run() {
        logger.info("Running cycling share adjustment to reach " + (targetShare * 100) + "%");

        LogitTools<BikeChoice> bikeChoiceLogit = new LogitTools<>(BikeChoice.class);
        List<MitoTrip> allTrips = new ArrayList<>(dataSet.getTrips().values());
        Map<MitoTrip, Double> bikeProbability = new HashMap<>(allTrips.size());

        for (MitoTrip trip : allTrips) {
            int origin = trip.getTripOrigin().getZoneId();
            int destination = trip.getTripDestination().getZoneId();
            double logDist = Math.log(Math.max(dataSet.getTravelDistancesNMT().getTravelDistance(origin, destination), 0.1));

            double utility = beta0 + beta1 * logDist + beta2 * logDist * logDist;

            int age = trip.getPerson().getAge();
            if (age < 18) utility += beta31;
            else if (age < 30) utility += beta32;
            else if (age < 60) utility += beta33;
            else utility += beta34;

            Purpose purpose = trip.getTripPurpose();
            switch (purpose) {
                case HBW:
                case HBE:
                    utility += beta4_HBM;
                    break;
                case HBS:
                case HBR:
                case HBO:
                    utility += beta4_HBD;
                    break;
                case NHBW:
                    utility += beta4_NHBM;
                    break;
                case NHBO:
                    utility += beta4_NHBD;
                    break;
                default:
                    logger.error("Unknown purpose " + purpose);
            }

            EnumMap<BikeChoice, Double> choiceUtilities = new EnumMap<>(BikeChoice.class);
            choiceUtilities.put(BikeChoice.BIKE, utility);
            choiceUtilities.put(BikeChoice.OTHER, 0.0);

            bikeProbability.put(trip, bikeChoiceLogit.getProbabilitiesMNL(choiceUtilities).get(BikeChoice.BIKE));
        }

        // Sort all trips by descending bicycle probability, then reassign the top N trips to bicycle
        allTrips.sort((trip1, trip2) ->
                Double.compare(bikeProbability.get(trip2), bikeProbability.get(trip1))
        );
        int N = (int) Math.round(allTrips.size() * targetShare);
        for (int i = 0; i < N; i++) {
            allTrips.get(i).setTripMode(Mode.bicycle);
        }

        logger.info("Adjusted " + N + " trips to bike (out of " + allTrips.size() + ").");

        reportModeShares();
    }

    private void reportModeShares() {
        logger.info("#################################################");
        logger.info("Mode shares after cycling share adjustment:");

        // Mode shares by purpose
        Map<Purpose, List<MitoTrip>> tripsByPurpose = dataSet.getTrips().values().stream()
                .filter(trip -> trip.getTripMode() != null)
                .collect(Collectors.groupingBy(MitoTrip::getTripPurpose));

        tripsByPurpose.forEach((purpose, trips) -> {
            final long totalTrips = trips.size();
            trips.parallelStream()
                    //group number of trips by mode
                    .collect(Collectors.groupingBy(MitoTrip::getTripMode, Collectors.counting()))
                    //calculate and add share to data set table
                    .forEach((mode, count) ->
                            dataSet.addModeShareForPurpose(purpose, mode, (double) count / totalTrips));
        });

        for (Purpose purpose : Purpose.values()) {
            logger.info("#################################################");
            logger.info("Mode shares for purpose " + purpose + ":");
            for (Mode mode : Mode.values()) {
                Double share = dataSet.getModeShareForPurpose(purpose, mode);
                if (share != null) {
                    logger.info(mode + " = " + share * 100 + "%");
                }
            }
        }
    }

    private enum BikeChoice { BIKE, OTHER }
}

