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
package de.hopmann.msc.commons.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import de.hopmann.msc.commons.model.PackageLocation;
import de.hopmann.msc.commons.model.PackageRepository;
import de.hopmann.msc.commons.model.PackageSource;
import de.hopmann.msc.commons.model.SourceIdentifier;
import de.hopmann.msc.commons.model.SourceIdentifier.SourceRepositoryIdentifier;
import de.hopmann.msc.commons.qualifier.CRAN;

@Named
@ApplicationScoped
public class RepositoryService implements IRepositoryService {

	private PackageRepository cranRepository;

	private Map<String, PackageSource> repositoryTypeMap = new HashMap<>();

	RepositoryService() {

	}

	@Inject
	RepositoryService(@Any Instance<PackageSource> repositoryInstance,
			@CRAN PackageRepository cranRepository) {
		for (PackageSource repository : repositoryInstance) {
			repositoryTypeMap.put(repository.getSourceType(), repository);
		}

		this.cranRepository = cranRepository;
	}

	@Override
	public PackageSource getPackageSource(String sourceType) {
		if (sourceType == null) {
			throw new IllegalArgumentException("Repository type not set");
		}

		PackageSource source = repositoryTypeMap.get(sourceType);

		if (source == null) {
			// TOOD better exception
			throw new RuntimeException("Repository " + sourceType
					+ " not found");
		}

		return source;
	}

	@Override
	public Collection<PackageSource> getPackageSources() {
		return repositoryTypeMap.values();
	}

	@Override
	public PackageRepository getCranRepository() {
		return cranRepository;
	}

	@Override
	public PackageLocation getPackageLocation(
			SourceIdentifier locationIdentifier) {
		if (locationIdentifier == null)
			return null;
		return (PackageLocation) getPackageSource(locationIdentifier
				.getSourceType());
	}

	@Override
	public PackageLocation getPackageRepository(
			SourceRepositoryIdentifier repositoryIdentifier) {
		if (repositoryIdentifier == null)
			return null;
		return (PackageLocation) getPackageSource(repositoryIdentifier
				.getSourceType());
	}
}
