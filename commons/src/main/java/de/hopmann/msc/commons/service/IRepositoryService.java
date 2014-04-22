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

import de.hopmann.msc.commons.model.PackageLocation;
import de.hopmann.msc.commons.model.PackageRepository;
import de.hopmann.msc.commons.model.PackageSource;
import de.hopmann.msc.commons.model.SourceIdentifier;
import de.hopmann.msc.commons.model.SourceIdentifier.SourceRepositoryIdentifier;

public interface IRepositoryService {

	/**
	 * Return loaded {@link PackageRepository} instance by type string.
	 * 
	 * @param repositoryType
	 * @return
	 */
	public abstract PackageSource getPackageSource(String repositoryType);

	public abstract PackageLocation getPackageLocation(
			SourceIdentifier locationIdentifier);

	public abstract PackageLocation getPackageRepository(
			SourceRepositoryIdentifier repositoryIdentifier);

	public abstract Collection<PackageSource> getPackageSources();

	public abstract PackageRepository getCranRepository();

}