package de.tum.bgu.msm;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Map;

public class HourlyVolumeEventHandler implements LinkEnterEventHandler {

    private final IdMap<Link, Map<Integer, Integer>> bikeVolumes = new IdMap<>(Link.class);
    private final IdMap<Link, Map<Integer, Integer>> pedVolumes = new IdMap<>(Link.class);
    private final IdMap<Link, Map<Integer, Integer>> carVolumes = new IdMap<>(Link.class);
    private final IdMap<Link, Map<Integer, Integer>> truckVolumes = new IdMap<>(Link.class);

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Link> linkId = event.getLinkId();

       String[] idSplit = event.getVehicleId().toString().split("_");
        String mode = idSplit[idSplit.length - 1];
        int hour = (int) (event.getTime() /3600);
        if(mode.equals("bike")) {
            bikeVolumes.putIfAbsent(linkId, new HashMap<>());
            bikeVolumes.get(linkId).put(hour, bikeVolumes.get(linkId).getOrDefault(hour, 0) + 1);
        } else if (mode.equals("walk")) {
            pedVolumes.putIfAbsent(linkId, new HashMap<>());
            pedVolumes.get(linkId).put(hour, pedVolumes.get(linkId).getOrDefault(hour, 0) + 1);
        } else if (mode.equals("truck")) {
            truckVolumes.putIfAbsent(linkId, new HashMap<>());
            truckVolumes.get(linkId).put(hour, truckVolumes.get(linkId).getOrDefault(hour, 0) + 1);
        } else {
            carVolumes.putIfAbsent(linkId, new HashMap<>());
            carVolumes.get(linkId).put(hour, carVolumes.get(linkId).getOrDefault(hour, 0) + 1);
        }
    }

    public IdMap<Link, Map<Integer, Integer>> getBikeVolumes() {
        return bikeVolumes;
    }

    public IdMap<Link, Map<Integer, Integer>> getPedVolumes() {
        return pedVolumes;
    }

    public IdMap<Link, Map<Integer, Integer>> getCarVolumes() {
        return carVolumes;
    }

    public IdMap<Link, Map<Integer, Integer>> getTruckVolumes() {
        return truckVolumes;
    }
}
