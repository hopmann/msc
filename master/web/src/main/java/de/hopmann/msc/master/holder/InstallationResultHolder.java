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
import java.util.ArrayList;
import java.util.List;

import de.hopmann.msc.master.ejb.entity.PackageResult;
import de.hopmann.msc.master.ejb.entity.PackageResult.PackageResultType;
import de.hopmann.msc.master.ejb.service.PackageService;

public class InstallationResultHolder implements Serializable {

	private static final long serialVersionUID = 1L;
	private PackageResult packageResult;
	private long minRevision;
	private PackageService packageService;

	public InstallationResultHolder(PackageResult packageResult,
			long minRevision, PackageService packageService) {
		this.packageResult = packageResult;
		this.minRevision = minRevision;
		this.packageService = packageService;
	}

	public PackageResult getInstallationResult() {
		return packageResult;
	}

	public PackageResultType getInstallationType() {
		return packageResult.getInstallationType();
	}

	public List<InstallationRangeHolder> getDependencies() {

		List<InstallationRangeHolder> results = new ArrayList<>();

		long maxRevision = packageResult.getRevision();

		for (PackageResult depInstallation : packageService
				.getDependencies(packageResult)) {

			InstallationRangeHolder rangeHolder = new InstallationRangeHolder(
					depInstallation.getPackageSource(), minRevision,
					maxRevision, packageService);
 
			results.add(rangeHolder);
		}

		return results;
	}

}
