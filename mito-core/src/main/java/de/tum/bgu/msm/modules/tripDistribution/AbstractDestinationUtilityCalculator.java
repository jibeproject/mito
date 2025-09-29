package de.tum.bgu.msm.modules.tripDistribution;

import de.tum.bgu.msm.data.MitoPerson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractDestinationUtilityCalculator {

    protected double[] distanceParams;
    protected double impedanceParam;
    protected double attractionParam = 1.;

    // Use immutable singleton list for better memory efficiency
    private static final List<Predicate<MitoPerson>> DEFAULT_CATEGORIES =
        Collections.singletonList(person -> true);

    public final double calculateUtility(double attraction, double travelDistance, int index) {
        if(attraction == 0.) {
            return 0.;
        }

        // Optimize: combine exponential operations and avoid Math.pow for attractionParam = 1
        double distanceImpedance = distanceParams[index] * travelDistance;
        double totalImpedance = impedanceParam + distanceImpedance;
        double expImpedance = Math.exp(totalImpedance);
        if (attractionParam == 1.) {
            // Common case: avoid Math.pow when attraction param is 1
            // Additional optimization: avoid multiplication when attraction is 1
            if (attraction == 1.) {
                return expImpedance;
            }
            return expImpedance * attraction;
        } else {
            // General case: use Math.pow only when necessary
            return expImpedance * Math.pow(attraction, attractionParam);
        }
    }

    public void adjustDistanceParams(double[] adjustment, Logger logger) {
        assert (adjustment.length == distanceParams.length);
        for(int i = 0 ; i < adjustment.length ; i++) {
            distanceParams[i] *= adjustment[i];
        }
        // Optimize logging: check level first and use parameterized logging
        if (logger.isInfoEnabled()) {
            logger.info("Adjusted distance parameters. New parameters: {}", Arrays.toString(distanceParams));
        }
    }

    public List<Predicate<MitoPerson>> getCategories() {
        // Return cached immutable singleton list
        return DEFAULT_CATEGORIES;
    }
}
