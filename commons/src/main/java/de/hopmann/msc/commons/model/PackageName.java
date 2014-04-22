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
package de.hopmann.msc.commons.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


public class PackageName {

	protected String name;

	// protected PackageVersion version;

//	protected Set<PackageInstallation> variations = new HashSet<PackageInstallation>();

	public PackageName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name not set");

		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public String getName() {
		return name;
	}

	// public PackageVersion getVersion() {
	// return version;
	// }

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PackageName)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		PackageName other = (PackageName) obj;
		return new EqualsBuilder().append(name, other.name)
		// .append(version, other.version)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(name)
		// .append(version)
				.toHashCode();
	}

}