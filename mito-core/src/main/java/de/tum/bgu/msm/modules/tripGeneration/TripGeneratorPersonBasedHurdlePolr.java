package de.tum.bgu.msm.modules.tripGeneration;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.io.input.readers.TripGenerationHurdleCoefficientReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import de.tum.bgu.msm.util.concurrent.RandomizableConcurrentFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;

public class TripGeneratorPersonBasedHurdlePolr extends RandomizableConcurrentFunction<Tuple<Purpose, Map<MitoPerson, Integer>>> implements TripGenerator {

    private static final Logger logger = LogManager.getLogger(TripGeneratorPersonBasedHurdlePolr.class);
    private Map<MitoPerson, Integer> tripCountsByPP = new LinkedHashMap<>();
    private final DataSet dataSet;
    private final Purpose purpose;
    private final TripGenPredictor tripGenerationCalculator;
    private Map<String, Double> binLogCoef;
    private Map<String, Double> polrCoef;

    protected TripGeneratorPersonBasedHurdlePolr(DataSet dataSet, Purpose purpose, TripGenPredictor tripGenerationCalculator) {
        super(MitoUtil.getRandomObject().nextLong());
        this.dataSet = dataSet;
        this.purpose = purpose;
        this.tripGenerationCalculator = tripGenerationCalculator;
        this.binLogCoef =
                new TripGenerationHurdleCoefficientReader(dataSet, purpose,
                        Resources.instance.getTripGenerationCoefficientsHurdleBinaryLogit()).readCoefficientsForThisPurpose();
        this.polrCoef =
                new TripGenerationHurdleCoefficientReader(dataSet, purpose,
                        Resources.instance.getTripGenerationCoefficientsHurdleOrderedLogit()).readCoefficientsForThisPurpose();

    }

    @Override
    public Tuple<Purpose, Map<MitoPerson, Integer>> call() throws Exception {
        logger.info("  Generating trips with purpose " + purpose + " (multi-threaded)");
        logger.info("Created trip frequency distributions for " + purpose);
        logger.info("Started assignment of trips for hh, purpose: " + purpose);
        final Iterator<MitoHousehold> iterator = dataSet.getModelledHouseholds().values().iterator();
        for (; iterator.hasNext(); ) {
            generateTripsForHousehold(iterator.next());
        }
        return new Tuple<>(purpose, tripCountsByPP);
    }

    private void generateTripsForHousehold(MitoHousehold hh) {
        for (MitoPerson person : hh.getPersons().values()) {
            int numberOfTrips = polrEstimateTrips(person);
            tripCountsByPP.put(person, numberOfTrips);
        }
    }
    private int polrEstimateTrips (MitoPerson pp) {
        double randomNumber = random.nextDouble();
        double binaryUtility = tripGenerationCalculator.getPredictor(pp.getHousehold(),pp, binLogCoef);
        double phi = Math.exp(binaryUtility) / (1 + Math.exp(binaryUtility));
        double mu = tripGenerationCalculator.getPredictor(pp.getHousehold(),pp, polrCoef);

        double[] intercepts = new double[6];
        intercepts[0] = polrCoef.get("1|2");
        intercepts[1] = polrCoef.get("2|3");
        intercepts[2] = polrCoef.get("3|4");
        intercepts[3] = polrCoef.get("4|5");
        intercepts[4] = polrCoef.get("5|6");
        intercepts[5] = polrCoef.get("6|7");

        int i = 0;
        double cumProb = 0;
        double prob = 1 - phi;
        cumProb += prob;

        while(randomNumber > cumProb) {
            i++;
            if(i < 7) {
                prob = Math.exp(intercepts[i-1] - mu) / (1 + Math.exp(intercepts[i-1] - mu));
            } else {
                prob = 1;
            }
            if(i > 1) {
                prob -= Math.exp(intercepts[i-2] - mu) / (1 + Math.exp(intercepts[i-2] - mu));
            }
            cumProb += phi * prob;
        }
        return i;
    }
}
