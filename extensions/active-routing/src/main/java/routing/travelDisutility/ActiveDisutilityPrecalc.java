package routing.travelDisutility;

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
        precalculateAttributeValues(network);
    }

    private void precalculateAttributeValues(Network network) {
        for(int i = 0 ; i < attributeCount ; i++) {
            for(Link link : network.getLinks().values()) {
                disutilities[link.getId().index() * attributeCount + i] = attributes.get(i).applyAsDouble(link);
            }
        }
    }

    @Override
    double getAttribute(int i, Link link) {
        return disutilities[link.getId().index() * attributeCount + i];
    }

}

