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
package de.hopmann.msc.slave.util;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.hopmann.msc.commons.model.PackageSource.PathHolder;
import de.hopmann.msc.slave.entity.PackageInstallationEntity;

/**
 * Class covering the process of deploying a package dependency structure.
 * Provides maintenance functionalities such as rollback and resource handling,
 * and manages required package libraries.
 * 
 */
public class InstallationContext implements AutoCloseable {

	private Map<String, PackageInstallationEntity> libraryPackages = new HashMap<>();
	private Set<PathHolder> fileHolders = new HashSet<>();
	private Set<Path> libPath = new HashSet<>();
	private PackageInstallationEntity installation;

	public void addLibPackage(PackageInstallationEntity packageEntity) {
		synchronized (libraryPackages) {
			libraryPackages.put(packageEntity.getPackageName(), packageEntity);
		}
		synchronized (libPath) {
			libPath.add(packageEntity.getLibraryPath());
		}
	}

	public Set<PackageInstallationEntity> getLibraryPackages() {
		return new HashSet<PackageInstallationEntity>(libraryPackages.values());
	}

	public void registerPathHolder(PathHolder holder) {
		synchronized (holder) {
			fileHolders.add(holder);
		}
	}

	@Override
	public void close() throws Exception {
		synchronized (fileHolders) {
			for (PathHolder holder : fileHolders) {
				holder.close();
			}
			fileHolders.clear();
		}
	}

	public void setInstallation(
			PackageInstallationEntity packageInstallationEntity) {
		this.installation = packageInstallationEntity;
	}

	public PackageInstallationEntity getInstallation() {
		return installation;
	}

	public Set<Path> getLibraryPaths() {
		return libPath;
	}

	public PackageInstallationEntity getLibraryPackage(String packageName) {
		return libraryPackages.get(packageName);
	}

}
