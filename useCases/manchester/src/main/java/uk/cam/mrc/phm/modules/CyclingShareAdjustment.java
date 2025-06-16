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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CyclingShareAdjustment extends Module {
    private static final Logger logger = LogManager.getLogger(CyclingShareAdjustment.class);

    private final Map<String, Double> coefficients;
    private final double targetShare;

    public CyclingShareAdjustment(DataSet dataSet, double targetShare) {
        super(dataSet,
                Purpose.getListedPurposes(Resources.instance.getString(Properties.TRIP_PURPOSES))
        );
        this.targetShare = targetShare;

        String csvPath = Resources.instance.getString("cycling.coefficients.file");
        try {
            this.coefficients = readCyclingCoefficients(csvPath);
        } catch (IOException error) {
            throw new RuntimeException("Failed to read cycling coefficients from CSV: " + csvPath, error);
            }
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

            double travelDistance = dataSet.getTravelDistancesNMT().getTravelDistance(origin, destination) * 1000.;
            double logDist = Math.log(Math.max(travelDistance, 0.1));

            double utility = getCoefficient("asc") + getCoefficient("logd") * logDist + getCoefficient("logd2")* logDist * logDist;

            int age = trip.getPerson().getAge();
            if (age < 18) utility += getCoefficient("age_group0-17");
            else if (age < 30) utility += getCoefficient("age_group18-29");
            else if (age < 60) utility += getCoefficient("age_group30-59");
            else utility += getCoefficient("age_group60+");

            Purpose purpose = trip.getTripPurpose();
            switch (purpose) {
                case HBW:
                case HBE:
                    utility += getCoefficient("purposeHBM");
                    break;
                case HBS:
                case HBO:
                    utility += getCoefficient("purposeHBD");
                    break;
                case NHBW:
                    utility += getCoefficient("purposeNHBM");;
                    break;
                case NHBO:
                    utility += getCoefficient("purposeNHBD");;
                    break;
                default:
                    logger.error("Unknown purpose " + purpose);
            }

            EnumMap<BikeChoice, Double> choiceUtilities = new EnumMap<>(BikeChoice.class);
            choiceUtilities.put(BikeChoice.BIKE, utility);
            choiceUtilities.put(BikeChoice.OTHER, 0.0);

            bikeProbability.put(trip, bikeChoiceLogit.getProbabilitiesMNL(choiceUtilities).get(BikeChoice.BIKE));
        }

        int adjustedTrips = applyProbabilisticAdjustments(allTrips, bikeProbability, targetShare);
        logger.info("Adjusted " + adjustedTrips + " trips to bike (out of " + allTrips.size() + ").");

        reportModeShares();
    }

    private int applyProbabilisticAdjustments(
            List<MitoTrip> allTrips,
            Map<MitoTrip, Double> bikeProbability,
            double targetShare
    ) {

        long existingBikeCount = allTrips.stream().filter(trip -> trip.getTripMode() == Mode.bicycle).count();
        int totalTrips = allTrips.size();
        int desiredTotalBikes = (int) Math.round(totalTrips * targetShare);
        int toBike = Math.max(0, desiredTotalBikes - (int) existingBikeCount);

        if (toBike <= 0) {
            return 0;
        }

        EnumSet<Purpose> eligiblePurposes = EnumSet.of(
                Purpose.HBW, Purpose.HBE,    // → HBM
                Purpose.HBS, Purpose.HBO,    // → HBD
                Purpose.NHBW,                // → NHBM
                Purpose.NHBO                 // → NHBD
        );
        
        // only non-bike and eligible purpose trips
        List<MitoTrip> nonBikeTrips = allTrips.stream()
                .filter(trip -> trip.getTripMode() != Mode.bicycle)
                .filter(trip -> eligiblePurposes.contains(trip.getTripPurpose()))
                .collect(Collectors.toList());

        int seed = Resources.instance.getInt(Properties.RANDOM_SEED, 0);
        Random random = new Random(seed);

        PriorityQueue<Map.Entry<MitoTrip, Double>> minHeap = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (MitoTrip candidate : nonBikeTrips) {
            double weight = bikeProbability.getOrDefault(candidate, 0.0);
            if (weight <= 0) {
                continue;
            }
            double uniform = random.nextDouble();
            double key = Math.pow(uniform, 1.0 / weight);

            Map.Entry<MitoTrip, Double> entry = new AbstractMap.SimpleEntry<>(candidate, key);
            if (minHeap.size() < toBike) {
                minHeap.offer(entry);
            } else if (key > minHeap.peek().getValue()) {
                minHeap.poll();
                minHeap.offer(entry);
            }
        }

        minHeap.forEach(each -> each.getKey().setTripMode(Mode.bicycle));

        return minHeap.size();
    }

    private Map<String, Double> readCyclingCoefficients(String csvFile) throws IOException {
        Map<String, Double> coefficientMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2)
                    continue;
                String variableName = parts[0].trim();
                double value   = Double.parseDouble(parts[1].trim());
                coefficientMap.put(variableName, value);
            }
        }
        return coefficientMap;
    }

    private double getCoefficient(String name) {
        Double estimate = coefficients.get(name);
        if (estimate == null) {
            throw new RuntimeException(name + "Cycling coefficient not found");
        }
        return estimate;
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

