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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigInteger;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.util.VersionHelper;

/**
 * Annotation for qualifying an R Environment as component in this systems
 * 
 */
@Inherited
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER,
		ElementType.TYPE })
public @interface VersionQualifier {
	@Nonbinding
	String dependencyMinVersion() default "";

	@Nonbinding
	String dependencyMaxVersion() default "";

	@Nonbinding
	String value();

	@Nonbinding
	String architecture() default "";

	@Nonbinding
	String osType() default "";

	@Nonbinding
	String flavor() default "";

	@Nonbinding
	int priority() default 0;

	public class VersionQaulifierHolder {
		private final VersionQualifier versionQualifier;
		private BigInteger dependencyMinVersion;
		private BigInteger dependencyMaxVersion;
		private BigInteger rVersion;

		public VersionQaulifierHolder(VersionQualifier versionQualifier)
				throws VersionFormatException {
			this.versionQualifier = versionQualifier;
			this.dependencyMinVersion = VersionHelper
					.getVersionNumber(versionQualifier.dependencyMinVersion());
			this.dependencyMaxVersion = VersionHelper
					.getVersionNumber(versionQualifier.dependencyMaxVersion());
			this.rVersion = VersionHelper.getVersionNumber(versionQualifier
					.value());
		}

		public BigInteger getDependencyMaxVersion() {
			return dependencyMaxVersion;
		}

		public BigInteger getDependencyMinVersion() {
			return dependencyMinVersion;
		}

		public BigInteger getRVersion() {
			return rVersion;
		}

		public String getArchitecture() {
			return versionQualifier.architecture();
		}

		public String getFlavor() {
			return versionQualifier.flavor();
		}
	}
}
