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
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.util.VersionHelper;

@Embeddable
public class VersionConstraintEntity implements Serializable {

	public enum VersionRelation {
		LESS("<"), LESSEQUAL("<="), EQUAL("="), GREATER(">"), GREATEREQUAL(">="), NONE(
				null);

		private String symbol;

		private VersionRelation(String symbol) {
			this.symbol = symbol;
		}

		public static VersionRelation valueOfRelationString(String value) {
			if (value == null) {
				return null;
			}

			for (VersionRelation relation : VersionRelation.values()) {
				if (value.equals(relation.symbol)) {
					return relation;
				}
			}

			return null;
		}
	}

	private static final long serialVersionUID = 1L;

	private static Pattern versionConstraintPattern = Pattern
			.compile("([<>=]*)\\s*([0-9.\\-]+)");

	@Column(columnDefinition = "numeric")
	private BigInteger minVersion;
	@Column(columnDefinition = "numeric")
	private BigInteger maxVersion;

	VersionConstraintEntity() {

	}

	public static VersionConstraintEntity valueOf(String value)
			throws VersionFormatException {
		if (value == null) {
			return null;
		}
		// TODO correct handling of multiple constraints
		VersionConstraintEntity versionConstraintEntity = new VersionConstraintEntity();

		Matcher matcher = versionConstraintPattern.matcher(value);
		while (matcher.find()) {
			VersionRelation relation = VersionRelation
					.valueOfRelationString(matcher.group(1));
			BigInteger versionNumber = VersionHelper.getVersionNumber(matcher
					.group(2));

			// TODO check validity, catch exceptions
			if (relation != null) {
				switch (relation) {
				case EQUAL:
					versionConstraintEntity.maxVersion = versionNumber
							.add(BigInteger.ONE);
					versionConstraintEntity.minVersion = versionNumber;
					break;
				case GREATER:
					versionConstraintEntity.minVersion = versionNumber
							.add(BigInteger.ONE);
					break;
				case GREATEREQUAL:
					versionConstraintEntity.minVersion = versionNumber;
					break;
				case LESS:
					versionConstraintEntity.maxVersion = versionNumber;
					break;
				case LESSEQUAL:
					versionConstraintEntity.maxVersion = versionNumber
							.add(BigInteger.ONE);
					break;
				default:
					// TODO exception
					break;
				}
			}
		}

		if (versionConstraintEntity.minVersion != null
				|| versionConstraintEntity.maxVersion != null) {
			return versionConstraintEntity;
		} else {
			return null;
		}
	}

	public void setMinVersion(BigInteger minVersion) {
		this.minVersion = minVersion;
	}

	public void setMaxVersion(BigInteger maxVersion) {
		this.maxVersion = maxVersion;
	}

	public BigInteger getMaxVersion() {
		return maxVersion;
	}

	public BigInteger getMinVersion() {
		return minVersion;
	}

}
