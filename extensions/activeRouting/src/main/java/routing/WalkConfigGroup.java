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
public final class WalkConfigGroup extends ActiveConfigGroup {
	// necessary to have this public

	public static final String GROUP_NAME = "walk";
	private static final String WALK_MODE = "walk";

	private static final double DEFAULT_MAX_WALK_SPEED = 5./3.;


	public WalkConfigGroup() {
		super(GROUP_NAME);
	}

	public String getMode() {
		return WALK_MODE;
	}

	public double getDefaultMaxWalkSpeed() {
		return DEFAULT_MAX_WALK_SPEED;
	}
}
