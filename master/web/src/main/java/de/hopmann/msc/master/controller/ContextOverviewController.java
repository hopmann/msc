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

import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.hopmann.msc.master.ejb.entity.PackageContext;
import de.hopmann.msc.master.ejb.service.CheckTaskService;

@Named
@ViewScoped
public class ContextOverviewController implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private FacesContext facesContext;

	@Inject
	private CheckTaskService checkTaskService;

	@Produces
	@Named
	private PackageContext currentPackageContext;

	private String packageNameToCheck;

	public String getPackageNameToCheck() {
		return packageNameToCheck;
	}

	public void setPackageNameToCheck(String packageNameToCheck) {
		this.packageNameToCheck = packageNameToCheck;
	}

	public void setCurrentPackageContext(PackageContext packageContext) {
		this.currentPackageContext = packageContext;
	}

	public PackageContext getCurrentPackageContext() {
		return currentPackageContext;
	}

	public void checkPackage() {
		checkTaskService.queueCheck(packageNameToCheck, currentPackageContext);
	}

}
