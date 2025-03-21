package de.tum.bgu.msm.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds person objects for the Microsimulation Transport Orchestrator (MITO)
 * @author Rolf Moeckel
 * Created on June 8, 2017 in Munich, Germany
 *
 */
public class MitoPerson7days extends MitoPersonImpl {

    private static final Logger logger = LogManager.getLogger(MitoPerson7days.class);
    private ModeSet modeSet = ModeSet.AutoPtCycleWalk;

    public MitoPerson7days(int id, MitoHousehold household, MitoOccupationStatus mitoOccupationStatus, MitoOccupation occupation, int age, MitoGender mitoGender, boolean driversLicense) {
        super(id,household,mitoOccupationStatus,occupation,age,mitoGender,driversLicense);
    }

    public ModeSet getModeSet() {
        return modeSet;
    }

    public void setModeSet(ModeSet modeSet) {
        this.modeSet = modeSet;
    }
}
