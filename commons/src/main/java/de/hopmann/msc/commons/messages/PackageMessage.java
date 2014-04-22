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
package de.hopmann.msc.commons.messages;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import de.hopmann.msc.commons.model.SourceIdentifier;
import de.hopmann.msc.commons.model.Version;

@XmlRootElement(name = "Package")
@XmlAccessorType(XmlAccessType.NONE)
public class PackageMessage {

	public PackageMessage() {

	}

	@XmlElement
	private String name;

	@XmlElement
	private Version packageVersion;

	@XmlElementRef
	private SourceIdentifier sourceDescription;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public SourceIdentifier getSourceDescription() {
		return sourceDescription;
	}

	public void setSourceDescription(SourceIdentifier sourceIdentifier) {
		this.sourceDescription = sourceIdentifier;
	}

	public Version getPackageVersion() {
		return packageVersion;
	}

	public void setPackageVersion(Version packageVersion) {
		this.packageVersion = packageVersion;
	}

}
