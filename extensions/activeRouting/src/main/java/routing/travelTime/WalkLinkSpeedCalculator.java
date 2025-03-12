package routing.travelTime;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.vehicles.Vehicle;

public interface WalkLinkSpeedCalculator extends LinkSpeedCalculator {
	/**
	 * @deprecated -- I find it weird that we need a separate interface for something that it elsewhere in the code expressed by (..., ..., null,
	 * null).  Maybe should use "optional"??  kai, dec'22
	 */
	double getMaximumVelocityForLink(Link link, Vehicle vehicle);
}
