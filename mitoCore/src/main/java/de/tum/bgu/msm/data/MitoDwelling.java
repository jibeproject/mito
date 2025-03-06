package de.tum.bgu.msm.data;

import org.locationtech.jts.geom.Coordinate;

public class MitoDwelling implements MicroLocation {

    protected MitoZone dwellingZone;

    protected Coordinate dwellingLocation;

    public MitoDwelling(MitoZone zone, Coordinate coordinate) {
        this.dwellingZone = zone;
        this.dwellingLocation = coordinate;
    }

    public MitoDwelling() {
    }

    @Override
    public int getZoneId() {
        return dwellingZone.getId();
    }

    @Override
    public Coordinate getCoordinate() {
        return dwellingLocation;
    }
}
