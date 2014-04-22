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
package de.hopmann.msc.master.controller;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.hopmann.msc.master.ejb.service.PackageService;
import de.hopmann.msc.master.holder.ManagedPackageHolder;

@ViewScoped
@Named
public class CurrentPackageController implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<String, ManagedPackageHolder> packageHolderCache = new HashMap<>();

	@Inject
	private FacesContext facesContext;
	@Inject
	private PackageService packageService;

	private ManagedPackageHolder currentPackage;

	@Produces
	@Named("currentPackage")
	public ManagedPackageHolder getCurrentPackage() {
		return currentPackage;
	}

	public void setCurrentPackage(ManagedPackageHolder currentPackage) {
		this.currentPackage = currentPackage;
	}

	public ManagedPackageHolder getPackageHolder(String packageName) {

		ManagedPackageHolder managedPackageHolder = packageHolderCache
				.get(packageName);
		if (managedPackageHolder == null) {
			managedPackageHolder = new ManagedPackageHolder(packageName,
					packageService);
			packageHolderCache.put(packageName, managedPackageHolder);
		}

		return managedPackageHolder;
	}
}
