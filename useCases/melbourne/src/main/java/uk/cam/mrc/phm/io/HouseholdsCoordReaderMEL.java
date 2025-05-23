package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.api.core.v01.network.Node;
import uk.cam.mrc.phm.util.parseMEL;


import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Qin on 02.07.2018.
 */
public class HouseholdsCoordReaderMEL extends AbstractCsvReader {

    private int posHHId = -1;
    private int posCoordX = -1;
    private int posCoordY = -1;
    private int posTAZId = -1;
    private final Map<Integer, List<Node>> nodesByZone = new ConcurrentHashMap<>();

    private SimpleFeature shapeFeature; // Alona added

    private static final Logger logger = LogManager.getLogger(HouseholdsCoordReaderMEL.class);

    public HouseholdsCoordReaderMEL(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        logger.info("Reading household microlocation coordinate from dwelling file");
        Path filePath = Resources.instance.getDwellingsFilePath();
        super.read(filePath, ",");
    }

    @Override
    protected void processHeader(String[] header) {
        header = Arrays.stream(header).map(
                h -> h.replace("\"", "").trim()
        ).toArray(String[]::new);
        posHHId = MitoUtil.findPositionInArray("hhID", header);
        posTAZId = MitoUtil.findPositionInArray("zone", header);
        //posCoordX = MitoUtil.findPositionInArray("coordX", header);
        //posCoordY = MitoUtil.findPositionInArray("coordY", header);
    }

    @Override
    protected void processRecord(String[] record) {
        int hhId = parseMEL.intParse(record[posHHId]);


        //vacant dwellings
        if (hhId > 0) {
            MitoHousehold hh = dataSet.getHouseholds().get(hhId);
            if (hh == null) {
                logger.warn(String.format("Household %d does not exist in mito.", hhId));
                return;
            }
            int taz = parseMEL.zoneParse(record[posTAZId]);
            MitoZone zone = dataSet.getZones().get(taz);
            if(zone == null) {
                logger.warn(String.format("Household %d is supposed to live in zone %d but this zone does not exist.", hhId, taz));
            }


            /*Coordinate homeLocation = new Coordinate(
            		Double.parseDouble(record[posCoordX]), Double.parseDouble(record[posCoordY]));
            */
            hh.setHomeLocation(zone.getRandomCoord(MitoUtil.getRandomObject()));
            hh.setHomeZone(zone);
            zone.addHousehold(hh);
        }
    }

}
