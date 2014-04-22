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

import java.io.IOException;
import java.util.List;

import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

import de.hopmann.msc.master.ejb.entity.PackageResult;
import de.hopmann.msc.master.ejb.entity.PackageSource;
import de.hopmann.msc.master.holder.InstallationRangeHolder;
import de.hopmann.msc.master.holder.InstallationResultHolder;
import de.hopmann.msc.master.holder.PackageSourceHolder;

@FacesComponent
public class PackageCheckComponent extends UIInput implements NamingContainer {

	private class PackageCheckTable extends
			InstallationTable<Long, PackageSource, PackageResult> {

	}

	private static final String FAMILY = "javax.faces.NamingContainer";

	private PackageSourceHolder packageSourceHolder;
	private int maxPreviousInstallations;

	private PackageCheckTable installationTable;

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

	public PackageSource getCheckColumn() {
		return packageSourceHolder.getPackageSource();
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		packageSourceHolder = getAttribute("packageSource");
		maxPreviousInstallations = getAttribute("maxPreviousInstallations", 5);

		createTable();
		super.encodeBegin(context);
	}

	private <T> T getAttribute(String key, T defaultValue) {
		@SuppressWarnings("unchecked")
		T value = (T) getAttributes().get(key);
		if (value != null) {
			return value;
		} else {
			return defaultValue;
		}
	}

	private <T> T getAttribute(String key) {
		return getAttribute(key, null);
	}

	public InstallationTable<Long, PackageSource, PackageResult> getInstallationTable() {
		return installationTable;
	}

	@Override
	protected Object getConvertedValue(FacesContext context,
			Object newSubmittedValue) throws ConverterException {
		// TODO Auto-generated method stub
		return super.getConvertedValue(context, newSubmittedValue);
	}

	private void createTable() {

		// class PackageSourceRowMap extends
		// TreeMap<PackageSource, InstallationTable.InstallationRow> {
		// private static final long serialVersionUID = 1L;
		//
		// public InstallationTable.InstallationRow getRow(
		// PackageSource packageSource, RowType rowType) {
		// InstallationTable.InstallationRow installationRow = super
		// .get(packageSource);
		// if (installationRow == null) {
		// installationRow = installationTable.createInstallationRow();
		// installationRow.setRowType(rowType);
		// this.put(packageSource, installationRow);
		// }
		// return installationRow;
		// }
		// }

		installationTable = new PackageCheckTable();

		List<InstallationResultHolder> checkResults = packageSourceHolder
				.getCheckResults();

		for (InstallationResultHolder installationResultHolder : checkResults) {
			// PackageSourceRowMap packageSourceRowMap = new
			// PackageSourceRowMap();

			// Include main check result
			PackageResult checkResult = installationResultHolder.getInstallationResult();
			PackageCheckTable.InstallationRow installationRow = installationTable
					.getRow(checkResult.getPackageSource());
			installationRow.putCell(checkResult.getRevision(), checkResult);

			for (InstallationRangeHolder installationRange : installationResultHolder
					.getDependencies()) {
				// Add previous installations
				PackageSource rowId = installationRange.getInstallation()
						.getPackageSource();

				for (PackageResult installation : installationRange
						.getPreviousInstallations()) {
					installationRow = installationTable.getRow(rowId);
					installationRow.putCell(installation.getRevision(),
							installation);
				}
			}

		}
	}
}
