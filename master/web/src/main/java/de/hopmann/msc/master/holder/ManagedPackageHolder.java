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
package de.hopmann.msc.master.holder;

import java.io.Serializable;

import de.hopmann.msc.master.ejb.service.PackageService;


public class ManagedPackageHolder implements Serializable {

	private static final long serialVersionUID = 1L;
	private String packageName;
	private PackageService packageService;

	public ManagedPackageHolder(String packageName, PackageService packageService) {
		this.packageName = packageName;
		this.packageService = packageService;
	}

	public String getPackageName() {
		return packageName;
	}

}
