package uk.cam.mrc.phm.calculators;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.io.input.readers.ModeChoiceCoefficientReader;
import de.tum.bgu.msm.modules.modeChoice.AbstractModeChoiceCalculator;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static de.tum.bgu.msm.data.Mode.*;
import static de.tum.bgu.msm.data.Purpose.RRT;

public class ModeChoiceCalculatorRrtMCR extends AbstractModeChoiceCalculator {

    private final static Logger logger = LogManager.getLogger(ModeChoiceCalculatorRrtMCR.class);

    public ModeChoiceCalculatorRrtMCR(DataSet dataSet) {
        super();
        coef = new ModeChoiceCoefficientReader(dataSet, RRT, Resources.instance.getModeChoiceCoefficients(RRT)).readCoefficientsForThisPurpose();
    }

    @Override
    public EnumMap<Mode, Double> calculateUtilities(Purpose purpose, MitoHousehold household, MitoPerson person, MitoZone originZone, MitoZone destinationZone, TravelTimes travelTimes, double travelDistanceAuto, double travelDistanceNMT, double peakHour_s) {

        EnumMap<Mode, Double> utilities = new EnumMap<>(Mode.class);

        Set<Mode> availableChoices;
        if(Resources.instance.getBoolean(Properties.RUN_MODESET,false)){
            availableChoices = ((MitoPerson7days)person).getModeSet().getModesMNL();
        }else {
            availableChoices = coef.keySet();
        }

        assert availableChoices != null;
        availableChoices.removeAll(EnumSet.of(autoDriver, autoPassenger, pt));
        for (Mode mode : availableChoices){
            final Map<String, Double> modeCoef = coef.get(mode);

            // Intercept
            double utility = modeCoef.get("INTERCEPT");

            // Age
            int age = person.getAge();
            if (age <= 24) {
                utility += modeCoef.get("age_5_24");
            } else if (age >= 65) {
                utility += modeCoef.get("age_65");
            }

            // Sex
            if(person.getMitoGender().equals(MitoGender.FEMALE)) {
                utility += modeCoef.get("female");
            }

            // household car
            if(person.getHousehold().getAutos() > 2) {
                utility += modeCoef.get("cars_3");
            }

            // Distance
            utility += Math.sqrt(travelDistanceNMT * 1000.) * modeCoef.get("dist_m_sqrt");

            utilities.put(mode, utility);
        }

        return utilities;
    }

    @Override
    public EnumMap<Mode, Double> calculateGeneralizedCosts(Purpose purpose, MitoHousehold household, MitoPerson person, MitoZone originZone, MitoZone destinationZone, TravelTimes travelTimes, double travelDistanceAuto, double travelDistanceNMT, double peakHour_s) {
        return null;
    }
}
