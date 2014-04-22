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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.validator.constraints.NotEmpty;

import de.hopmann.msc.commons.model.SourceIdentifier;

@XmlRootElement(name = "Check")
@XmlAccessorType(XmlAccessType.NONE)
public class CheckTaskMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	public CheckTaskMessage() {

	}

	@XmlAttribute
	private Long contextIdRef;

	@NotNull
	@NotEmpty
	@XmlElementRef
	private PackageMessage mPkgDescription;

	@XmlElementRef
	private SourceIdentifier.SourceRepositoryIdentifier mDefaultRepository;

	@XmlElementRef
	private PackageInstallerMessage mPackageInstaller;

	@XmlElementRef
	@XmlElementWrapper(name = "dependencies")
	private Set<PackageMessage> mPkgDependencies = new HashSet<PackageMessage>(
			0);

	private HashMap<String, PackageMessage> mPkgDependenciesCache;

	public void setPackage(PackageMessage pkg) {
		this.mPkgDescription = pkg;
	}

	public PackageMessage getPackageDescription() {
		return mPkgDescription;
	}

	public SourceIdentifier.SourceRepositoryIdentifier getDefaultRepository() {
		return mDefaultRepository;
	}

	public void setDefaultRepository(
			SourceIdentifier.SourceRepositoryIdentifier defaultRepository) {
		this.mDefaultRepository = defaultRepository;
	}

	public Set<PackageMessage> getDependencies() {
		return mPkgDependencies;
	}

	public void addDependency(PackageMessage packageMessage) {
		mPkgDependencies.add(packageMessage);
	}

	public PackageMessage getDependency(String pkgName) {
		if (mPkgDependenciesCache == null) {
			mPkgDependenciesCache = new HashMap<String, PackageMessage>();
			if (mPkgDependencies != null) {
				for (PackageMessage dependency : mPkgDependencies) {
					mPkgDependenciesCache.put(dependency.getName(), dependency);
				}
			}
		}

		return mPkgDependenciesCache.get(pkgName);
	}

	public PackageInstallerMessage getPackageInstaller() {
		return mPackageInstaller;
	}

	public void setPackageInstaller(PackageInstallerMessage mPackageInstaller) {
		this.mPackageInstaller = mPackageInstaller;
	}

	public Long getContextIdRef() {
		return contextIdRef;
	}

	public void setContextIdRef(Long contextIdRef) {
		this.contextIdRef = contextIdRef;
	}
}
