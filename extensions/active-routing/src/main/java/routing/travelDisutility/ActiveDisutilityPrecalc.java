package routing.travelDisutility;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;
import routing.ActiveConfigGroup;


public class ActiveDisutilityPrecalc extends ActiveDisutility {

    private final double[] disutilities;

    // Custom parameters
    public ActiveDisutilityPrecalc(Network network, ActiveConfigGroup activeConfigGroup, TravelTime timeCalculator) {
        super(activeConfigGroup, timeCalculator);
        this.disutilities = new double[Id.getNumberOfIds(Link.class) * attributeCount];
        precalculateAttributeValues(network, activeConfigGroup.getMode());
    }

    private void precalculateAttributeValues(Network network, String mode) {
        for(int i = 0 ; i < attributeCount ; i++) {
            int nonFiniteCount = 0;
            for(Link link : network.getLinks().values()) {
                double value = attributes.get(i).applyAsDouble(link);
                // A non-finite attribute (e.g. from a NaN link attribute in the network data)
                // would make the disutility non-finite and corrupt least-cost path search.
                // Treat it as 0 (no street environment penalty) and report how often it happened.
                if (!Double.isFinite(value)) {
                    value = 0.;
                    nonFiniteCount++;
                }
                disutilities[link.getId().index() * attributeCount + i] = value;
            }
            if (nonFiniteCount > 0) {
                LogManager.getLogger(ActiveDisutilityPrecalc.class).warn("Attribute " + i + " for mode '" +
                        mode + "' was NaN or infinite on " + nonFiniteCount + " of " +
                        network.getLinks().size() + " links; treated as 0 for routing disutility.");
            }
        }
    }

    @Override
    double getAttribute(int i, Link link) {
        return disutilities[link.getId().index() * attributeCount + i];
    }

}

