package de.tum.bgu.msm.data.travelTimes;

import de.tum.bgu.msm.data.Id;
import de.tum.bgu.msm.data.Location;
import de.tum.bgu.msm.data.Region;
import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.io.input.readers.CsvGzSkimMatrixReader;
import de.tum.bgu.msm.io.output.OmxMatrixWriter;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import de.tum.bgu.msm.util.matrices.Matrices;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxHdf5Datatype;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SkimTravelTimes implements TravelTimes {

    private final static Logger logger = LogManager.getLogger(SkimTravelTimes.class);

    private final ConcurrentMap<String, IndexedDoubleMatrix2D> matricesByMode = new ConcurrentHashMap<>();

    private Map<String, IndexedDoubleMatrix2D> travelTimesFromRegion = new HashMap<>();
    private final Map<String, IndexedDoubleMatrix2D> travelTimesToRegion = new HashMap<>();

    /**
     * Reads a skim matrix from an omx file and stores it for the given mode and year. To allow conversion between units
     * use the factor to multiply all values.
     * @param mode the mode for which the travel times are read
     * @param file the path to the omx file
     * @param matrixName the name of the matrix inside the omx file
     * @param factor a scalar factor which every entry is multiplied with
     */
    public final void readSkim(final String mode, final String file, final String matrixName, final double factor) {
        logger.info("Reading {} skim ({}: {})", mode, file, matrixName);
        try (OmxFile omx = new OmxFile(file)) {
            omx.openReadOnly();
            final Set<String> lookupNames = omx.getLookupNames();
            OmxLookup<?, ?> lookup = null;
            if (!lookupNames.isEmpty()) {
                final Iterator<String> iterator = omx.getLookupNames().iterator();
                final String next = iterator.next();
                lookup = omx.getLookup(next);
                if (!lookup.getOmxJavaType().equals(OmxHdf5Datatype.OmxJavaType.INT)) {
                    throw new IllegalArgumentException("Provided omx matrix lookup " +
                            "is not of type int but of type: " + lookup.getOmxJavaType().name() +
                            " please use int.");
                }
                if (iterator.hasNext()) {
                    logger.warn("More than one lookup was provided. Will use the first one (name: {})", next);
                }
            }

            // Create matrix and immediately convert - don't hold references longer than necessary
            final OmxMatrix<?, ?> timeOmxSkimTransit = omx.getMatrix(matrixName);
            final IndexedDoubleMatrix2D skim = Matrices.convertOmxToDoubleMatrix2D(timeOmxSkimTransit, lookup, factor);

            // Store the matrix result
            matricesByMode.put(mode, skim);

            // Clear regional matrices to free memory
            clearRegionalMatrices();

        } finally {
            // Force garbage collection
            System.gc();
        }
    }

    /**
     * Clear regional matrices more aggressively to help with memory management
     */
    private void clearRegionalMatrices() {
        if (!travelTimesFromRegion.isEmpty()) {
            travelTimesFromRegion.clear();
        }
        if (!travelTimesToRegion.isEmpty()) {
            travelTimesToRegion.clear();
        }
    }

    /**
     * Reads a skim matrix from an csv.gz file and stores it for the given mode and year. To allow conversion between units
     * use the factor to multiply all values.
     * @param mode the mode for which the travel times are read
     * @param file the path to the file
     * @param factor a scalar factor which every entry is multiplied with
     */
    public final void readSkimFromCsvGz(final String mode, final String file, final double factor,Collection<? extends Id> zoneLookup) {
        logger.info("Reading " + mode + " skim");
        IndexedDoubleMatrix2D skim = new CsvGzSkimMatrixReader().readAndConvertToDoubleMatrix2D(file, factor, zoneLookup);
        matricesByMode.put(mode, skim);
        clearRegionalMatrices();
    }

    // called from within SILO!
    public void updateRegionalTravelTimes(Collection<Region> regions, Collection<Zone> zones) {
        logger.info("Updating minimal zone to region travel times...");
        IndexedDoubleMatrix2D travelTimesFromRegionCar = new IndexedDoubleMatrix2D(regions, zones);
        IndexedDoubleMatrix2D travelTimesToRegionCar = new IndexedDoubleMatrix2D(zones, regions);
        IndexedDoubleMatrix2D travelTimesFromRegionPt = new IndexedDoubleMatrix2D(regions, zones);
        IndexedDoubleMatrix2D travelTimesToRegionPt = new IndexedDoubleMatrix2D(zones, regions);

        // Cache matrix references to avoid repeated ConcurrentHashMap lookups in hot loops
        final IndexedDoubleMatrix2D carMatrix = matricesByMode.get(TransportMode.car);
        final IndexedDoubleMatrix2D ptMatrix = matricesByMode.get(TransportMode.pt);

        regions.parallelStream().forEach( r -> {
            for(Zone zone: zones) {
                int zoneId = zone.getZoneId();
                double minFromCar = Double.MAX_VALUE;
                double minToCar = Double.MAX_VALUE;
                double minFromPt = Double.MAX_VALUE;
                double minToPt = Double.MAX_VALUE;

                for (Zone zoneInRegion : r.getZones()) {
                    int regionZoneId = zoneInRegion.getZoneId();

                    // Use cached matrix references to avoid repeated map lookups
                    double travelTimeFromRegionCar = carMatrix.getIndexed(regionZoneId, zoneId);
                    if (travelTimeFromRegionCar < minFromCar) {
                        minFromCar = travelTimeFromRegionCar;
                    }
                    double travelTimeToRegionCar = carMatrix.getIndexed(zoneId, regionZoneId);
                    if (travelTimeToRegionCar < minToCar) {
                        minToCar = travelTimeToRegionCar;
                    }
                    double travelTimeFromRegionPt = ptMatrix.getIndexed(regionZoneId, zoneId);
                    if (travelTimeFromRegionPt < minFromPt) {
                        minFromPt = travelTimeFromRegionPt;
                    }
                    double travelTimeToRegionPt = ptMatrix.getIndexed(zoneId, regionZoneId);
                    if (travelTimeToRegionPt < minToPt) {
                        minToPt = travelTimeToRegionPt;
                    }
                }
                travelTimesFromRegionCar.setIndexed(r.getId(), zoneId, minFromCar);
                travelTimesToRegionCar.setIndexed(zoneId, r.getId(), minToCar);
                travelTimesFromRegionPt.setIndexed(r.getId(), zoneId, minFromPt);
                travelTimesToRegionPt.setIndexed(zoneId, r.getId(), minToPt);
            }
        });
        travelTimesFromRegion.put(TransportMode.car, travelTimesFromRegionCar);
        travelTimesFromRegion.put(TransportMode.pt, travelTimesFromRegionPt);
        travelTimesToRegion.put(TransportMode.car, travelTimesToRegionCar);
        travelTimesToRegion.put(TransportMode.pt, travelTimesToRegionPt);
    }


    /**
     * Updates a skim matrix from an external source
     * @param mode the mode for which the travel times are read
     * @param skim the skim matrix with travel times in minutes
     */
    public void updateSkimMatrix(IndexedDoubleMatrix2D skim, String mode){
        matricesByMode.put(mode, skim);
        logger.warn("The skim matrix for mode " + mode + " has been updated");
        travelTimesFromRegion.remove(mode);
        travelTimesToRegion.remove(mode);
    }

    private double getMinimumPtTravelTime(int origin, int destination, double timeOfDay_s) {
        // Cache matrix lookups to avoid repeated ConcurrentHashMap access
        final IndexedDoubleMatrix2D busMatrix = matricesByMode.get("bus");
        final IndexedDoubleMatrix2D tramMetroMatrix = matricesByMode.get("tramMetro");
        final IndexedDoubleMatrix2D trainMatrix = matricesByMode.get("train");

        double travelTime = Double.MAX_VALUE;

        double busTime = busMatrix.getIndexed(origin, destination);
        if (busTime < travelTime) {
            travelTime = busTime;
        }

        double tramMetroTime = tramMetroMatrix.getIndexed(origin, destination);
        if (tramMetroTime < travelTime) {
            travelTime = tramMetroTime;
        }

        double trainTime = trainMatrix.getIndexed(origin, destination);
        if (trainTime < travelTime) {
            travelTime = trainTime;
        }

        return travelTime;
    }

    public void printOutCarSkim(String mode, String filePath, String matrixName) {
        OmxMatrixWriter.createOmxSkimMatrix(matricesByMode.get(mode),
                filePath,
                matrixName);
    }

	@Override
	public double getTravelTime(Location origin, Location destination, double timeOfDay_s, String mode) {
		int originZone = origin.getZoneId();
		int destinationZone = destination.getZoneId();
	
		// Currently, the time of day is not used here, but it could. E.g. if there are multiple matrices for
		// different "time-of-day slices" the argument could be used to select the correct matrix, nk/dz, jan'18
		if (mode.equals("pt")) {
			if (matricesByMode.containsKey("pt")) {
				return matricesByMode.get(mode).getIndexed(originZone, destinationZone);
			} else if (matricesByMode.containsKey("bus") && matricesByMode.containsKey("tramMetro") && matricesByMode.containsKey("train")){
				return getMinimumPtTravelTime(originZone, destinationZone, timeOfDay_s);
			} else {
				throw new RuntimeException("define transit travel modes!!");
			}
		} else {
            IndexedDoubleMatrix2D matrix = matricesByMode.get(mode);
            if (matrix == null) {
                logger.warn("Travel time for mode {} not found. Available modes: {}", mode, matricesByMode.keySet());
            }
            assert matrix != null;
            return matrix.getIndexed(originZone, destinationZone);
		}
	}
	
	@Override
	public double getTravelTimeFromRegion(Region origin, Zone destination, double timeOfDay_s, String mode) {
        if(!travelTimesFromRegion.containsKey(mode)) {
            throw new RuntimeException("Travel time to regions not initialized. " +
                    "Make sure to call updateZoneToRegionTravelTimes() first");
        }
        return travelTimesFromRegion.get(mode).getIndexed(origin.getId(), destination.getId());
	}

    @Override
    public double getTravelTimeToRegion(Zone origin, Region destination, double timeOfDay_s, String mode) {
        if(!travelTimesToRegion.containsKey(mode)) {
            throw new RuntimeException("Travel time to regions not initialized. " +
                    "Make sure to call updateZoneToRegionTravelTimes() first");
        }
        return travelTimesToRegion.get(mode).getIndexed(origin.getId(), destination.getId());
    }

    @Override
    public IndexedDoubleMatrix2D getPeakSkim(String mode) {
        return matricesByMode.get(mode);
    }

    @Override
    public TravelTimes duplicate() {
        SkimTravelTimes travelTimes = new SkimTravelTimes();
        for(Map.Entry<String, IndexedDoubleMatrix2D> skims: this.matricesByMode.entrySet()) {
            travelTimes.matricesByMode.put(skims.getKey(), skims.getValue().copy());
        }
        for(Map.Entry<String, IndexedDoubleMatrix2D> entry: travelTimesFromRegion.entrySet()) {
            travelTimes.travelTimesFromRegion.put(entry.getKey(), entry.getValue().copy());
        }
        for(Map.Entry<String, IndexedDoubleMatrix2D> entry: travelTimesToRegion.entrySet()) {
            travelTimes.travelTimesToRegion.put(entry.getKey(), entry.getValue().copy());
        }
        return travelTimes;
    }

    //TODO: used in silo. should probably return a deep copy to prevent illegal changes.
	public IndexedDoubleMatrix2D getMatrixForMode(String mode) {
			return matricesByMode.get(mode);
	}
}

