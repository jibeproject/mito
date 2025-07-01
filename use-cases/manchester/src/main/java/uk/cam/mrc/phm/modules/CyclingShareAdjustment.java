package uk.cam.mrc.phm.modules;

import de.tum.bgu.msm.data.Mode;
import de.tum.bgu.msm.modules.Module;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoTrip;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.util.LogitTools;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CyclingShareAdjustment extends Module {
    private static final Logger logger = LogManager.getLogger(CyclingShareAdjustment.class);
    private static final EnumSet<Purpose> eligiblePurposes = EnumSet.of(
            Purpose.HBW, Purpose.HBE,   // → HBM
            Purpose.HBS, Purpose.HBO,   // → HBD
            Purpose.NHBW,               // → NHBM
            Purpose.NHBO                // → NHBD
    );

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
        logger.info("Running cycling share adjustment to reach {}%", targetShare * 100);

        List<MitoTrip> allTrips = new ArrayList<>(dataSet.getTrips().values());
        List<MitoTrip> candidates = allTrips.stream()
                .filter(trip -> trip.getTripMode() != Mode.bicycle)
                .filter(trip -> eligiblePurposes.contains(trip.getTripPurpose()))
                .collect(Collectors.toList());

        Map<MitoTrip, Double> bikeProbability = new HashMap<>(candidates.size());
        LogitTools<BikeChoice> bikeChoiceLogit = new LogitTools<>(BikeChoice.class);

        for (MitoTrip trip : candidates) {
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
                    logger.warn("Ignoring purpose{}", purpose);
            }

            EnumMap<BikeChoice, Double> choiceUtilities = new EnumMap<>(BikeChoice.class);
            choiceUtilities.put(BikeChoice.BIKE, utility);
            choiceUtilities.put(BikeChoice.OTHER, 0.0);

            bikeProbability.put(trip, bikeChoiceLogit.getProbabilitiesMNL(choiceUtilities).get(BikeChoice.BIKE));
        }

        int adjustedTrips = applyProbabilisticAdjustments(allTrips, candidates, bikeProbability, targetShare);
        logger.info("Adjusted {} trips to bike (out of {}).", adjustedTrips, allTrips.size());

        reportModeShares();
    }

    private int applyProbabilisticAdjustments(
            List<MitoTrip> allTrips,
            List<MitoTrip> candidates,
            Map<MitoTrip, Double> bikeProbability,
            double targetShare
    ) {

        long existingBikeCount = allTrips.stream().filter(trip -> trip.getTripMode() == Mode.bicycle).count();
        int totalTrips = allTrips.size();
        int desiredTotalBikes = (int) Math.round(totalTrips * targetShare);
        int toBike = Math.max(0, desiredTotalBikes - (int) existingBikeCount);

        if (candidates.size() < toBike) {
            logger.warn("Only {} eligible trips but {} requested; will convert {} trips only.",
                    candidates.size(), toBike, candidates.size());
        }

        if (toBike == 0) {
            return 0;
        }

        int seed = Resources.instance.getInt(Properties.RANDOM_SEED, 1);
        Random random = new Random(seed);

        PriorityQueue<Map.Entry<MitoTrip, Double>> minHeap = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (MitoTrip candidate : candidates) {
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
        Map<String, Double> coefficientsMap = new HashMap<>();
        try (
                Reader input = Files.newBufferedReader(Paths.get(csvFile), StandardCharsets.UTF_8);
                CSVParser parser = new CSVParser(input, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())
        ) {
            for (CSVRecord record : parser) {
                String variableName = record.get("Variable");
                double value        = Double.parseDouble(record.get("Estimate"));
                coefficientsMap.put(variableName, value);
            }
        }
        return coefficientsMap;
    }

    private double getCoefficient(String name) {
        Double estimate = coefficients.get(name);
        if (estimate == null) {
            throw new RuntimeException("Cycling coefficient for" + name +  "not found");
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

