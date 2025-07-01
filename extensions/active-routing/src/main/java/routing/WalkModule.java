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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import routing.travelDisutility.WalkTravelDisutilityFactory;
import routing.travelTime.WalkLinkSpeedCalculator;
import routing.travelTime.WalkLinkSpeedCalculatorImpl;
import routing.travelTime.WalkTravelTime;


public class WalkModule extends AbstractModule {

	private static final Logger LOG = LogManager.getLogger(WalkModule.class);

	@Inject
	private WalkConfigGroup walkConfigGroup;

	public WalkModule() {
	}

	@Override
	public void install() {
		this.addTravelTimeBinding(walkConfigGroup.getMode()).to(WalkTravelTime.class).in(Singleton.class);
		this.addTravelDisutilityFactoryBinding(walkConfigGroup.getMode()).to(WalkTravelDisutilityFactory.class).in(Singleton.class);
		this.installOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.addLinkSpeedCalculator().to(WalkLinkSpeedCalculator.class);
			}
		});
		bind(WalkLinkSpeedCalculator.class).to(WalkLinkSpeedCalculatorImpl.class);
	}
}
