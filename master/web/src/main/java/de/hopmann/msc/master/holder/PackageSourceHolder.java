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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.hopmann.msc.master.ejb.entity.PackageResult;
import de.hopmann.msc.master.ejb.entity.PackageResult.PackageResultType;
import de.hopmann.msc.master.ejb.entity.PackageSource;
import de.hopmann.msc.master.ejb.entity.PackageContext;
import de.hopmann.msc.master.ejb.service.PackageService;

public class PackageSourceHolder implements Serializable {

	private static final long serialVersionUID = 1L;
	private PackageSource packageSource;
	private PackageService packageService;

	public PackageSourceHolder(PackageSource packageSource,
			PackageService packageService) {
		this.packageSource = packageSource;
		this.packageService = packageService;
	}

	public List<InstallationResultHolder> getCheckResults() {
		List<InstallationResultHolder> results = new ArrayList<>();

		List<PackageResult> checkResults = packageService.getPackageResults(
				packageSource, PackageResultType.CHECK);

		Collections.sort(checkResults, new Comparator<PackageResult>() {
			@Override
			public int compare(PackageResult o1, PackageResult o2) {
				return -Long.compare(o1.getRevision(), o2.getRevision());
			}
		});

		for (int i = 0; i < checkResults.size(); i++) {

			long minRevision = 0;
			if (i < checkResults.size() - 1) {
				minRevision = checkResults.get(i + 1).getRevision();
			}

			InstallationResultHolder resultHolder = new InstallationResultHolder(
					checkResults.get(i), minRevision, packageService);
			results.add(resultHolder);
		}

		return results;
	}

	public PackageContext getRepository() {
		return packageSource.getRepository();
	}

	public PackageSource getPackageSource() {
		return packageSource;
	}
}
