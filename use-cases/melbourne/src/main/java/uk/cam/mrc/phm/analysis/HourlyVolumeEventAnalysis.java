package uk.cam.mrc.phm.analysis;

import de.tum.bgu.msm.HourlyVolumeEventHandler;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.logging.Level;

public class HourlyVolumeEventAnalysis {
    private static final String MATSIM_NETWORK = Resources.instance.getString(Properties.MATSIM_NETWORK_FILE);
    private static final String scenarioName = Resources.instance.getString(Properties.SCENARIO_NAME);
    private static final String day = "sunday";
    private static final String year = Resources.instance.getString(Properties.SCENARIO_YEAR);
    private static final String MATSIM_EVENT_ACTIVE = MessageFormat.format(
            "./scenOutput/{0}/matsim/{1}/{2}/bikePed/{1}.output_events.xml.gz",
            scenarioName, year, day
    );
    private static final String MATSIM_VEHICLES_ACTIVE = MessageFormat.format(
            "./scenOutput/{0}/matsim/{1}/{2}/bikePed/{1}.output_vehicles.xml.gz",
            scenarioName, year, day
    );
    private static final String OUTPUT_PATH_ACTIVE = MessageFormat.format(
            "./scenOutput/{0}/matsim/{1}/hourlyVolume_bikePed_{2}.csv",
            scenarioName, year, day
    );
    private static final String MATSIM_EVENT_CAR = MessageFormat.format(
            "./scenOutput/{0}/matsim/{1}/{2}/car/{1}.output_events.xml.gz",
            scenarioName, year, day
    );
    private static final String MATSIM_VEHICLES_CAR = MessageFormat.format(
            "./scenOutput/{0}/matsim/{1}/{2}/car/{1}.output_vehicles.xml.gz",
            scenarioName, year, day
    );
    private static final String OUTPUT_PATH_CAR = MessageFormat.format(
            "./scenOutput/{0}/matsim/{1}/hourlyVolume_carTruck_{2}.csv",
            scenarioName, year, day
    );
    private static final int SCALE_FACTOR_ACTIVE = 1;
    private static final int SCALE_FACTOR_CAR = 10;

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(HourlyVolumeEventAnalysis.class.getName());

    public static void main(String[] args) {

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(MATSIM_NETWORK);

        Vehicles activeVehicles = VehicleUtils.createVehiclesContainer();
        new MatsimVehicleReader(activeVehicles).readFile(MATSIM_VEHICLES_ACTIVE);

        EventsManager eventsManager = new EventsManagerImpl();
        HourlyVolumeEventHandler volumeEventHandler = new HourlyVolumeEventHandler(activeVehicles);
        eventsManager.addHandler(volumeEventHandler);
        EventsUtils.readEvents(eventsManager,MATSIM_EVENT_ACTIVE);

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(OUTPUT_PATH_ACTIVE);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to create PrintWriter for output file", e);
        }

        assert pw != null;
        pw.println("linkId,edgeId,osmId,hour,bike,ped");


        for (Link link :  network.getLinks().values()) {
            String linkId = link.getId().toString();
            String edgeId = link.getAttributes().getAttribute("edgeID").toString();
            String osmId;
            if (link.getAttributes().getAttribute("osmID")== null){
                osmId = "NA";
            }else {
                osmId = link.getAttributes().getAttribute("osmID").toString();
            }

            for(int hour = 0; hour <= 24; hour++) {
                int bikeVolumes = 0;
                int pedVolumes = 0;
                if(volumeEventHandler.getBikeVolumes().get(link.getId())!=null){
                    bikeVolumes = volumeEventHandler.getBikeVolumes().get(link.getId()).getOrDefault(hour, 0) * SCALE_FACTOR_ACTIVE;
                }

                if(volumeEventHandler.getPedVolumes().get(link.getId())!=null){
                    pedVolumes = volumeEventHandler.getPedVolumes().get(link.getId()).getOrDefault(hour, 0) * SCALE_FACTOR_ACTIVE;
                }

                pw.println(linkId + "," + edgeId + "," + osmId + "," + hour + ","  + bikeVolumes + "," + pedVolumes);
            }
        }

        pw.close();

        //car truck flow
//        Vehicles motorVehicles = VehicleUtils.createVehiclesContainer();
//        new MatsimVehicleReader(motorVehicles).readFile(MATSIM_VEHICLES_CAR);

        /*EventsManager eventsManagerCar = new EventsManagerImpl();
        HourlyVolumeEventHandler volumeEventHandlerCar = new HourlyVolumeEventHandler(motorVehicles);
        eventsManagerCar.addHandler(volumeEventHandlerCar);
        EventsUtils.readEvents(eventsManagerCar,MATSIM_EVENT_CAR);

        PrintWriter pwCar = null;
        try {
            pwCar = new PrintWriter(new File(OUTPUT_PATH_CAR));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder headerCar = new StringBuilder();
        headerCar.append("linkId,edgeId,osmId,hour,car,truck");
        pwCar.println(headerCar);


        for (Link link :  network.getLinks().values()) {
            String linkId = link.getId().toString();
            String edgeId = link.getAttributes().getAttribute("edgeID").toString();
            String osmId;
            if (link.getAttributes().getAttribute("osmID")== null){
                osmId = "NA";
            }else {
                osmId = link.getAttributes().getAttribute("osmID").toString();
            }


            for(int hour = 0; hour <= 24; hour++) {
                int carVolumes = 0;
                int truckVolumes = 0;
                if(volumeEventHandlerCar.getCarVolumes().get(link.getId())!=null){
                    carVolumes = volumeEventHandlerCar.getCarVolumes().get(link.getId()).getOrDefault(hour, 0) * SCALE_FACTOR_CAR;
                }

                if(volumeEventHandlerCar.getTruckVolumes().get(link.getId())!=null){
                    truckVolumes = volumeEventHandlerCar.getTruckVolumes().get(link.getId()).getOrDefault(hour, 0) * SCALE_FACTOR_CAR;
                }
                pwCar.println(linkId + "," + edgeId + "," + osmId + "," + hour + ","  + carVolumes + "," + truckVolumes);
            }
        }

        pwCar.close();*/

    }
}
