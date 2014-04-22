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

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Builder class to interface with R CMD check
 * 
 */
public class RCheckBuilder extends RCMDBuilder<RCheckBuilder> {

	public enum InstallArgument {
		NO_INSTALL("--no-install"), NO_MANUAL("--no-manual"), NO_CLEAN(
				"--no-clean"), NO_CODOC("--no-codoc"), NO_EXAMPLES(
				"--no-examples"), NO_TESTS("--no-tests"), NO_VIGNETTES(
				"--no-vignettes"), NO_BUILD_VIGNETTES("--no-build-vignettes"), AS_CRAN(
				"--as-cran"), NO_MULTIARCH("--no-multiarch");

		private String arg;

		private InstallArgument(String arg) {
			this.arg = arg;
		}
	}

	public enum EnvrionmentParameter {
		_R_CHECK_FORCE_SUGGESTS_
	}

	protected EnumSet<InstallArgument> arguments = EnumSet
			.noneOf(InstallArgument.class);
	protected Path installationLogPath;

	public RCheckBuilder(Path packagePath, Path workingDirectoryBasePath)
			throws IOException {
		super(packagePath, workingDirectoryBasePath);
		arguments = EnumSet.of(InstallArgument.NO_MULTIARCH,
				InstallArgument.NO_INSTALL, InstallArgument.NO_BUILD_VIGNETTES,
				InstallArgument.NO_MANUAL); // TODO as cran?
	}

	@Override
	protected void processCommands(List<String> commands) {
		commands.add("check");
		if (installationLogPath != null) {
			commands.add("--install=check:\""
					+ installationLogPath.toAbsolutePath().toString() + "\"");
			arguments.remove(InstallArgument.NO_INSTALL);
		}
		for (InstallArgument arg : arguments) {
			commands.add(arg.arg);
		}
	}

	@Override
	protected void processEnvironment(Map<String, String> environment) {

	}

	public RCheckBuilder setInstallationLogPath(Path installationLogPath) {
		this.installationLogPath = installationLogPath;
		return this;
	}

}
