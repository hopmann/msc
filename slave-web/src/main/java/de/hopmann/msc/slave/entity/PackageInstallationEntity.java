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
package de.hopmann.msc.slave.entity;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import de.hopmann.msc.commons.messages.CMDOutputMessage;
import de.hopmann.msc.commons.model.Version;

@Entity
public class PackageInstallationEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	private String packageName;

	@Embedded
	private Version packageVersion;

	private String sourceType;

	private String sourceLocation;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "versionNumber", column = @Column(name = "sourceversionnumber", columnDefinition = "numeric")),
			@AttributeOverride(name = "versionString", column = @Column(name = "sourceversionstring")) })
	private Version sourceVersion;

	private Boolean isFailed;

	@Embedded
	private PackageInstallerEntity packageInstaller;

	@ManyToMany(fetch = FetchType.LAZY)
	private Set<PackageInstallationEntity> actualDependencies = new HashSet<PackageInstallationEntity>(
			0);

	@Transient
	private Path libraryPath;

	@Transient
	private List<CMDOutputMessage> cMDOutputMessages;

	public PackageInstallationEntity() {

	}

	public void addDependencies(Set<PackageInstallationEntity> dependencies) {
		this.actualDependencies.addAll(dependencies);
	}

	public void addDependency(PackageInstallationEntity installationEntity) {
		actualDependencies.add(installationEntity);
	}

	public Set<PackageInstallationEntity> getActualDependencies() {
		return actualDependencies;
	}

	public PackageInstallerEntity getPackageInstaller() {
		return packageInstaller;
	}

	public String getPackageName() {
		return packageName;
	}

	public Version getPackageVersion() {
		return packageVersion;
	}

	public String getSourceLocation() {
		return sourceLocation;
	}

	public String getSourceType() {
		return sourceType;
	}

	public Version getSourceVersion() {
		return sourceVersion;
	}

	public void setPackageInstaller(PackageInstallerEntity packageInstaller) {
		this.packageInstaller = packageInstaller;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setPackageVersion(Version packageVersion) {
		this.packageVersion = packageVersion;
	}

	public void setSourceLocation(String sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public void setSourceVersion(Version sourceVersion) {
		this.sourceVersion = sourceVersion;
	}

	public Long getId() {
		return id;
	}

	public Path getLibraryPath() {
		return libraryPath;
	}

	public void setLibraryPath(Path libraryPath) {
		this.libraryPath = libraryPath;
	}

	public void setFailed(Boolean failed) {
		this.isFailed = failed;
	}

	public Boolean isFailed() {
		return isFailed;
	}

	public List<CMDOutputMessage> getOutputMessages() {
		return cMDOutputMessages;
	}

	public void setOutputMessages(Collection<CMDOutputMessage> messages) {
		cMDOutputMessages = new ArrayList<>(messages);
	}
}