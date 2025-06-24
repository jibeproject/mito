package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import uk.cam.mrc.phm.util.parseMEL;

import java.util.Arrays;

public class CalibrationRegionMapReaderMEL extends AbstractCsvReader {

    private int zoneIndex;
    private int regionIndex;

    public CalibrationRegionMapReaderMEL(DataSet dataSet) {
        super(dataSet);
    }
    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(CalibrationRegionMapReaderMEL.class);

    @Override
    protected void processHeader(String[] header) {
//        logger.info(Arrays.toString(header));
        header = Arrays.stream(header).map(
                h -> h.replace("\"", "").trim()
        ).toArray(String[]::new);
        zoneIndex = MitoUtil.findPositionInArray("SA1_MAIN16", header);
        regionIndex = MitoUtil.findPositionInArray("calibrationRegion", header);
    }

    @Override
    protected void processRecord(String[] record) {
        int zone = parseMEL.zoneParse(record[zoneIndex]);
        String region = record[regionIndex];

        dataSet.getModeChoiceCalibrationData().getZoneToRegionMap().put(zone, region);
        //return null;
    }

    @Override
    public void read() {
        super.read(Resources.instance.getCalibrationRegionsPath(), ",");
    }
}
