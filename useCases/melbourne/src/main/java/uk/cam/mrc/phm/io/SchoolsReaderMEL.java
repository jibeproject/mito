package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoSchool;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import uk.cam.mrc.phm.util.parseMEL;

import java.nio.file.Path;
import java.util.Arrays;

public class SchoolsReaderMEL extends AbstractCsvReader {

    private static final Logger logger = LogManager.getLogger(SchoolsReaderMEL.class);

    private int posId = -1;
    private int posZone = -1;
    private int posOccupancy = -1;
    private int posCoordX = -1;
    private int posCoordY = -1;

    public SchoolsReaderMEL(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    protected void processHeader(String[] header) {
        header = Arrays.stream(header).map(
                h -> h.replace("\"", "").trim()
            ).toArray(String[]::new);
        posId = MitoUtil.findPositionInArray("id", header);
        posZone = MitoUtil.findPositionInArray("zone", header);
        posOccupancy = MitoUtil.findPositionInArray("occupancy", header);
        posCoordX = MitoUtil.findPositionInArray("coordX", header);
        posCoordY = MitoUtil.findPositionInArray("coordY", header);
    }

    @Override
    protected void processRecord(String[] record) {
        int id = Integer.parseInt(record[posId]);
        int zoneId = parseMEL.zoneParse(record[posZone]);
        MitoZone zone = dataSet.getZones().get(zoneId);
        int occupancy = parseMEL.intParse(record[posOccupancy]);
        Coordinate coordinate = (new Coordinate(Double.parseDouble(record[posCoordX]),
                Double.parseDouble(record[posCoordY])));
        MitoSchool school = new MitoSchool(zone, coordinate, id);
        dataSet.addSchool(school);
        zone.addSchoolEnrollment(occupancy);
    }

    @Override
    public void read() {
        logger.info("Reading school micro data from ascii file");
        Path filePath = Resources.instance.getSchoolsFilePath();
        super.read(filePath, ",");
    }
}
