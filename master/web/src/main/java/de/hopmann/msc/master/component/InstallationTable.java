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

import java.util.AbstractList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class InstallationTable<ColValue, RowValue, CellValue> {

	public enum RowType {
		CHECK, UPSTREAM_INSTALLATION, DOWNSTREAM_INSTALLATION
	}

	private LinkedHashMap<ColValue, InstallationColumn> columns = new LinkedHashMap<>();
	private LinkedHashMap<RowValue, InstallationRow> rows = new LinkedHashMap<>();

	public class InstallationColumn {
		private final ColValue value;

		public InstallationColumn(ColValue value) {
			this.value = value;
		}

		public ColValue getValue() {
			return value;
		}

		@Override
		public boolean equals(Object obj) {
			return value.equals(obj);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}

	public class InstallationRow {
		private Map<ColValue, CellValue> cells = new HashMap<>();
		private RowType rowType;
		private RowValue rowValue;

		public InstallationRow(RowValue rowValue) {
			this.rowValue = rowValue;
		}

		public void setRowType(RowType rowType) {
			this.rowType = rowType;
		}

		public RowType getRowType() {
			return rowType;
		}

		public RowValue getRowValue() {
			return rowValue;
		}

		/**
		 * Returns a {@link List} of cells ordered according to the columns
		 * registered in the superior {@link InstallationTable} structure.
		 * 
		 * @return {@link List} of cells with fixed ordering according to known
		 *         columns
		 */
		public List<CellValue> getCells() {
			return new AbstractList<CellValue>() {

				private List<InstallationColumn> columnList = new Vector<InstallationColumn>(
						columns.values());

				@Override
				public CellValue get(int index) {
					return cells.get(columnList.get(index));
				}

				@Override
				public int size() {
					return columns.size();
				}
			};
		}

		public void putCell(ColValue col, CellValue cell) {
			cells.put(col, cell);
			registerColumn(col);
		}

	}

	private void registerColumn(ColValue col) {
		// Although Set semantics required, JSF needs List implementation
		if (!columns.containsKey(col)) {
			columns.put(col, new InstallationColumn(col));
		}
	}

	public InstallationRow getRow(RowValue value) {
		InstallationRow installationRow = rows.get(value);
		if (installationRow == null) {
			installationRow = new InstallationRow(value);
			rows.put(value, installationRow);
		}

		return installationRow;
	}

	public List<InstallationRow> getRows() {
		return new Vector<InstallationRow>(rows.values()); // XXX performance?
	}

	public List<InstallationColumn> getColumns() {
		return new Vector<InstallationColumn>(columns.values());// XXX
																// performance?
	}

}