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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculator;
import org.matsim.contrib.bicycle.BicycleTravelTime;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import routing.travelDisutility.BicycleTravelDisutilityFactory;
import routing.travelTime.BicycleLinkSpeedCalculatorImpl;


public class BicycleModule extends AbstractModule {

	@Inject
	private BicycleConfigGroup bicycleConfigGroup;

	public BicycleModule() {
	}

	@Override
	public void install() {
		addTravelTimeBinding(bicycleConfigGroup.getMode()).to(BicycleTravelTime.class).in(Singleton.class);
		addTravelDisutilityFactoryBinding(bicycleConfigGroup.getMode()).to(BicycleTravelDisutilityFactory.class).in(Singleton.class);
		this.installOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.addLinkSpeedCalculator().to(BicycleLinkSpeedCalculator.class);
			}

		});
		bind( BicycleLinkSpeedCalculator.class ).to( BicycleLinkSpeedCalculatorImpl.class ) ;
	}
}
