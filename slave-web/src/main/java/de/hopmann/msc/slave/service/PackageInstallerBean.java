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
package de.hopmann.msc.slave.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.messages.PackageInstallerMessage;
import de.hopmann.msc.commons.util.VersionHelper;
import de.hopmann.msc.slave.installer.PackageInstaller;
import de.hopmann.msc.slave.installer.PackageInstallerHolder;

/**
 * Provides access to available R environments
 * 
 */
@ApplicationScoped
public class PackageInstallerBean {

	private List<PackageInstallerHolder> packageInstallerCache = new ArrayList<>();

	PackageInstallerBean() {

	}

	@Inject
	PackageInstallerBean(
			@Any Instance<PackageInstaller> packageInstallerInstance, Logger log) {

		for (PackageInstaller packageInstaller : packageInstallerInstance) {
			try {
				packageInstallerCache.add(new PackageInstallerHolder(
						packageInstaller));
			} catch (Exception e) {
				log.log(Level.WARNING, "Could not load PackageInstaller "
						+ packageInstaller.getClass().getSimpleName(), e);
			}
		}
	}

	public List<PackageInstallerHolder> getAvailableInstaller() {
		return Collections.unmodifiableList(packageInstallerCache);
	}

	public PackageInstallerHolder getPackageInstaller(String versionString,
			String flavor, String architecture) throws VersionFormatException {

		BigInteger versionNumber = VersionHelper
				.getVersionNumber(versionString);

		for (PackageInstallerHolder packageInstaller : packageInstallerCache) {
			boolean matches = true;

			matches &= (packageInstaller.getVersion().equals(versionNumber));
			if (flavor != null) {
				matches &= packageInstaller.getFlavor().equals(flavor);
			}
			if (architecture != null) {
				matches &= packageInstaller.getArchitecture().equals(flavor);
			}

			if (matches) {
				return packageInstaller;
			}
		}

		throw new RuntimeException("No matching PackageInstaller found");

		// TODO error handling
	}

	public PackageInstallerHolder getPackageInstaller(
			PackageInstallerMessage packageInstallerDescription)
			throws VersionFormatException {
		if (packageInstallerDescription == null)
			throw new IllegalArgumentException();
		return getPackageInstaller(
				packageInstallerDescription.getVersionString(),
				packageInstallerDescription.getFlavor(),
				packageInstallerDescription.getArchitecture());
	}

}
