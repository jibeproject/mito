package de.tum.bgu.msm;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class DailyVolumeEventHandler implements LinkEnterEventHandler {

    private final IdMap<Link, Integer> bikeVolumes = new IdMap<>(Link.class);
    private final IdMap<Link, Integer> pedVolumes = new IdMap<>(Link.class);
    private final IdMap<Link, Integer> carVolumes = new IdMap<>(Link.class);
    private final IdMap<Link, Integer> truckVolumes = new IdMap<>(Link.class);

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Link> linkId = event.getLinkId();

       String[] idSplit = event.getVehicleId().toString().split("_");
        String mode = idSplit[idSplit.length - 1];

        if(mode.equals("bike")) {
            bikeVolumes.put(linkId, bikeVolumes.getOrDefault(linkId,0) + 1);
        } else if (mode.equals("walk")) {
            pedVolumes.put(linkId, pedVolumes.getOrDefault(linkId,0) + 1);
        } else if (mode.equals("truck")) {
            truckVolumes.put(linkId, truckVolumes.getOrDefault(linkId,0) + 1);
        } else {
            carVolumes.put(linkId, carVolumes.getOrDefault(linkId,0) + 1);
        }
    }

    public IdMap<Link, Integer> getBikeVolumes() {
        return bikeVolumes;
    }

    public IdMap<Link, Integer> getPedVolumes() {
        return pedVolumes;
    }

    public IdMap<Link, Integer> getCarVolumes() {
        return carVolumes;
    }

    public IdMap<Link, Integer> getTruckVolumes() {
        return truckVolumes;
    }
}
