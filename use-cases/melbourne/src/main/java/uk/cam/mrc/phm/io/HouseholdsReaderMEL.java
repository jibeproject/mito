package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.cam.mrc.phm.util.parseMEL;

import java.nio.file.Path;

/**
 * Created by Nico on 17.07.2017.
 */
public class HouseholdsReaderMEL extends AbstractCsvReader {

    private int posId = -1;
    private int posTaz = -1;
    private int posAutos = -1;
    private static final Logger logger = LogManager.getLogger(HouseholdsReaderMEL.class);
    private static final double scaleFactorForTripGeneration = Resources.instance.getDouble(Properties.SCALE_FACTOR_FOR_TRIP_GENERATION, 1.0);

    public HouseholdsReaderMEL(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        Path filePath = Resources.instance.getHouseholdsFilePath();
        logger.info("  Reading household micro data from ascii file ({})", filePath);
        super.read(filePath, ",");
    }

    @Override
    protected void processHeader(String[] header) {
        header = parseMEL.stringParse(header);
        posId = MitoUtil.findPositionInArray("id", header);
        posTaz = MitoUtil.findPositionInArray("zone", header);
        posAutos = MitoUtil.findPositionInArray("autos", header);
    }

    @Override
    protected void processRecord(String[] record) {
        int id = parseMEL.intParse(record[posId]);
        int autos = Integer.parseInt(record[posAutos]);

        // is the household modelled? (depends on scale factor)
        boolean isModelled = MitoUtil.getRandomObject().nextDouble() < scaleFactorForTripGeneration;

        MitoHousehold hh = new MitoHousehold(id, 0, autos, isModelled);
        dataSet.addHousehold(hh);
    }
}
