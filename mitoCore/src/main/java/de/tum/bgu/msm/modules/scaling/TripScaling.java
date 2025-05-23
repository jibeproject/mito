package de.tum.bgu.msm.modules.scaling;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.modules.Module;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class TripScaling extends Module {

    private double tripScalingFactor;
    private static final Logger logger = LogManager.getLogger(TripScaling.class);

    public TripScaling(DataSet dataSet, List<Purpose> purposes) {
        super(dataSet, purposes);
        tripScalingFactor = Double.parseDouble(Resources.instance.getString(Properties.TRIP_SCALING_FACTOR));
    }

    @Override
    public void run() {

        scaleTrips();
    }

    private void scaleTrips() {

        dataSet.getTrips().values().forEach(trip -> {
            if (MitoUtil.getRandomObject().nextDouble() < tripScalingFactor) {
                dataSet.addTripToSubsample(trip);
            }
        });

        logger.info("Trips scaled down. The sub-sample of trips contains " + dataSet.getTripSubsample().size() + " trips.");

    }
}
