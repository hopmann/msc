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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import de.hopmann.msc.commons.model.CheckResult;

@XmlRootElement(name = "CheckResult")
@XmlAccessorType(XmlAccessType.NONE)
public class CheckResultMessage extends ResultMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	public CheckResultMessage() {

	}
	
	@XmlElementRef
	@XmlElementWrapper(name = "installationDependencies")
	private Set<PackageResultMessage> installationDependencies = new HashSet<PackageResultMessage>(
			0);

	@XmlElementRef
	@XmlElementWrapper(name = "checkDependencies")
	private Set<PackageResultMessage> checkDependencies = new HashSet<PackageResultMessage>(
			0);

	@XmlElement
	private CheckResult checkResult;

	public CheckResult getCheckResult() {
		return checkResult;
	}

	public void setCheckResult(CheckResult checkResult) {
		this.checkResult = checkResult;
	}

	public void addInstallationDependency(PackageResultMessage dependencyMessage) {
		installationDependencies.add(dependencyMessage);
	}

	public Set<PackageResultMessage> getInstallationDependencies() {
		return installationDependencies;
	}

	public void addCheckDependency(PackageResultMessage dependencyMessage) {
		checkDependencies.add(dependencyMessage);
	}

	public Set<PackageResultMessage> getCheckDependencies() {
		return checkDependencies;
	}

}
