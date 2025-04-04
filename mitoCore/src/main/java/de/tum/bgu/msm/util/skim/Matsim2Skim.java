package de.tum.bgu.msm.util.skim;

import com.google.common.collect.Iterables;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.trafficAssignment.CarSkimUpdater;
import de.tum.bgu.msm.util.MitoUtil;
import de.tum.bgu.msm.util.concurrent.ConcurrentExecutor;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Matsim2Skim {

    private final static Logger logger = LogManager.getLogger(CarSkimUpdater.class);
    private final Network network;
    private final TravelDisutility travelDisutility;
    private final TravelTime travelTime;

    public Matsim2Skim(Network network, TravelDisutility travelDisutility, TravelTime travelTime) {
        this.network = network;
        this.travelDisutility = travelDisutility;
        this.travelTime = travelTime;
    }

    public void calculateMatrixFromMatsim(int numberOfCalcPoints,
                                          IndexedDoubleMatrix2D carTravelTimeMatrix,
                                          IndexedDoubleMatrix2D carDistanceMatrix,
                                          double time,
                                          Collection<MitoZone> mitoZones) {
        final Map<Integer, List<Node>> nodesByZone = new ConcurrentHashMap<>();

        mitoZones.stream().parallel().forEach(mitoZone -> {
            nodesByZone.put(mitoZone.getId(), new LinkedList<>());
            for (int i = 0; i < numberOfCalcPoints; i++) { // Several points in a given origin zone
                Coord originCoord = CoordUtils.createCoord(mitoZone.getRandomCoord(MitoUtil.getRandomObject()));
                Node originNode = NetworkUtils.getNearestLink(network, originCoord).getToNode();
                nodesByZone.get(mitoZone.getId()).add(originNode);
            }
        });
        logger.info("Assigned nodes to " + nodesByZone.keySet().size() + " zones");


        long startTime2 = System.currentTimeMillis();

        final int partitionSize = (int) ((double) mitoZones.size() / Runtime.getRuntime().availableProcessors()) + 1;
        logger.info("Intended size of all of partititons = " + partitionSize);
        Iterable<List<MitoZone>> partitions = Iterables.partition(mitoZones, partitionSize);
        ConcurrentExecutor<Void> executor = ConcurrentExecutor.fixedPoolService(Runtime.getRuntime().availableProcessors());

        for (final List<MitoZone> partition : partitions) {
            logger.info("Size of partititon = " + partition.size());

            executor.addTaskToQueue(() -> {
                try {
                    MultiNodePathCalculator calculator
                            = (MultiNodePathCalculator) new FastMultiNodeDijkstraFactory(true).createPathCalculator(network, travelDisutility, travelTime);

                    Set<InitialNode> toNodes = new HashSet<>();
                    for (MitoZone zone : mitoZones) {
                        // Several points in a given origin zone
                        for (int i = 0; i < numberOfCalcPoints; i++) {
                            Node originNode = nodesByZone.get(zone.getId()).get(i);
                            toNodes.add(new InitialNode(originNode, 0., 0.));
                        }
                    }

                    ImaginaryNode aggregatedToNodes = MultiNodeDijkstra.createImaginaryNode(toNodes);

                    for (MitoZone origin : partition) {
                        Node node = nodesByZone.get(origin.getId()).get(0);
                        calculator.calcLeastCostPath(node, aggregatedToNodes, time, null, null);
                        for (MitoZone destination : mitoZones) {

                            double meanTravelTime = 0;
                            double meanDistance = 0;
                            List<Node> nodes = nodesByZone.get(destination.getId());
                            for (Node n : nodes) {
                                LeastCostPathCalculator.Path path = calculator.constructPath(node, n, time);

                                //convert to minutes
                                double travelTime = path.travelTime / 60.;
                                double distance = 0.;
                                for (Link link : path.links) {
                                    distance += link.getLength();
                                }
                                meanTravelTime += travelTime;
                                meanDistance += distance;
                            }

                            meanTravelTime /= nodes.size();
                            meanDistance /= nodes.size();

                            carTravelTimeMatrix.setIndexed(origin.getId(), destination.getId(), meanTravelTime);
                            carDistanceMatrix.setIndexed(origin.getId(), destination.getId(), meanDistance / 1000.);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                logger.warn("Finished thread.");
                return null;
            });
        }
        executor.execute();

        long runtime2 = (System.currentTimeMillis() - startTime2) / 1000;
        logger.info("Completed car matrix update in " + runtime2 + " seconds(dvrp methods)");


        assignIntrazonals(carTravelTimeMatrix, carDistanceMatrix,5, 10, 0.33f);
    }

    private void assignIntrazonals(IndexedDoubleMatrix2D travelTimeMatrix, IndexedDoubleMatrix2D distanceMatrix,
                                   int numberOfNeighbours, float maximumMinutes, float proportionOfTime) {
        int nonIntrazonalCounter = 0;
        for (int i = 1; i < travelTimeMatrix.columns(); i++) {
            int i_id = travelTimeMatrix.getIdForInternalColumnIndex(i);
            double[] minTimeValues = new double[numberOfNeighbours];
            double[] minDistValues = new double[numberOfNeighbours];
            for (int k = 0; k < numberOfNeighbours; k++) {
                minTimeValues[k] = maximumMinutes;
                minDistValues[k] = maximumMinutes / 60 * 50; //maximum distance results from maximum time at 50 km/h
            }
            //find the  n closest neighbors - the lower travel time values in the matrix column
            for (int j = 1; j < travelTimeMatrix.rows(); j++) {
                int j_id = travelTimeMatrix.getIdForInternalRowIndex(j);
                int minimumPosition = 0;
                while (minimumPosition < numberOfNeighbours) {
                    if (minTimeValues[minimumPosition] > travelTimeMatrix.getIndexed(i_id, j_id) && travelTimeMatrix.getIndexed(i_id, j_id) != 0) {
                        for (int k = numberOfNeighbours - 1; k > minimumPosition; k--) {
                            minTimeValues[k] = minTimeValues[k - 1];
                            minDistValues[k] = minDistValues[k - 1];

                        }
                        minTimeValues[minimumPosition] = travelTimeMatrix.getIndexed(i_id, j_id);
                        minDistValues[minimumPosition] = distanceMatrix.getIndexed(i_id, j_id);

                        break;
                    }
                    minimumPosition++;
                }
            }
            double globalMinTime = 0;
            double globalMinDist = 0;
            for (int k = 0; k < numberOfNeighbours; k++) {
                globalMinTime += minTimeValues[k];
                globalMinDist += minDistValues[k];
            }
            globalMinTime = globalMinTime / numberOfNeighbours * proportionOfTime;
            globalMinDist = globalMinDist / numberOfNeighbours * proportionOfTime;

            //fill with the calculated value the cells with zero
            for (int j = 1; j < travelTimeMatrix.rows(); j++) {
                int j_id = travelTimeMatrix.getIdForInternalColumnIndex(j);
                if (travelTimeMatrix.getIndexed(i_id, j_id) == 0) {
                    travelTimeMatrix.setIndexed(i_id, j_id, globalMinTime);
                    distanceMatrix.setIndexed(i, j, globalMinDist);
                    if (i != j) {
                        nonIntrazonalCounter++;
                    }
                }
            }
        }
        logger.info("Calculated intrazonal times and distances using the " + numberOfNeighbours + " nearest neighbours.");
        logger.info("The calculation of intrazonals has also assigned values for cells with travel time equal to 0, that are not intrazonal: (" +
                nonIntrazonalCounter + " cases).");
    }
}
