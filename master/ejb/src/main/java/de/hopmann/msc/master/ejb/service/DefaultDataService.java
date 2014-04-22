/**
 * Copyright (C) 2014 Holger Hopmann (h.hopmann@uni-muenster.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.hopmann.msc.master.ejb.service;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import de.hopmann.msc.master.ejb.entity.PackageContext;

/**
 * Loads default datasets on application deployment.
 * 
 */
@Startup
@Singleton
public class DefaultDataService {

	@Inject
	private PackageService packageService;

	@PostConstruct
	private void init() {
		String testContextName = "Test Context R 3.0.2";

		PackageContext testContext = packageService
				.getPackageRepositoryByName(testContextName);

		if (testContext == null) {
			packageService.addPackageContext(testContextName);
		}
	}
}
