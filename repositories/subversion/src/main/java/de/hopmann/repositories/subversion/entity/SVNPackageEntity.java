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
package de.hopmann.repositories.subversion.entity;

import javax.persistence.Entity;

import de.hopmann.msc.commons.model.Version;
import de.hopmann.repositories.commons.entity.PackageEntity;

@Entity
public class SVNPackageEntity extends PackageEntity {
	private static final long serialVersionUID = 1L;

	public SVNPackageEntity() {

	}

	public SVNPackageEntity(String sourceLocation, Version sourceVersion) {
		setSourceLocation(sourceLocation);
		setSourceVersion(sourceVersion);
	}

}