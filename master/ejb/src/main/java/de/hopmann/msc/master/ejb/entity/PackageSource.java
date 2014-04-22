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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Entity implementation class for Entity: PackageClassEntity
 * 
 */
@Entity
public class PackageSource implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long id;

	private String sourceType;

	private String sourceLocation;

	private long maxRevisionNumber;

	@NotNull
	private String packageName;

	@ManyToOne
	private PackageContext repositoryEntity;

	@OneToMany
	private List<PackageResult> packageResults = new ArrayList<>();

	public PackageSource() {
		super();
	}

	public PackageContext getRepository() {
		return repositoryEntity;
	}

	public List<PackageResult> getPackageResults() {
		return packageResults;
	}

	public String getPackageName() {
		return packageName;
	}

	public long getMaxRevisionNumber() {
		return maxRevisionNumber;
	}

	public String getSourceLocation() {
		return sourceLocation;
	}

	public Long getId() {
		return id;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setRepository(PackageContext contextEntity) {
		this.repositoryEntity = contextEntity;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setSourceLocation(String sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public void addPackageResult(PackageResult resultEntity) {
		packageResults.add(resultEntity);
		if (resultEntity.getRevision() > this.maxRevisionNumber) {
			this.maxRevisionNumber = resultEntity.getRevision();
		}
		resultEntity.setOwningPackageEntityInternal(this);
	}

	public void setMaxRevisionNumber(long revision) {
		this.maxRevisionNumber = revision;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PackageSource)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		PackageSource other = (PackageSource) obj;

		return new EqualsBuilder()
				.append(getSourceType(), other.getSourceType())
				.append(getSourceLocation(), other.getSourceLocation())
				.append(getRepository(), other.getRepository())
				.append(getPackageName(), other.getPackageName()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(getSourceType())
				.append(getSourceLocation()).append(getRepository())
				.append(getPackageName()).toHashCode();
	}
	
}
