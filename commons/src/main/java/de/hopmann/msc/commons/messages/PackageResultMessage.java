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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import de.hopmann.msc.commons.model.InstallationResult;
import de.hopmann.msc.commons.model.Version;

@XmlRootElement(name = "PackageResult")
@XmlAccessorType(XmlAccessType.NONE)
public class PackageResultMessage {

	public PackageResultMessage() {

	}

	@XmlElement
	private String name;

	@XmlElement
	private Version packageVersion;

	@XmlElement
	private String sourceType;

	@XmlElement
	private String sourceLocation;

	@XmlElement
	private Version sourceVersion;

	@XmlElement
	private InstallationResult installationResult;

	@XmlElementRef
	@XmlElementWrapper(name = "messages")
	private List<CMDOutputMessage> cMDOutputMessages;

	public void setName(String name) {
		this.name = name;
	}

	public String getPackageName() {
		return name;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getSourceType() {
		return sourceType;
	}

	public Version getPackageVersion() {
		return packageVersion;
	}

	public void setPackageVersion(Version packageVersion) {
		this.packageVersion = packageVersion;
	}

	public void setSourceLocation(String sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public String getSourceLocation() {
		return sourceLocation;
	}

	public Version getSourceVersion() {
		return sourceVersion;
	}

	public void setSourceVersion(Version sourceVersion) {
		this.sourceVersion = sourceVersion;
	}

	public InstallationResult getInstallationResult() {
		return installationResult;
	}

	public void setInstallationResult(InstallationResult installationResult) {
		this.installationResult = installationResult;
	}

	public void setOutputMessages(List<CMDOutputMessage> cMDOutputMessages) {
		this.cMDOutputMessages = cMDOutputMessages;
	}

}
