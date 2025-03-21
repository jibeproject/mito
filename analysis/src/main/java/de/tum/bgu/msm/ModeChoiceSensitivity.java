package de.tum.bgu.msm;

import de.tum.bgu.msm.data.*;
import de.tum.bgu.msm.data.travelTimes.TravelTimes;
import de.tum.bgu.msm.io.input.readers.CalibrationDataReader;
import de.tum.bgu.msm.io.input.readers.CalibrationRegionMapReader;
import de.tum.bgu.msm.modules.ModeChoiceCalculator2017Impl;
import de.tum.bgu.msm.modules.modeChoice.DefaultModeChoiceCalibrationData;
import de.tum.bgu.msm.modules.modeChoice.ModeChoiceCalculator;
import de.tum.bgu.msm.modules.modeChoice.CalibratingModeChoiceCalculatorImpl;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;


import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EnumMap;

public class ModeChoiceSensitivity {


    private ModeChoiceCalculator calculator;

    private PrintWriter pw;
    private final double speedCar = 52;
    private final double speedTrain = 15.;
    private final double speedBus = 19.;
    private final double speedTramMetro = 13.;
    private final double detourNMT = 0.8;
    private final int[] incomes = new int[]{2000, 10000};


    public static void main(String[] args) throws FileNotFoundException {

        ModeChoiceSensitivity modeChoiceSensitivity = new ModeChoiceSensitivity();
        modeChoiceSensitivity.setup();
        modeChoiceSensitivity.run();

    }


    public void setup() throws FileNotFoundException {
        Resources.initializeResources("./test/muc/test.properties");

        pw = new PrintWriter("modeChoiceSensitivity_v3.csv");
        pw.print("purpose,income,distance,factorCar,factorPt,factorCarPrice,factorPtPrice");
        for (Mode mode : Mode.values()) {
            pw.print(",");
            pw.print(mode.toString());
        }
        pw.println();
    }


    public void run() {
        MitoZone zone = DummyZone.dummy;
        zone.setDistanceToNearestRailStop(0.5f);
        //origin.setAreaTypeHBWModeChoice(AreaType.HBW_mediumSizedCity);

        DataSet dataSet = new DataSetImpl();
        dataSet.setModeChoiceCalibrationData(new DefaultModeChoiceCalibrationData());
        new CalibrationDataReader(dataSet).read();
        new CalibrationRegionMapReader(dataSet).read();

        int counter = 0;
        for (int income : incomes) {

            MitoHousehold hh = new MitoHousehold(1, income, 1, Boolean.TRUE);
            MitoPerson pp = new MitoPersonImpl(1, hh, MitoOccupationStatus.STUDENT, DummyOccupation.dummy, 20, MitoGender.FEMALE, true);
            hh.addPerson(pp);
            pp.setHasBicycle(true);
            MitoTrip trip = new MitoTripImpl(1, Purpose.HBS);
            trip.setTripOrigin(zone);
            trip.setTripDestination(zone);

            for (double carPriceFactor = 0.; carPriceFactor <= 5.; carPriceFactor += 1.) {
                for (double ptPriceFactor = 0.; ptPriceFactor <= 5.; ptPriceFactor += 1.) {
                    for (Purpose purpose : Purpose.getAllPurposes()) {
                        calculator = new ModeChoiceCalculatorWithPriceFactors(new ModeChoiceCalculator2017Impl(purpose, null), carPriceFactor, ptPriceFactor, purpose, null);
                        calculator = new CalibratingModeChoiceCalculatorImpl(calculator, dataSet.getModeChoiceCalibrationData());
                        for (double distance_km = 0.; distance_km < 150.; distance_km += 5.) {
                            double thisDistance_km = distance_km;
                            for (double factorPt = 0.2; factorPt <= 3.; factorPt += 0.2) {
                                for (double factorCar = 0.2; factorCar <= 3.; factorCar += 0.2) {

                                    double thisSpeedCar = speedCar;
                                    double thisSpeedBus = speedBus;
                                    double thisSpeedTramMetro = speedTramMetro;
                                    double thisSpeedTrain = speedTrain;

                                    double thisFactorCar = factorCar;
                                    double thisFactorPt = factorPt;

                                    final TravelTimes travelTimes = new TravelTimes() {
                                        @Override
                                        public double getTravelTime(Location origin, Location destination, double timeOfDay_s, String mode) {
                                            switch (mode) {
                                                case "car":
                                                    return thisDistance_km / thisSpeedCar * 60 * thisFactorCar;
                                                case "bus":
                                                    return thisDistance_km / thisSpeedBus * 60 * thisFactorPt;
                                                case "tramMetro":
                                                    return thisDistance_km / thisSpeedTramMetro * 60 * thisFactorPt;
                                                case "train":
                                                    return thisDistance_km / thisSpeedTrain * 60 * thisFactorPt;
                                                default:
                                                    throw new RuntimeException();
                                            }
                                        }

                                        @Override
                                        public double getTravelTimeFromRegion(Region origin, Zone destination, double timeOfDay_s, String mode) {
                                            return 0;
                                        }

                                        @Override
                                        public double getTravelTimeToRegion(Zone origin, Region destination, double timeOfDay_s, String mode) {
                                            return 0;
                                        }


                                        @Override
                                        public IndexedDoubleMatrix2D getPeakSkim(String mode) {
                                            return null;
                                        }

                                        @Override
                                        public TravelTimes duplicate() {
                                            return null;
                                        }
                                    };
                                    EnumMap<Mode, Double> result = calculator.calculateProbabilities(purpose, hh, pp, zone, zone, travelTimes, thisDistance_km, thisDistance_km * detourNMT, 0);
                                    counter++;
                                    pw.print(purpose.toString() + "," + income + "," + distance_km + "," + factorCar + "," + factorPt +
                                            "," + carPriceFactor + "," + ptPriceFactor);
                                    for (Mode mode : Mode.values()) {
                                        pw.print(",");
                                        pw.print(result.get(mode));
                                    }
                                    pw.println();

                                }
                            }


                        }
                        System.out.println("Calculations: " + counter);

                    }

                }
            }

        }
        pw.close();

    }
}
