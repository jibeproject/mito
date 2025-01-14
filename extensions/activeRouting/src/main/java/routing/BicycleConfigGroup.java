/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package routing;

/**
 * @author smetzler, dziemke
 */
public class BicycleConfigGroup extends ActiveConfigGroup {
	// necessary to have this public

	public static final String GROUP_NAME = "bike";
	private static final String BICYCLE_MODE = "bike";

	private static final double DISMOUNT_FACTOR = 0.362;
	private static final double DEFAULT_MAX_BIKE_SPEED = 5.5;


	public BicycleConfigGroup() {
		super(GROUP_NAME);
	}

	public String getMode() {
		return BICYCLE_MODE;
	}

	public double getDismountFactor() {
		return DISMOUNT_FACTOR;
	}

	public double getDefaultMaxBikeSpeed() {
		return DEFAULT_MAX_BIKE_SPEED;
	}
}
