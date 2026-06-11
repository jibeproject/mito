package routing.travelDisutility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import routing.ActiveConfigGroup;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToDoubleFunction;

public class ActiveDisutility implements TravelDisutility {

    private final static Logger logger = LogManager.getLogger(ActiveDisutility.class);
    private final static AtomicBoolean NON_ROUTABLE_LINK_WARNING_ISSUED = new AtomicBoolean(false);

    private final ActiveConfigGroup activeConfigGroup;
    private final TravelTime timeCalculator;
    final List<ToDoubleFunction<Link>> attributes;
    final int attributeCount;

    public ActiveDisutility(ActiveConfigGroup activeConfigGroup, TravelTime timeCalculator) {
        this.activeConfigGroup = activeConfigGroup;
        this.timeCalculator = timeCalculator;
        this.attributes = activeConfigGroup.getAttributes();
        this.attributeCount = attributes.size();
    }

    double getAttribute(int i, Link link) {
        return attributes.get(i).applyAsDouble(link);
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        if(link.getAllowedModes().contains(activeConfigGroup.getMode())) {

            double linkTime = timeCalculator.getLinkTravelTime(link, 0., person, vehicle);
            double[] weights = activeConfigGroup.getWeights().apply(person);

            if(weights == null) {
                // return travel time only
                return linkTime;
            } else {
                // Check size matches
                if(weights.length != attributeCount) {
                    throw new RuntimeException("Size of marginal weights array (" + weights.length + ") does not match size of attributes list (" + attributeCount + ")");
                }

                // compute expansion
                double streetEnvironmentAdjustment = 1.;
                for(int i = 0; i < attributeCount ; i++) {
                    if(weights[i] < 0) {
                        throw new RuntimeException("All active travel disutility weights must be positive!");
                    }
                    streetEnvironmentAdjustment += weights[i] * getAttribute(i, link);
                }

                // return adjusted disutility
                return linkTime * streetEnvironmentAdjustment;
            }
        } else {
            // This link cannot be traversed by the routed mode, so the routing network should
            // have been filtered to exclude it. Returning NaN here corrupts least-cost path
            // search (NaN comparisons are always false, breaking the priority queue ordering),
            // so return a large finite penalty instead.
            if (NON_ROUTABLE_LINK_WARNING_ISSUED.compareAndSet(false, true)) {
                logger.warn("Link " + link.getId() + " does not allow mode '" + activeConfigGroup.getMode() +
                        "'. The routing network should be filtered to the routed mode before routing. " +
                        "Applying a large finite disutility penalty (further warnings suppressed).");
            }
            return Math.max(link.getLength(), 1.) * 1.0e6;
        }
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0;
    }

}

