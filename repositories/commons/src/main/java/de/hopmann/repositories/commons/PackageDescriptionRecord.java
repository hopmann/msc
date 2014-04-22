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
package de.hopmann.repositories.commons;

import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.model.DependencyType;
import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.commons.util.ControlFile.ControlField;
import de.hopmann.msc.commons.util.ControlFile.ControlFields;
import de.hopmann.repositories.commons.entity.DependencyEntity;
import de.hopmann.repositories.commons.entity.PackageEntity;

public abstract class PackageDescriptionRecord<T extends PackageEntity> {
	@ControlField("Package")
	protected String pkgName;
	@ControlField("Version")
	protected String pkgVersionString;
	@ControlFields({ @ControlField("License"), @ControlField("Licence") })
	protected String license;

	@ControlField("Depends")
	protected DependencyEntity[] dependsDependencies = new DependencyEntity[0];
	@ControlField("Suggests")
	protected DependencyEntity[] suggestsDependencies = new DependencyEntity[0];
	@ControlField("Imports")
	protected DependencyEntity[] importsDependencies = new DependencyEntity[0];
	@ControlField("LinkingTo")
	protected DependencyEntity[] linkingToDependencies = new DependencyEntity[0];
	@ControlField("Enhances")
	protected DependencyEntity[] enhancesDependencies = new DependencyEntity[0];

	@ControlField("OS_type")
	protected String osType;

	public PackageDescriptionRecord() {

	}

	protected abstract T createEntity();

	public T buildEntity() throws VersionFormatException {
		T packageEntity = createEntity();

		packageEntity.setName(pkgName);
		packageEntity.setVersion(Version.fromRVersion(pkgVersionString));
		packageEntity.setLicense(license);
		packageEntity.setOsType(osType);

		addDependencyEntities(dependsDependencies, DependencyType.DEPENDS,
				packageEntity);
		addDependencyEntities(enhancesDependencies, DependencyType.ENHANCES,
				packageEntity);
		addDependencyEntities(importsDependencies, DependencyType.IMPORTS,
				packageEntity);
		addDependencyEntities(linkingToDependencies, DependencyType.LINKINGTO,
				packageEntity);
		addDependencyEntities(suggestsDependencies, DependencyType.SUGGESTS,
				packageEntity);

		return packageEntity;
	}

	protected void addDependencyEntities(DependencyEntity[] dependencyEntities,
			DependencyType dependencyType, PackageEntity packageEntity) {

		// TODO correct handling of multiple version constraints, not returning
		// array but set, combining multiple same dependencies

		for (DependencyEntity dependencyEntity : dependencyEntities) {
			if (dependencyType == DependencyType.DEPENDS
					&& dependencyEntity.getDependingPackage().equalsIgnoreCase(
							"R")) {
				// R dependency
				packageEntity.setRVersionConstraint(dependencyEntity
						.getVersionConstraint());
			} else {
				dependencyEntity.setType(dependencyType);
				packageEntity.addDependency(dependencyEntity);
			}
		}
	}

	public String getPackageName() {
		return pkgName;
	}
}
