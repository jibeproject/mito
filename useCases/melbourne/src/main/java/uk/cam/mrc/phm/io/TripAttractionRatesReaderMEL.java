package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class TripAttractionRatesReaderMEL extends AbstractCsvReader {

    private static final Logger logger = LogManager.getLogger(de.tum.bgu.msm.io.input.readers.TripAttractionRatesReader.class);
    private final Map<Purpose, Integer> indexForPurpose = new EnumMap<>(Purpose.class);
    private int variableIndex;
    private Purpose purpose;

    public TripAttractionRatesReaderMEL(DataSet dataSet, Purpose purpose) {
        super(dataSet);
        this.purpose = purpose;
    }

    @Override
    public void read() {
        super.read(Resources.instance.getTripAttractionRatesFilePath(), ",");
    }

    @Override
    protected void processHeader(String[] header) {
        header = Arrays.stream(header).map(
                h -> h.replace("\"", "").trim()
        ).toArray(String[]::new);
        logger.info("Processing header: " + Arrays.toString(header));
        variableIndex = MitoUtil.findPositionInArray("poi", header);

        indexForPurpose.put(purpose, MitoUtil.findPositionInArray(purpose.name(), header));

    }

    @Override
    protected void processRecord(String[] record) {
        ExplanatoryVariable variable = ExplanatoryVariable.valueOf(record[variableIndex]);
        double rate = Double.parseDouble(record[indexForPurpose.get(purpose)]);
        purpose.setTripAttractionForVariable(variable, rate);
    }
}