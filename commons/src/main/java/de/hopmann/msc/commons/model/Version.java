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

import java.math.BigInteger;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.util.VersionHelper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "Version")
@Embeddable
@Access(AccessType.FIELD)
public class Version implements Comparable<Version> {

	// TODO make sure that there is no comparison by versionString or on object
	// level
	@XmlAttribute
	private String versionString;

	@Column(columnDefinition = "numeric", nullable = false)
	@XmlElement
	@NotNull
	private BigInteger versionNumber;

	public BigInteger getVersionNumber() {
		return versionNumber;
	}

	public String getVersionString() {
		return versionString;
	}

	public void setVersionNumber(BigInteger versionNumber) {
		this.versionNumber = versionNumber;
	}

	public void setVersionString(String versionString) {
		this.versionString = versionString;
	}

	protected Version() {

	}

	public Version(BigInteger versionNumber) {
		this.versionNumber = versionNumber;
	}

	public Version(String versionString, BigInteger versionNumber) {
		this(versionNumber);
		this.versionString = versionString;
	}

	public static Version fromRVersion(String versionString)
			throws VersionFormatException {
		if (versionString == null || versionString.isEmpty()) {
			return null;
		}
		return new Version(versionString,
				VersionHelper.getVersionNumber(versionString));
	}

	public static Version fromVersionNumber(long versionNumber) {
		return new Version(BigInteger.valueOf(versionNumber));
	}

	@Override
	public int compareTo(Version other) {
		return this.versionNumber.compareTo(other.versionNumber);
	}

	@Override
	public String toString() {
		if (versionString != null && !versionString.isEmpty()) {
			return versionString;
		} else {
			return versionNumber.toString();
		}
	}

}
