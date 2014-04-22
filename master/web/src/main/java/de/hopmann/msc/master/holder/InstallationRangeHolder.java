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
package de.hopmann.msc.master.holder;

import java.io.Serializable;
import java.util.List;

import de.hopmann.msc.master.ejb.entity.PackageResult;
import de.hopmann.msc.master.ejb.entity.PackageSource;
import de.hopmann.msc.master.ejb.service.PackageService;

public class InstallationRangeHolder implements Serializable {

	private static final long serialVersionUID = 1L;
	private long minRevision;
	private PackageSource packageSource;
	private long maxRevision;

	private PackageService packageService;

	public InstallationRangeHolder(PackageSource packageSource,
			long minRevision, long maxRevision, PackageService packageService) {
		this.packageSource = packageSource;
		this.maxRevision = maxRevision;
		this.minRevision = minRevision;

		this.packageService = packageService;
	}

	public List<PackageResult> getPreviousInstallations() {
		return packageService.getPackageResultRange(packageSource,
				minRevision + 1, maxRevision);
	}

	public PackageResult getInstallation() {
		List<PackageResult> installationRange = packageService
				.getPackageResultRange(packageSource, maxRevision, maxRevision);
		if (!installationRange.isEmpty()) {
			return installationRange.get(0);
		} else {
			return null;
		}
	}

	public String getPackageName() {
		return packageSource.getPackageName();
	}

}
