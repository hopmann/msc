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
import java.lang.Long;
import java.lang.String;

import javax.persistence.*;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Entity implementation class for Entity: PackageContext
 * 
 */
@Entity
public class PackageContext implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	private String name; // TODO name id

	private String rVersion; // TODO Execution Context

	private static final long serialVersionUID = 1L;

	private long revisionNumber;

	public PackageContext() {

	}

	public PackageContext(String name) {
		this.name = name;
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getRevisionNumber() {
		return revisionNumber;
	}

	public void setRevisionNumber(long revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	public String getRVersion() {
		return rVersion;
	}

	public void setRVersion(String rVersion) {
		this.rVersion = rVersion;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PackageContext)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		PackageContext other = (PackageContext) obj;

		return new EqualsBuilder().append(getName(), other.getName())
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(getName()).toHashCode();
	}

}
