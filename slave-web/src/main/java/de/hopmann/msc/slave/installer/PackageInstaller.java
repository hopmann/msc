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
package de.hopmann.msc.slave.installer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.hopmann.msc.commons.messages.CMDOutputMessage;
import de.hopmann.msc.commons.model.CheckResult;
import de.hopmann.msc.commons.model.InstallationResult;

public interface PackageInstaller {

	public class InstallerException extends Exception {

		private static final long serialVersionUID = 1L;

		public InstallerException(String msg, Throwable cause) {
			super(msg, cause);
		}

		public InstallerException(String msg) {
			super(msg);
		}

		public InstallerException(Throwable cause) {
			super(cause);
		}

	}

	public static class InstallerResult<T> {

		private List<CMDOutputMessage> cMDOutputMessages = new ArrayList<>(0);
		private T result;

		public List<CMDOutputMessage> getOutputMessages() {
			return cMDOutputMessages;
		}

		public void addOutputMessage(CMDOutputMessage message) {
			cMDOutputMessages.add(message);
		}

		public T getResult() {
			return result;
		}

		public void setResult(T result) {
			this.result = result;
		}
	}

	InstallerResult<InstallationResult> installPackage(Path sourceDirectoryPath,
			Path libraryDirectoryPath, Path logOutputPath, Set<Path> libPath)
			throws IOException;

	List<String> getCorePackageNames();

	InstallerResult<CheckResult> checkPackage(Path sourceDirectoryPath,
			Path installationLibraryPath, Path installationLogPath,
			Path logOutputPath, Set<Path> libPath) throws IOException;

}
