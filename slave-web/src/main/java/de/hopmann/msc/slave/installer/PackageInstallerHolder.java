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
package de.hopmann.msc.slave.installer;

import java.math.BigInteger;
import java.util.List;

import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.util.VersionHelper;

/**
 * Wraps a {@link PackageInstaller} for more efficient access to version data
 * types
 * 
 */
public class PackageInstallerHolder {
	private PackageInstaller packageInstaller;

	private BigInteger version;
	private BigInteger dependencyMinVersion;
	private BigInteger dependencyMaxVersion;
	private String flavor;
	private String architecture;
	private String osType;

	public PackageInstallerHolder(PackageInstaller packageInstaller)
			throws VersionFormatException {
		this.packageInstaller = packageInstaller;

		VersionQualifier versionAnnotation = packageInstaller.getClass()
				.getAnnotation(VersionQualifier.class);
		if (versionAnnotation == null) {
			throw new IllegalArgumentException(
					"Provided package installer has no VersionQualifier annotation");
		}
		version = VersionHelper.getVersionNumber(versionAnnotation.value());

		dependencyMinVersion = VersionHelper.getVersionNumber(
				versionAnnotation.dependencyMinVersion(),
				VersionHelper.MIN_VERSION);
		dependencyMaxVersion = VersionHelper.getVersionNumber(
				versionAnnotation.dependencyMaxVersion(),
				VersionHelper.MAX_VERSION);
		architecture = getAnnotationString(versionAnnotation.architecture());
		flavor = getAnnotationString(versionAnnotation.flavor());
		osType = getAnnotationString(versionAnnotation.osType());
	}

	private String getAnnotationString(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		return value;
	}

	public PackageInstaller getPackageInstaller() {
		return packageInstaller;
	}

	public List<String> getCorePackageNames() {
		return packageInstaller.getCorePackageNames();
	}

	public BigInteger getVersion() {
		return version;
	}

	public String getFlavor() {
		return flavor;
	}

	public String getArchitecture() {
		return architecture;
	}

	public BigInteger getDependencyMaxVersion() {
		return dependencyMaxVersion;
	}

	public BigInteger getDependencyMinVersion() {
		return dependencyMinVersion;
	}

	public String getOsType() {
		return osType;
	}
}
