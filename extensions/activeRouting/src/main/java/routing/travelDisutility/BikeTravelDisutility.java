package routing.travelDisutility;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import routing.BicycleConfigGroup;
import routing.components.Gradient;
import routing.components.JctStress;
import routing.components.LinkAmbience;
import routing.components.LinkStress;

/**
 * Custom walk and bicycle disutility for JIBE
 * based on BicycleTravelDisutility by Dominik Ziemke
 */
public class BikeTravelDisutility implements TravelDisutility {

    private final static Logger logger = Logger.getLogger(BikeTravelDisutility.class);
    private final TravelTime timeCalculator;
    private final BicycleConfigGroup bicycleConfigGroup;

    // Custom parameters
    public BikeTravelDisutility(BicycleConfigGroup bicycleConfigGroup, TravelTime timeCalculator) {
        this.timeCalculator = timeCalculator;
        this.bicycleConfigGroup = bicycleConfigGroup;

    }


    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        if(link.getAllowedModes().contains(TransportMode.bike)) {

            String purpose = person.getAttributes().getAttribute("purpose").toString();

            double linkTime = timeCalculator.getLinkTravelTime(link, 0., null, vehicle);
            double linkLength = link.getLength();

            //speed
            double speed = Math.min(1.,link.getFreespeed() / 22.35);

            // Gradient factor
            double gradient = Math.max(Math.min(Gradient.getGradient(link),0.5),0.);

            // VGVI
            double vgvi = Math.max(0.,0.81 - LinkAmbience.getVgviFactor(link));

            // Link stress
            double linkStress = LinkStress.getStress(link,TransportMode.bike);

            // Junction stress
            double jctStress = 0;
            if((boolean) link.getAttributes().getAttribute("crossVehicles")) {
                double junctionWidth = Math.min(linkLength,(double) link.getAttributes().getAttribute("crossWidth"));
                jctStress = (junctionWidth / linkLength) * JctStress.getStress(link,TransportMode.bike);
            }

            // Link disutility
            double disutility = linkTime * (1 +
                    bicycleConfigGroup.getMarginalCostGradient().get(purpose) * gradient +
                    bicycleConfigGroup.getMarginalCostVgvi().get(purpose) * vgvi +
                    bicycleConfigGroup.getMarginalCostLinkStress().get(purpose) * linkStress +
                    bicycleConfigGroup.getMarginalCostJctStress().get(purpose) * jctStress +
                    bicycleConfigGroup.getMarginalCostSpeed().get(purpose) * speed);


            if(Double.isNaN(disutility)) {
                throw new RuntimeException("Null JIBE disutility for link " + link.getId().toString());
            }

            return disutility;

        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0;
    }

}
