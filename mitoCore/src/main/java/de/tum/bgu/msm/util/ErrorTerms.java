package de.tum.bgu.msm.util;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.GumbelDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.Well19937c;
import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public class ErrorTerms<E extends Enum<E>> {

    private static final Logger logger = Logger.getLogger(ErrorTerms.class);
    private final Class<E> enumClass;
    private final EnumSet<E> choiceSet;
    private final UniformRealDistribution uniformRealDitribution;
    private final ExponentialDistribution exponentialDistribution;
    private final GumbelDistribution gumbelDistribution;
    private final List<Tuple<EnumSet<E>, Double>> nests;

    public ErrorTerms(Class<E> enumClass, EnumSet<E> choiceSet, List<Tuple<EnumSet<E>, Double>> nests, int seed) {
        this.enumClass = enumClass;
        this.choiceSet = choiceSet;
        this.gumbelDistribution = new GumbelDistribution(new Well19937c(seed),0,1);
        this.nests = nests;
        if(nests != null) {
            this.uniformRealDitribution = new UniformRealDistribution(new Well19937c(seed + 1),0,1);
            this.exponentialDistribution = new ExponentialDistribution(new Well19937c(seed + 2),1);
        } else {
            this.uniformRealDitribution = null;
            this.exponentialDistribution = null;
        }
    }
//
//    public static void main(String[] args) throws FileNotFoundException {
//        PrintWriter pw = new PrintWriter("corrVarsTest.csv");
//        for(int i = 1 ; i < 1000000 ; i++) {
//            double[] result = getCorrelatedValues(3,0.5);
////            double[] result = gumbelDistribution.sample(3);
//            pw.println(String.format("%.5f;%.5f;%.5f%n",result[0],result[1],result[2]));
//        }
//        pw.close();
//    }

    public EnumMap<E,Double> sampleErrorTerms() {
        EnumMap<E,Double> sim = new EnumMap<>(enumClass);
        if(nests == null) {
            choiceSet.forEach(c -> sim.put(c,gumbelDistribution.sample()));
        } else {
            for (Tuple<EnumSet<E>, Double> nest : nests) {
                EnumSet<E> nestChoices = nest.getFirst();
                double[] nestTerms = getCorrelatedValues(nestChoices.size(),nest.getSecond());
                int i = 0;
                for(E choice : nestChoices) {
                    sim.put(choice,nestTerms[i]);
                    i++;
                }
            }
        }
        return sim;
    }

    public double[] getCorrelatedValues(int d, double lambda) {
        if(lambda > 1 || lambda <= 0) {
            throw new RuntimeException("Nesting parameter must be equal to or less than 1 and greater than 0");
        } else if (lambda == 1) {
            return gumbelDistribution.sample(d);
        } else {
            double s = rpstable(lambda);
            double[] sim = new double[d];
            for (int i = 0 ; i < d ; i++) {
                sim[i] = -Math.log(1. / Math.exp(lambda * (s - Math.log(exponentialDistribution.sample()))));
            }
            return sim;
        }
    }

    public double rpstable(double cexp) {
        double tcexp = 1 - cexp;
        double u = Math.PI * uniformRealDitribution.sample();
        double w = Math.log(exponentialDistribution.sample());
        double a = Math.log(Math.sin(tcexp*u)) + (cexp/tcexp) * Math.log(Math.sin(cexp*u)) - (1/tcexp) * Math.log(Math.sin(u));
        return (tcexp/cexp) * (a - w);
    }
}
