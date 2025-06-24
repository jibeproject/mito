package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.data.Purpose;
import de.tum.bgu.msm.modules.tripGeneration.AttractionCalculator;
import de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.cam.mrc.phm.io.TripAttractionRatesReaderMEL;

import static de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable.HH;

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
                float tripAttraction = 0;
                for (ExplanatoryVariable variable : ExplanatoryVariable.values()) {
                    double attribute;
                    if(HH.equals(variable)) {
                        attribute = zone.getNumberOfHouseholds();
                    }else if(zone.getPoiWeightsByType().get(variable.toString())==null) {
                        continue;
                    }else{
                        attribute = zone.getPoiWeightsByType().get(variable.toString());
                    }
                    Double rate = purpose.getTripAttractionForVariable(variable);
                    if(rate == null) {
                        throw new RuntimeException("Purpose " + purpose + " does not have an attraction" +
                                " rate for variable " + variable + " registered.");
                    }
                    tripAttraction += (float) (attribute * rate);
                }
                zone.setTripAttraction(purpose, tripAttraction);
        }
    }
}
