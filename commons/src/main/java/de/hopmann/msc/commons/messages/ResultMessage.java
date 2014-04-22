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

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.hibernate.validator.constraints.NotEmpty;

@XmlSeeAlso({ CheckResultMessage.class, ExceptionResultMessage.class })
@XmlAccessorType(XmlAccessType.NONE)
public abstract class ResultMessage {

	@XmlAttribute
	private Long contextIdRef;

	@NotNull
	@NotEmpty
	@XmlElementRef
	private PackageResultMessage packageMessage;
	@XmlElementRef
	private PackageInstallerMessage mPackageInstaller;
	@XmlElementRef
	@XmlElementWrapper(name = "messages")
	private List<CMDOutputMessage> cMDOutputMessages;

	public ResultMessage() {
		super();
	}

	public void setPackage(PackageResultMessage pkg) {
		this.packageMessage = pkg;
	}

	public PackageResultMessage getPackageDescription() {
		return packageMessage;
	}

	public PackageInstallerMessage getPackageInstaller() {
		return mPackageInstaller;
	}

	public void setPackageInstaller(PackageInstallerMessage mPackageInstaller) {
		this.mPackageInstaller = mPackageInstaller;
	}

	public void setOutputMessages(List<CMDOutputMessage> cMDOutputMessages) {
		this.cMDOutputMessages = cMDOutputMessages;
	}

	public void setContextIdRef(Long contextIdRef) {
		this.contextIdRef = contextIdRef;
	}

	public Long getContextIdRef() {
		return contextIdRef;
	}

}