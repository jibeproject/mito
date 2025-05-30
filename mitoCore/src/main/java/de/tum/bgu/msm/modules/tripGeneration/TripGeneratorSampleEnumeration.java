package de.tum.bgu.msm.modules.tripGeneration;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import de.tum.bgu.msm.util.concurrent.RandomizableConcurrentFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;

import static de.tum.bgu.msm.modules.tripGeneration.RawTripGenerator.DROPPED_TRIPS_AT_BORDER_COUNTER;
import static de.tum.bgu.msm.modules.tripGeneration.RawTripGenerator.TRIP_ID_COUNTER;

class TripGeneratorSampleEnumeration extends RandomizableConcurrentFunction<Tuple<Purpose, Map<MitoHousehold, List<MitoTrip>>>> implements TripGenerator {

    private static final Logger logger = LogManager.getLogger(TripGeneratorSampleEnumeration.class);
    private final boolean dropAtBorder = Resources.instance.getBoolean(Properties.REMOVE_TRIPS_AT_BORDER);
    private Map<MitoHousehold, List<MitoTrip>> tripsByHH = new LinkedHashMap<>();
    private final DataSet dataSet;
    private final Purpose purpose;
    private final HouseholdTypeManager householdTypeManager;
    private final MitoTripFactory mitoTripFactory;
    TripGeneratorSampleEnumeration(DataSet dataSet, Purpose purpose, MitoTripFactory mitoTripFactory) {
        super(MitoUtil.getRandomObject().nextLong());
        this.dataSet = dataSet;
        this.purpose = purpose;
        householdTypeManager = new HouseholdTypeManager(purpose);
        this.mitoTripFactory = mitoTripFactory;
    }

    @Override
    public Tuple<Purpose, Map<MitoHousehold, List<MitoTrip>>> call() {
        logger.info("  Generating trips with purpose " + purpose + " (multi-threaded)");
        logger.info("Created trip frequency distributions for " + purpose);
        logger.info("Started assignment of trips for hh, purpose: " + purpose);
        final Iterator<MitoHousehold> iterator = dataSet.getModelledHouseholds().values().iterator();
        for (; iterator.hasNext(); ) {
            generateTripsForHousehold(iterator.next());
        }
        return new Tuple<>(purpose, tripsByHH);
    }


    private void generateTripsForHousehold(MitoHousehold hh) {
        HouseholdType hhType = householdTypeManager.determineHouseholdType(hh);
        if (hhType == null) {
            logger.error("Could not create trips for Household " + hh.getId() + " for Purpose " + purpose + ": No Household Type applicable");
            return;
        }
        Integer[] tripFrequencies = householdTypeManager.getTripFrequenciesForHouseholdType(hhType);
        if (tripFrequencies == null) {
            logger.error("Could not find trip frequencies for this hhType/Purpose: " + hhType.getId() + "/" + purpose);
            return;
        }
        if (MitoUtil.getSum(tripFrequencies) == 0) {
            //logger.info("No trips for this hhType/Purpose: " + hhType.getId() + "/" + purpose);
            return;
        }

        List<MitoTrip> trips = new ArrayList<>();
        int numberOfTrips = selectNumberOfTrips(tripFrequencies);
        for (int i = 0; i < numberOfTrips; i++) {
            MitoTrip trip = createTrip(hh);
            if (trip != null) {
                trips.add(trip);
            }
        }
        tripsByHH.put(hh, trips);
    }

    private int selectNumberOfTrips(Integer[] tripFrequencies) {
        double[] probabilities = new double[tripFrequencies.length];
        for (int i = 0; i < tripFrequencies.length; i++) {
            probabilities[i] = (double) tripFrequencies[i];
        }
        return MitoUtil.select(probabilities, random);
    }

    private MitoTrip createTrip(MitoHousehold hh) {
        boolean dropThisTrip = reduceTripGenAtStudyAreaBorder(hh.getHomeZone());
        if (dropThisTrip) {
            DROPPED_TRIPS_AT_BORDER_COUNTER.incrementAndGet();
            return null;
        }
        return mitoTripFactory.createTrip(TRIP_ID_COUNTER.incrementAndGet(), purpose);
    }

    private boolean reduceTripGenAtStudyAreaBorder(MitoZone tripOrigin) {
        if (dropAtBorder) {
            float damper = dataSet.getZones().get(tripOrigin.getId()).getReductionAtBorderDamper();
            return random.nextFloat() < damper;
        }
        return false;
    }
}
