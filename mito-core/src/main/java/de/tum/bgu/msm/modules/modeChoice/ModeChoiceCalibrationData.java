package de.tum.bgu.msm.modules.modeChoice;

import de.tum.bgu.msm.data.*;

import java.util.List;
import java.util.Map;

public interface ModeChoiceCalibrationData {
    double[] getCalibrationFactorsAsArray(Purpose tripPurpose, Location tripOrigin);

    Map<String, Map<Purpose, Map<Mode, Double>>> getObservedModalShare();

    Map<String, Map<Purpose, Map<Mode, Double>>> getCalibrationFactors();

    Map<Integer, String> getZoneToRegionMap();

    void updateCalibrationCoefficients(DataSet dataSet, int iteration, List<Purpose> purposes);

    void close();
}
