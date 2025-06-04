package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.AreaTypes;
import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.nio.file.Path;
import java.util.Arrays;


/**
 * @author Nico
 */
public class ZonesReaderMEL extends AbstractCsvReader {

    private static final Logger logger = LogManager.getLogger(ZonesReaderMEL.class);
    private int idIndex;
    private int areaTypeIndex;
    private static final String ZONE_ID = Resources.instance.getString(Properties.ZONE_SHAPEFILE_ID_FIELD);

    public ZonesReaderMEL(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        Path filePath = Resources.instance.getZonesInputFile();
        logger.info("Reading zones data from ascii file ({})", filePath);
        super.read(filePath, ",");
        mapFeaturesToZones(dataSet);
    }

    private static void mapFeaturesToZones(DataSet dataSet) {
        for (SimpleFeature feature: ShapeFileReader.getAllFeatures(Resources.instance.getZoneShapesInputFile().toString())) {
            int zoneId = Integer.parseInt(feature.getAttribute(ZONE_ID).toString());
            MitoZone zone = dataSet.getZones().get(zoneId);
            if (zone != null){
                zone.setGeometry((Geometry) feature.getDefaultGeometry());
            }else{
                logger.warn("zoneId " + zoneId + " doesn't exist in mito zone system");
            }
        }
    }

    @Override
    protected void processHeader(String[] header) {
        header = Arrays.stream(header).map(
                h -> h.replace("\"", "").trim()
        ).toArray(String[]::new);
        idIndex = MitoUtil.findPositionInArray(ZONE_ID,header);
        areaTypeIndex = MitoUtil.findPositionInArray("urbanType", header);
    }

    @Override
    protected void processRecord(String[] record) {
        int zoneId = Integer.parseInt(record[idIndex]);
        String urbanType = record[areaTypeIndex];
        MitoZone zone = new MitoZone(zoneId, null);
        zone.setAreaTypeR("urban".equals(urbanType)? AreaTypes.RType.URBAN : AreaTypes.RType.RURAL);
        dataSet.addZone(zone);
    }
}
