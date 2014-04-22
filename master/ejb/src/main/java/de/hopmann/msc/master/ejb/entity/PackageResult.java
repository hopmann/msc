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
package de.hopmann.msc.master.ejb.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import de.hopmann.msc.commons.model.CheckResult;
import de.hopmann.msc.commons.model.InstallationResult;
import de.hopmann.msc.commons.model.Version;

@Entity
public class PackageResult implements Serializable {

	public enum PackageResultType {
		CHECK, INSTALLATION, PROBE
	}

	private static final long serialVersionUID = 1L;
	@Embedded
	private CheckResult checkResult;

	@ManyToMany
	private List<PackageResult> dependencies = new ArrayList<PackageResult>(0);

	@Id
	@GeneratedValue
	private Long id;

	@Embedded
	private InstallationResult installationResult;

	private PackageResultType installationType;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private PackageSource packageSource;

	private long revision;
	private String sourceLocation;

	private String sourceType;

	private Version sourceVersion;

	protected PackageResult() {

	}

	public PackageResult(PackageResultType type) {
		setInstallationType(type);
	}

	public void addDependencies(Set<PackageResult> installations) {
		dependencies.addAll(installations);
	}

	public void addDependency(PackageResult dependency) {
		dependencies.add(dependency);
	}

	public CheckResult getCheckResult() {
		return checkResult;
	}

	public List<PackageResult> getDependencies() {
		return dependencies;
	}

	public Long getId() {
		return this.id;
	}

	public InstallationResult getInstallationResult() {
		return installationResult;
	}

	public PackageResultType getInstallationType() {
		return installationType;
	}

	public PackageSource getPackageSource() {
		return packageSource;
	}

	public long getRevision() {
		return revision;
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

	public void setCheckResult(CheckResult checkResult) {
		this.checkResult = checkResult;
	}

	public void setInstallationResult(InstallationResult installationResult) {
		this.installationResult = installationResult;
	}

	public void setInstallationType(PackageResultType installationType) {
		this.installationType = installationType;
	}

	void setOwningPackageEntityInternal(PackageSource packageSource) {
		this.packageSource = packageSource;
	}

	public void setPackageSource(PackageSource owningPackageEntity) {
		owningPackageEntity.addPackageResult(this);
	}

	public void setRevision(long revision) {
		this.revision = revision;
		if (packageSource != null) {
			if (this.revision > packageSource.getMaxRevisionNumber()) {
				packageSource.setMaxRevisionNumber(this.revision);
			}
		}
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

}
