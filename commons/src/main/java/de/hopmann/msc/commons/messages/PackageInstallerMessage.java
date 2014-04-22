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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PackageInstaller")
@XmlAccessorType(XmlAccessType.NONE)
public class PackageInstallerMessage {

	protected PackageInstallerMessage() {

	}

	public PackageInstallerMessage(String versionString) {
		this.versionString = versionString;
	}

	@XmlAttribute
	private String versionString;

	@XmlAttribute
	private String flavor;

	@XmlAttribute
	private String architecture;

	public String getVersionString() {
		return versionString;
	}

	public String getArchitecture() {
		return architecture;
	}

	public String getFlavor() {
		return flavor;
	}

	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

}
