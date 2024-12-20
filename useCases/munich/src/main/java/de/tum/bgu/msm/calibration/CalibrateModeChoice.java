package de.tum.bgu.msm.calibration;

import de.tum.bgu.msm.util.MunichImplementationConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements the Transport in Microsimulation Orchestrator (MITO)
 *
 * @author Rolf Moeckel
 * Created on Feb 12, 2017 in Munich, Germany
 */
class CalibrateModeChoice {

    private static final Logger logger = LogManager.getLogger(CalibrateModeChoice.class);

    public static void main(String[] args) {
        logger.info("Started the Microsimulation Transport Orchestrator (MITO)");
        MitoModelForModeChoiceCalibration model = MitoModelForModeChoiceCalibration.standAloneModel(args[0], MunichImplementationConfig.get());
        model.run();


    }
}
