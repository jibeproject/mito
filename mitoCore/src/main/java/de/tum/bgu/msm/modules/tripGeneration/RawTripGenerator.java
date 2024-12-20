package de.tum.bgu.msm.modules.tripGeneration;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.util.MitoUtil;
import de.tum.bgu.msm.util.concurrent.ConcurrentExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by Nico on 20.07.2017.
 */
public class RawTripGenerator {

    private static final Logger logger = LogManager.getLogger(RawTripGenerator.class);

    final static AtomicInteger DROPPED_TRIPS_AT_BORDER_COUNTER = new AtomicInteger();
    final static AtomicInteger TRIP_ID_COUNTER = new AtomicInteger();

    private final DataSet dataSet;
    private final List<Purpose> purposes;
    private MitoTripFactory mitoTripFactory;
    //private final EnumSet<Purpose> PURPOSES = EnumSet.of(HBW, HBE, HBS, HBO, NHBW, NHBO);
    private Map<Purpose, TripGenerator> tripGeneratorByPurpose;

    public RawTripGenerator(DataSet dataSet, Map<Purpose, TripGenerator> tripGeneratorByPurpose, List<Purpose> purposes, MitoTripFactory mitoTripFactory) {
        this.dataSet = dataSet;
        this.tripGeneratorByPurpose = tripGeneratorByPurpose;
        this.purposes = purposes;
        this.mitoTripFactory = mitoTripFactory;
    }

    public void run () {
        generateByPurposeMultiThreaded();
        logTripGeneration();
    }

    private void generateByPurposeMultiThreaded() {
        final ConcurrentExecutor<Tuple<Purpose, Map<MitoPerson, Integer>>> executor =
                ConcurrentExecutor.fixedPoolService(purposes.size());
        List<Callable<Tuple<Purpose, Map<MitoPerson, Integer>>>> tasks = new ArrayList<>();
        for(Purpose purpose: purposes) {
            tasks.add(tripGeneratorByPurpose.get(purpose));
        }
        final List<Tuple<Purpose, Map<MitoPerson, Integer>>> results = executor.submitTasksAndWaitForCompletion(tasks);

        for(Tuple<Purpose, Map<MitoPerson, Integer>> result: results) {
            final Purpose purpose = result.getFirst();
            final int sum = result.getSecond().values().stream().mapToInt(Integer::intValue).sum();
            logger.info("Created " + sum + " trips for " + purpose);
            final Map<MitoPerson, Integer> tripCountsByPP = result.getSecond();

            for(Map.Entry<MitoPerson, Integer> tripsByPerson: tripCountsByPP.entrySet()) {
                int numberOfTrips = tripsByPerson.getValue();

                for (int i = 0; i < numberOfTrips; i++) {
                    MitoTrip trip = mitoTripFactory.createTrip(TRIP_ID_COUNTER.incrementAndGet(), purpose);
                    trip.setPerson(tripsByPerson.getKey());
                    tripsByPerson.getKey().addTrip(trip);
                    tripsByPerson.getKey().getHousehold().addTripsForPurpose(purpose, trip);
                    dataSet.addTrip(trip);
                }
            }
        }
    }

    private void logTripGeneration() {
        long rawTrips = dataSet.getTrips().size() + DROPPED_TRIPS_AT_BORDER_COUNTER.get();
        logger.info("  Generated " + MitoUtil.customFormat("###,###", rawTrips) + " raw trips.");
        if (DROPPED_TRIPS_AT_BORDER_COUNTER.get() > 0) {
            logger.info(MitoUtil.customFormat("  " + "###,###", DROPPED_TRIPS_AT_BORDER_COUNTER.get()) +
                    " trips were dropped at boundary of study area.");
        }
    }
}
