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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import de.hopmann.msc.commons.model.Version;

@MappedSuperclass
public abstract class PackageEntity implements Serializable {
	// TODO id version+name?
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	protected Long id;

	protected String name;

	protected String sourceLocation;

	protected String license;

	@Embedded
	protected Version version;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "versionNumber", column = @Column(name = "sourceversionnumber", columnDefinition = "numeric")),
			@AttributeOverride(name = "versionString", column = @Column(name = "sourceversionstring")) })
	protected Version sourceVersion;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "minVersion", column = @Column(name = "minVersionR", columnDefinition = "numeric")),
			@AttributeOverride(name = "maxVersion", column = @Column(name = "maxVersionR", columnDefinition = "numeric")) })
	protected VersionConstraintEntity rVersionConstraint;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	protected List<DependencyEntity> dependencies = new ArrayList<DependencyEntity>(
			0);

	protected String osType;

	public PackageEntity() {

	}

	public PackageEntity(String name) {
		setName(name);
	}

//	public static PackageEntity fromSourceLocation(String sourceLocation,
//			Version sourceVersion) {
//		PackageEntity newInstance = new PackageEntity();
//		newInstance.sourceLocation = sourceLocation;
//		newInstance.sourceVersion = sourceVersion;
//		return newInstance;
//	}
//
//	public static PackageEntity fromSourceRepository(String packageName,
//			Version packageVersion) {
//		PackageEntity newInstance = new PackageEntity();
//		newInstance.name = packageName;
//		newInstance.version = packageVersion;
//		return newInstance;
//	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public List<DependencyEntity> getDependencies() {
		return dependencies;
	}

	public void addDependency(DependencyEntity dependencyEntity) {
		dependencies.add(dependencyEntity);
		//dependencyEntity.setOwningPackageInternal(this);
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public void setRVersionConstraint(VersionConstraintEntity rVersionConstraint) {
		this.rVersionConstraint = rVersionConstraint;
	}

	public VersionConstraintEntity getRVersionConstraint() {
		return rVersionConstraint;
	}

	public void setOsType(String osType) {
		this.osType = osType;
	}

	public String getOsType() {
		return osType;
	}

	public String getSourceLocation() {
		return sourceLocation;
	}

	public Version getSourceVersion() {
		return sourceVersion;
	}

	public void setSourceLocation(String sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public void setSourceVersion(Version sourceVersion) {
		this.sourceVersion = sourceVersion;
	}

}