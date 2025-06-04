package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.modules.tripGeneration.AttractionCalculator;
import de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.cam.mrc.phm.io.TripAttractionRatesReaderMEL;

import java.util.List;

import static de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable.HH;
import static java.lang.Double.NaN;

public class AttractionCalculatorMEL implements AttractionCalculator {

    private final Purpose purpose;

    private static final Logger logger = LogManager.getLogger(AttractionCalculatorMEL.class);

    private final DataSet dataSet;

    public AttractionCalculatorMEL(DataSet dataSet, Purpose purpose) {
        this.dataSet = dataSet;
        this.purpose = purpose;
        new TripAttractionRatesReaderMEL(dataSet, purpose).read();
    }

    @Override
    public void run() {
        logger.info("  Calculating trip attractions");
        for (MitoZone zone : dataSet.getZones().values()) {
                Double tripAttraction = 0.0;
                for (ExplanatoryVariable variable : ExplanatoryVariable.values()) {
                    double attribute;
                    if(HH.equals(variable)) {
                        attribute = zone.getNumberOfHouseholds();
                        // logger.info("AttractionCalculatorMEL: HH.equals(variable) is true, using number of households: {}", attribute);
                    }else if(zone.getPoiWeightsByType().get(variable.toString())==null) {
                        // logger.info("AttractionCalculatorMEL: No POI weights found for variable {}", variable);
                        continue;
                    }else{
                        attribute = zone.getPoiWeightsByType().get(variable.toString());
                        // logger.info("AttractionCalculatorMEL: Using POI weights for variable {}: {}", variable, attribute);
                    }
                    Double rate = purpose.getTripAttractionForVariable(variable);
                    if(rate == null) {
                        throw new RuntimeException("Purpose " + purpose + " does not have an attraction" +
                                " rate for variable " + variable + " registered.");
                    }
                    tripAttraction += (Double) (attribute * rate);
                    if(Double.isNaN(tripAttraction)) {
                        throw new RuntimeException("Trip attraction for zone " + zone.getId() + " is NaN. " +
                                "Variable: " + variable + ", attribute: " + attribute + ", rate: " + rate);
                    }
                }
                zone.setTripAttraction(purpose, tripAttraction);
//            // Weight for specific buildings  --- USED FOR MANCHESTER, PERHAPS NOT FOR MELBOURNE
//            List<MicroLocation> microDestinations = zone.getMicroDestinations();
//            if(microDestinations.size() > 0) {
//                double[] microDestinationWeights = new double[microDestinations.size()];
//                for(int i = 0 ; i < microDestinations.size() ; i++) {
//                    MicroLocation loc = microDestinations.get(i);
//                    double weight;
//                    ExplanatoryVariable code;
//                    if(loc instanceof MitoPoi) {
//                        weight = ((MitoPoi) loc).getWeight();
//                        code = ((MitoPoi) loc).getCode();
//                    } else if (loc instanceof MitoVacantDwelling || loc instanceof MitoHousehold) {
//                        weight = zone.getVacancyRate();
//                        code = HH;
//                    } else {
//                        throw new RuntimeException("Unrecognised MicroLocation class " + loc.getClass().getName() + " for trip distribution");
//                    }
//                    microDestinationWeights[i] = weight * purpose.getTripAttractionForVariable(code);
//                }
//                zone.setMicroDestinationWeightsByPurpose(purpose, microDestinationWeights);
//            }

        }
    }
}