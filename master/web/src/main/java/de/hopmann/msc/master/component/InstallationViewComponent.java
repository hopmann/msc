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
package de.hopmann.msc.master.component;

import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIInput;

import de.hopmann.msc.commons.model.CheckResult;
import de.hopmann.msc.commons.model.InstallationResult;
import de.hopmann.msc.master.ejb.entity.PackageResult;

@FacesComponent
public class InstallationViewComponent extends UIInput implements
		NamingContainer {

	private static final String FAMILY = "javax.faces.NamingContainer";

	public enum InstallationStatus {
		OK("ok"), ERROR("error"), WARNING("warning"); // TODO type Outdated etc.

		private String styleType;

		private InstallationStatus(String styleType) {
			this.styleType = styleType;
		}

		public String getStyleType() {
			return styleType;
		}
	}

	@Override
	public String getFamily() {
		return FAMILY;
	}

	@Override
	public Object getSubmittedValue() {
		return this;
	}

	@Override
	public PackageResult getValue() {
		return (PackageResult) super.getValue();
	}

	public InstallationStatus getInstallationStatus() {
		PackageResult packageResult = getValue();

		if (packageResult.getCheckResult() != null) {
			CheckResult checkResult = packageResult.getCheckResult();
			if (checkResult.getCheckErrorCount() > 0)
				return InstallationStatus.ERROR;
			if (checkResult.getCheckWarningCount() > 0) {
				return InstallationStatus.WARNING;
			}
			return InstallationStatus.OK;
		}

		if (packageResult.getInstallationResult() != null) {
			InstallationResult installationResult = packageResult
					.getInstallationResult();
			return installationResult.isFailed() ? InstallationStatus.ERROR
					: InstallationStatus.OK;
		}

		return InstallationStatus.WARNING;

	}
}
