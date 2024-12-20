package routing.travelDisutility;


import de.tum.bgu.msm.data.Purpose;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import routing.BicycleConfigGroup;
import routing.components.Gradient;
import routing.components.JctStress;
import routing.components.LinkAmbience;
import routing.components.LinkStress;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom walk and bicycle disutility for JIBE
 * based on BicycleTravelDisutility by Dominik Ziemke
 */
public class BikeTravelDisutilityPreCalc implements TravelDisutility {

    private final static Logger logger = LogManager.getLogger(BikeTravelDisutilityPreCalc.class);
    private final TravelTime timeCalculator;
    private final BicycleConfigGroup bicycleConfigGroup;
    private Network network;
    private String[] purposeList;
    private Map<String, Map<Id<Link>, Double>> disutilitiesByPurpose = new HashMap<>();

    // Custom parameters
    public BikeTravelDisutilityPreCalc(Network network, String[] purposeList, BicycleConfigGroup bicycleConfigGroup, TravelTime timeCalculator) {
        this.timeCalculator = timeCalculator;
        this.bicycleConfigGroup = bicycleConfigGroup;
        this.network = network;
        this.purposeList = purposeList;
        precalculateDisutility();
    }

    public void precalculateDisutility() {
        logger.info("start calculating disutilities for bikes.");
        for(String purpose : purposeList){
            disutilitiesByPurpose.put(purpose, new HashMap<>());
            for(Link link : network.getLinks().values()) {
                calculateDisutility(link, purpose);
            }
        }
        logger.info("precalculated disutilities.");
    }

    public void calculateDisutility(Link link, String purpose) {
        if(link.getAllowedModes().contains(TransportMode.bike)) {
            double linkTime = timeCalculator.getLinkTravelTime(link, 0., null, null);
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

            disutilitiesByPurpose.get(purpose).put(link.getId(), disutility);

        }
    }


    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        String purpose = person.getAttributes().getAttribute("purpose").toString();
        return disutilitiesByPurpose.get(purpose).get(link.getId());
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0;
    }

}
