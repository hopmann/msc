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
package de.hopmann.repositories.commons.entity;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.model.DependencyType;

@Entity
public class DependencyEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue
	private Integer id;

//	@ManyToOne(fetch = FetchType.LAZY)
//	private T owningPackage;
	private String dependingPackageName;

	@Embedded
	private VersionConstraintEntity versionConstraint;

	private DependencyType type;

	private static Pattern dependencyPattern = Pattern
			.compile("([^(]+)(?:\\(([^)]+)\\))?");

	public DependencyEntity() {

	}

	public static DependencyEntity valueOf(String value)
			throws VersionFormatException {
		if (value == null) {
			return null;
		}

		Matcher matcher = dependencyPattern.matcher(value);
		if (matcher.find()) {

			String pkgName = matcher.group(1).trim();
			VersionConstraintEntity constraint = VersionConstraintEntity
					.valueOf(matcher.group(2));
			// TODO maybe check validity

			DependencyEntity dependencyEntity = new DependencyEntity();

			dependencyEntity.setDependingPackage(pkgName);
			dependencyEntity.setVersionConstraint(constraint);

			return dependencyEntity;
		} else {
			return null;
		}
	}

	public Integer getId() {
		return id;
	}

	public String getDependingPackage() {
		return dependingPackageName;
	}

	public void setDependingPackage(String dependingPackageName) {
		this.dependingPackageName = dependingPackageName;
	}

//	public PackageEntity getOwningPackage() {
//		return owningPackage;
//	}
//
//	public void setOwningPackage(T packageEntity) {
//		packageEntity.addDependency(this);
//	}
//
//	void setOwningPackageInternal(T packageEntity) {
//		owningPackage = packageEntity;
//	}

	public void setVersionConstraint(VersionConstraintEntity versionConstraint) {
		this.versionConstraint = versionConstraint;
	}

	public VersionConstraintEntity getVersionConstraint() {
		return versionConstraint;
	}

	public DependencyType getType() {
		return type;
	}

	public void setType(DependencyType type) {
		this.type = type;
	}

}
