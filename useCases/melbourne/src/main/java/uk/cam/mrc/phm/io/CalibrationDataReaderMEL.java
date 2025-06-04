package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.Mode;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

public class CalibrationDataReaderMEL extends AbstractCsvReader {

    private int regionIndex;
    private int purposeIndex;
    private int modeIndex;
    private int shareIndex;
    private int kIndex;


    public CalibrationDataReaderMEL(DataSet dataSet) {
        super(dataSet);
    }

    private static final Logger logger = LogManager.getLogger(CalibrationDataReaderMEL.class);
    @Override
    protected void processHeader(String[] header) {
        regionIndex = MitoUtil.findPositionInArray("calibrationRegion", header);
        purposeIndex = MitoUtil.findPositionInArray("purpose", header);
        modeIndex = MitoUtil.findPositionInArray("mode", header);
        shareIndex = MitoUtil.findPositionInArray("share", header);
        kIndex = MitoUtil.findPositionInArray("factor", header);
    }

    @Override
    protected void processRecord(String[] record) {
        //  logger.info(Arrays.toString(record));
        String region = record[regionIndex];
        String purposeString = record[purposeIndex];

        // Skip processing if purpose is 'NA', 'business' or unkown
        if ("NA".equalsIgnoreCase(purposeString)) {
            return;
        }
        if ("business".equalsIgnoreCase(purposeString)) {
           return;
        }
        if ("unknown".equalsIgnoreCase(purposeString)) {
            return;
        }
        Purpose purpose = Purpose.valueOf(record[purposeIndex]);
        Mode mode = Mode.valueOf(record[modeIndex]);

        double share = Double.parseDouble(record[shareIndex]);
        double factor = Double.parseDouble(record[kIndex]);

        dataSet.getModeChoiceCalibrationData().getObservedModalShare().putIfAbsent(region, new HashMap<>());
        dataSet.getModeChoiceCalibrationData().getObservedModalShare().get(region).putIfAbsent(purpose, new HashMap<>());
        dataSet.getModeChoiceCalibrationData().getObservedModalShare().get(region).get(purpose).put(mode, share);

        dataSet.getModeChoiceCalibrationData().getCalibrationFactors().putIfAbsent(region, new HashMap<>());
        dataSet.getModeChoiceCalibrationData().getCalibrationFactors().get(region).putIfAbsent(purpose, new HashMap<>());
        dataSet.getModeChoiceCalibrationData().getCalibrationFactors().get(region).get(purpose).put(mode, factor);

        //return null;
    }

    @Override

    public void read() {
        Path filePath = Resources.instance.getCalibrationFactorsPath();
        logger.info("Reading calibration data from file: {}", filePath);
        super.read(filePath, ",");
    }
}

