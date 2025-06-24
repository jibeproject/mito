package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable;
import de.tum.bgu.msm.resources.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.cam.mrc.phm.util.parseMEL;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class TripAttractionRatesReaderMEL extends AbstractCsvReader {

    private static final Logger logger = LogManager.getLogger(TripAttractionRatesReaderMEL.class);
    private final Map<Purpose, Integer> indexForPurpose = new EnumMap<>(Purpose.class);
    private int variableIndex;
    private final Purpose purpose;

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
        logger.info("Processing purpose: {}", purpose.name());
        variableIndex = parseMEL.findPositionInArray("poi", header);

        int purposeIndex = parseMEL.findPositionInArray(purpose.name(), header);
        if (purposeIndex >= 0) {
            indexForPurpose.put(purpose, purposeIndex);
        } else {
            logger.warn("{} not found in header;  rates will be set to 0.0 for this purpose.", purpose.name());
        }
    }

    @Override
    protected void processRecord(String[] record) {
        ExplanatoryVariable variable = ExplanatoryVariable.valueOf(record[variableIndex]);
        if (!indexForPurpose.containsKey(purpose)) {
            purpose.setTripAttractionForVariable(variable, 0.0);
        } else {
            double rate = Double.parseDouble(record[indexForPurpose.get(purpose)]);
            purpose.setTripAttractionForVariable(variable, rate);
        }
    }
}