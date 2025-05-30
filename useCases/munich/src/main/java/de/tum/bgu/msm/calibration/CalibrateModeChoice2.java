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
class CalibrateModeChoice2 {

    private static final Logger logger = LogManager.getLogger(CalibrateModeChoice2.class);

    public static void main(String[] args) {
        logger.info("Started the Microsimulation Transport Orchestrator (MITO)");
        MitoModel2ForModeChoiceCalibration model = MitoModel2ForModeChoiceCalibration.standAloneModel(args[0], MunichImplementationConfig.get());
        model.run();


    }
}
