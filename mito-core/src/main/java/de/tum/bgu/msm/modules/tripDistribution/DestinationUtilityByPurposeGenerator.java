package de.tum.bgu.msm.modules.tripDistribution;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.data.travelDistances.TravelDistances;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.Callable;

public class DestinationUtilityByPurposeGenerator implements Callable<Triple<Purpose,Integer, IndexedDoubleMatrix2D>> {

    private final static Logger logger = LogManager.getLogger(DestinationUtilityByPurposeGenerator.class);

    private final AbstractDestinationUtilityCalculator calculator;
    private final Purpose purpose;
    private final Map<Integer, MitoZone> zones;
    private final TravelDistances travelDistances;
    private final int categoryIndex;


     DestinationUtilityByPurposeGenerator(Purpose purpose, DataSet dataSet, AbstractDestinationUtilityCalculator calculator, int categoryIndex) {
        this.purpose = purpose;
        this.zones = dataSet.getZones();
        this.travelDistances = dataSet.getTravelDistancesNMT();
        this.categoryIndex = categoryIndex;
        this.calculator = calculator;
    }

    @Override
    public ImmutableTriple<Purpose, Integer, IndexedDoubleMatrix2D> call() {
        final IndexedDoubleMatrix2D utilityMatrix = new IndexedDoubleMatrix2D(zones.values(), zones.values());
        for (MitoZone origin : zones.values()) {
            for (MitoZone destination : zones.values()) {
                double tripAttraction = destination.getTripAttraction(purpose);
                double distances = travelDistances.getTravelDistance(origin.getId(), destination.getId());
                final double utility =  calculator.calculateUtility(tripAttraction,
                        distances,categoryIndex);
                if (Double.isInfinite(utility) || Double.isNaN(utility)) {
                    throw new RuntimeException(utility + " utility calculated! Please check calculation!" +
                            " Origin: " + origin + " | Destination: " + destination + " | Distance: " + distances +
                            " | Purpose: " + purpose + " | attraction rate: " + tripAttraction);
                }
                utilityMatrix.setIndexed(origin.getId(), destination.getId(), utility);
            }
        }
        logger.info("Utility matrix for purpose " + purpose + " category " + categoryIndex + " done.");
        return new ImmutableTriple<>(purpose, categoryIndex, utilityMatrix);
    }
}
