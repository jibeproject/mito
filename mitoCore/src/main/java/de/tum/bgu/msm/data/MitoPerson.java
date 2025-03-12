package de.tum.bgu.msm.data;

import java.util.*;

public interface MitoPerson extends Id {
    MitoOccupation getOccupation();

    MitoOccupationStatus getMitoOccupationStatus();

    @Override
    int getId();

    int getAge();

    MitoGender getMitoGender();

    boolean hasDriversLicense();

    void setErrorTerms(EnumMap<Mode, Double> errorTerms);

    Map<Mode,Double> getErrorTerms();

    Set<MitoTrip> getTrips();

    void addTrip(MitoTrip trip);

    void removeTripFromPerson(MitoTrip trip);

    @Override
    int hashCode();

    Optional<Boolean> getHasBicycle();

    void setHasBicycle(boolean hasBicycle);

    MitoHousehold getHousehold();

    List<MitoTrip> getTripsForPurpose(Purpose purpose);

    boolean hasTripsForPurpose(Purpose purpose);
}
