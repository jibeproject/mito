package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoJob;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.data.jobTypes.JobTypeFactory;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import uk.cam.mrc.phm.util.parseMEL;

import java.nio.file.Path;

/**
 * Created by Nico on 17.07.2017.
 */
public class JobReaderMEL extends AbstractCsvReader {

    private static final Logger logger = LogManager.getLogger(JobReaderMEL.class);
    private final JobTypeFactory factory;

    private int posId = -1;
    private int posMicroBuildingId = -1;
    private int posZone = -1;
    private int posWorker = -1;
    private int posType = -1;
    private int posJobCoordX = -1;
    private int posJobCoordY = -1;

    public JobReaderMEL(DataSet dataSet, JobTypeFactory factory) {
        super(dataSet);
        this.factory = factory;
    }

    @Override
    public void read() {
        Path filePath = Resources.instance.getJobsFilePath();
        logger.info("Reading job micro data from ascii file ({})", filePath);
        super.read(filePath, ",");
    }

    @Override
    protected void processHeader(String[] header) {
        header = parseMEL.stringParse(header);
        posId = MitoUtil.findPositionInArray("id", header);
        posMicroBuildingId = MitoUtil.findPositionInArray("microBuildingID", header);
        posZone = MitoUtil.findPositionInArray("zone", header);
        posWorker = MitoUtil.findPositionInArray("personId", header);
        posType = MitoUtil.findPositionInArray("type", header);
        posJobCoordX = MitoUtil.findPositionInArray("coordX", header);
        posJobCoordY = MitoUtil.findPositionInArray("coordY", header);
    }

    @Override
    protected void processRecord(String[] record) {
        int id = Integer.parseInt(record[posId]);
        long microBuildingId = Long.parseLong(record[posMicroBuildingId]);
        int zoneId = parseMEL.zoneParse(record[posZone]);
        int worker = Integer.parseInt(record[posWorker]);
        String type = parseMEL.stringParse(record[posType]);
        if (worker > 0) {
            MitoZone zone = dataSet.getZones().get(zoneId);
            if (zone == null) {
                logger.warn(String.format("Job %d refers to non-existing zone %d! Ignoring it.", id, zoneId));
                //return null;
            }

            try {
                zone.addEmployeeForType(factory.getType(type.toUpperCase().replaceAll("\"","")));
            } catch (IllegalArgumentException e) {
                //logger.error("Job Type " + type + " used in job microdata but is not defined");
            }

            Coordinate coordinate;
            if(microBuildingId != -1) {
                coordinate = (new Coordinate(Double.parseDouble(record[posJobCoordX]),Double.parseDouble(record[posJobCoordY])));
            } else {
                coordinate = zone.getCentroid().getCoordinate();
            }
            MitoJob job = new MitoJob(zone, coordinate, id);
            dataSet.addJob(job);
        }
    }
}
