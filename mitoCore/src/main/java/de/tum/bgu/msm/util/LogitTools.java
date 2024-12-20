package de.tum.bgu.msm.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;

public class LogitTools<E extends Enum<E>> {

    private static final Logger logger = LogManager.getLogger(LogitTools.class);
    private final Class<E> enumClass;

    public LogitTools(Class<E> enumClass) {
        this.enumClass = enumClass;
    }


    public EnumMap<E, Double> getProbabilitiesMNL(EnumMap<E, Double> utilities) {

        EnumMap<E, Double> probabilities = new EnumMap<>(enumClass);
        EnumMap<E, Double> expUtils = new EnumMap<>(enumClass);
        double expUtilsSum = 0.;

        for(E option : utilities.keySet()) {
            double expUtil = Math.exp(utilities.get(option));
            expUtils.put(option, expUtil);
            expUtilsSum += expUtil;
        }

        for(E option : utilities.keySet()) {
            probabilities.put(option, expUtils.get(option) / expUtilsSum);
        }

        return probabilities;
    }

    public static <T> T getHighest(Map<T, Double> mappedUtilities, Map<T, Double> errorTerms) {
        T chosen = null;
        double maxUtility = Double.NEGATIVE_INFINITY;
        for(Map.Entry<T,Double> e : mappedUtilities.entrySet()) {
            double utility = e.getValue() + errorTerms.get(e.getKey());
            if(utility > maxUtility) {
                maxUtility = utility;
                chosen = e.getKey();
            }
        }
        if(chosen == null) {
            throw new RuntimeException("No feasible alternative");
        }
        return chosen;
    }

    public EnumMap<E, Double> getProbabilitiesNL(EnumMap<E, Double> utilities, List<Tuple<EnumSet<E>, Double>> nests) {

        EnumMap<E, Double> expOptionUtils = new EnumMap<>(enumClass);
        EnumMap<E, Double> expNestSums = new EnumMap<>(enumClass);
        EnumMap<E, Double> expNestUtils = new EnumMap<>(enumClass);
        EnumMap<E, Double> probabilities = new EnumMap<>(enumClass);
        double expSumRoot = 0;

        for (Tuple<EnumSet<E>, Double> nest : nests) {
            double expNestSum = 0;
            EnumSet<E> nestOptions = EnumSet.copyOf(nest.getFirst());
            nestOptions.retainAll(utilities.keySet());
            double nestingCoefficient = nest.getSecond();
            for (E option : nestOptions) {
                double expOptionUtil = Math.exp(utilities.get(option) / nestingCoefficient);
                expOptionUtils.put(option, expOptionUtil);
                expNestSum += expOptionUtil;
            }
            double expNestUtil = Math.exp(nestingCoefficient * Math.log(expNestSum));
            for (E option : nestOptions) {
                expNestSums.put(option, expNestSum);
                expNestUtils.put(option, expNestUtil);
            }
            expSumRoot += expNestUtil;
        }

        for (E option : utilities.keySet()) {
            if (expNestSums.get(option) == 0) {
                probabilities.put(option, 0.);
            } else {
                probabilities.put(option, (expOptionUtils.get(option) * expNestUtils.get(option)) / (expNestSums.get(option) * expSumRoot));
            }
        }
        return probabilities;
    }

    public EnumMap<E, Double> getProbabilities(EnumMap<E, Double> utilities, List<Tuple<EnumSet<E>, Double>> nests) {

        if(nests == null) {
            return getProbabilitiesMNL(utilities);
        } else {
            return getProbabilitiesNL(utilities, nests);
        }
    }

    public double getLogsumMNL(EnumMap<E, Double> utilities, double scaleParameter) {
        double expSum = 0.;
        for(E option : utilities.keySet()) {
            double expUtil = Math.exp(utilities.get(option) * scaleParameter);
            expSum += expUtil;
        }
        return(Math.log(expSum) / scaleParameter);
    }

    public double getLogsumNL(EnumMap<E, Double> utilities, List<Tuple<EnumSet<E>, Double>> nests, double scaleParameter) {
        double expSumRoot = 0.;
        for(Tuple<EnumSet<E>,Double> nest : nests) {
            double expNestSum = 0;
            EnumSet<E> nestOptions = EnumSet.copyOf(nest.getFirst());
            nestOptions.retainAll(utilities.keySet());
            double nestScaleParameter = scaleParameter / nest.getSecond();
            for(E option : nestOptions) {
                double expOptionUtil = Math.exp(utilities.get(option) * nestScaleParameter);
                expNestSum += expOptionUtil;
            }
            double expNestUtil = Math.exp(scaleParameter * Math.log(expNestSum) / nestScaleParameter);
            expSumRoot += expNestUtil;
        }
        return(Math.log(expSumRoot) / scaleParameter);
    }
}
