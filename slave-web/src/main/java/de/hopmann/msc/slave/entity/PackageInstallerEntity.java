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
package de.hopmann.msc.slave.entity;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.slave.installer.PackageInstallerHolder;

@Embeddable
@Access(AccessType.FIELD)
public class PackageInstallerEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "versionNumber", column = @Column(name = "installerversionnumber", columnDefinition = "numeric")),
		@AttributeOverride(name = "versionString", column = @Column(name = "installerversionstring")) })
	private Version version;

	private String flavor;

	private String architecture;

	PackageInstallerEntity() {

	}

	public PackageInstallerEntity(PackageInstallerHolder installerHolder) {
		version = new Version(installerHolder.getVersion());
		flavor = installerHolder.getFlavor();
		architecture = installerHolder.getArchitecture();
	}

	public String getArchitecture() {
		return architecture;
	}

	public String getFlavor() {
		return flavor;
	}

	public Version getVersion() {
		return version;
	}

}
