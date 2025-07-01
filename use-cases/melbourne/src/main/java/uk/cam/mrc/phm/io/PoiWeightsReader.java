package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.cam.mrc.phm.util.parseMEL;


import java.nio.file.Path;
import java.util.HashMap;

/**
 * @author Nico
 * @author Carl
 */
public class PoiWeightsReader extends AbstractCsvReader {

    private static final Logger logger = LogManager.getLogger(PoiWeightsReader.class);
    private int idIndex;
    private HashMap<String, Integer> poiIndex = new HashMap<>();

    public PoiWeightsReader(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        Path filePath = Resources.instance.getZonesInputFile();
        logger.info("Reading POI weights data from ascii file ({})", filePath);
        super.read(filePath, ",");
    }

    @Override
    protected void processHeader(String[] header) {
        header = parseMEL.stringParse(header);
        idIndex = MitoUtil.findPositionInArray("SA1_MAIN16", header);
        poiIndex.put("EYA",MitoUtil.findPositionInArray("EYA", header));
        poiIndex.put("EDU",MitoUtil.findPositionInArray("EDU", header));
        poiIndex.put("FR",MitoUtil.findPositionInArray("FR", header));
        poiIndex.put("RSPF",MitoUtil.findPositionInArray("RSPF", header));
        poiIndex.put("SCL",MitoUtil.findPositionInArray("SCL", header));
        poiIndex.put("CHR",MitoUtil.findPositionInArray("CHR", header));
        poiIndex.put("EE",MitoUtil.findPositionInArray("EE", header));
        poiIndex.put("FIN",MitoUtil.findPositionInArray("FIN", header));
        poiIndex.put("PHC",MitoUtil.findPositionInArray("PHC", header));
        poiIndex.put("POS",MitoUtil.findPositionInArray("POS", header));
        poiIndex.put("SER",MitoUtil.findPositionInArray("SER", header));
    }

    @Override
    protected void processRecord(String[] record) {
        int zoneId = parseMEL.zoneParse((record[idIndex]));
        for(String poiType : poiIndex.keySet()){
            float weights = Float.parseFloat(record[poiIndex.get(poiType)]);
            dataSet.getZones().get(zoneId).getPoiWeightsByType().put(poiType, (double) weights);
        }

    }
}