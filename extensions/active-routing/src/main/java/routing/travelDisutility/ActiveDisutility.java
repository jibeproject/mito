package routing.travelDisutility;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import routing.ActiveConfigGroup;

import java.util.List;
import java.util.function.ToDoubleFunction;

public class ActiveDisutility implements TravelDisutility {

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
            return Double.NaN;
        }
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0;
    }

}

