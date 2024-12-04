package de.tum.bgu.msm.modules.tripGeneration;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.io.input.readers.TripGenerationHurdleCoefficientReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import de.tum.bgu.msm.util.concurrent.RandomizableConcurrentFunction;
import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import umontreal.ssj.probdist.NegativeBinomialDist;

import java.util.*;

public class TripGeneratorPersonBasedHurdleNegBin extends RandomizableConcurrentFunction<Tuple<Purpose, Map<MitoPerson, Integer>>> implements TripGenerator {

    private static final Logger logger = Logger.getLogger(TripGeneratorPersonBasedHurdleNegBin.class);
    private Map<MitoPerson, Integer> tripCountsByPP = new LinkedHashMap<>();

    private final DataSet dataSet;
    private final Purpose purpose;

    private final TripGenPredictor tripGenerationCalculator;

    private Map<String, Double> binLogCoef;
    private Map<String, Double> negBinCoef;

    protected TripGeneratorPersonBasedHurdleNegBin(DataSet dataSet, Purpose purpose, TripGenPredictor tripGenerationCalculator) {
        super(MitoUtil.getRandomObject().nextLong());
        this.dataSet = dataSet;
        this.purpose = purpose;
        this.tripGenerationCalculator = tripGenerationCalculator;
        this.binLogCoef =
                new TripGenerationHurdleCoefficientReader(dataSet, purpose,
                        Resources.instance.getTripGenerationCoefficientsHurdleBinaryLogit()).readCoefficientsForThisPurpose();
        this.negBinCoef =
                new TripGenerationHurdleCoefficientReader(dataSet, purpose,
                        Resources.instance.getTripGenerationCoefficientsHurdleNegativeBinomial()).readCoefficientsForThisPurpose();

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
            int numberOfTrips = hurdleEstimateTrips(person);
            tripCountsByPP.put(person, numberOfTrips);
        }
    }

    private int hurdleEstimateTrips(MitoPerson pp) {
        double randomNumber = random.nextDouble();
        double binaryUtility = tripGenerationCalculator.getPredictor(pp.getHousehold(), pp, binLogCoef);
        double phi = Math.exp(binaryUtility) / (1 + Math.exp(binaryUtility));
        double mu = Math.exp(tripGenerationCalculator.getPredictor(pp.getHousehold(),pp, negBinCoef));
        double theta = negBinCoef.get("theta");

        NegativeBinomialDist nb = new NegativeBinomialDist(theta, theta / (theta + mu));

        double p0_zero = Math.log(phi);
        double p0_count = Math.log(1 - nb.cdf(0));
        double logphi = p0_zero - p0_count;

        int i = 0;
        double cumProb = 0;
        double prob = 1 - Math.exp(p0_zero);
        cumProb += prob;

        while(randomNumber > cumProb) {
            i++;
            prob = Math.exp(logphi + Math.log(nb.prob(i)));
            cumProb += prob;
        }
        return(i);
    }
}
