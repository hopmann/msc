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
 * Builder pattern to interact with R CMD INSTALL.
 * 
 */
public class RInstallBuilder extends RCMDBuilder<RInstallBuilder> {

	public enum InstallArgument {
		NO_CONFIGURE("--no-configure"), NO_DOCS("--no-docs"), HTML("--html"), NO_HTML(
				"--no-html"), LATEX("--latex"), EXAMPLE("--example"), FAKE(
				"--fake"), NO_LOCK("--no-lock"), LOCK("--lock"), PKGLOCK(
				"--pkglock"), BUILD("--build"), INSTALL_TESTS("--install-tests"), NO_R(
				"--no-R"), NO_LIBS("--no-libs"), NO_DATA("--no-data"), NO_HELP(
				"--no-help"), NO_DEMO("--no-demo"), NO_EXEC("--no-exec"), NO_INST(
				"--no-inst"), NO_MULTIARCH("--no-multiarch"), LIBS_ONLY(
				"--libs-only");

		private String arg;

		private InstallArgument(String arg) {
			this.arg = arg;
		}
	}

	protected EnumSet<InstallArgument> arguments;

	public RInstallBuilder(Path packagePath, Path workingDirectoryBasePath)
			throws IOException {
		super(packagePath, workingDirectoryBasePath);
		arguments = EnumSet.of(InstallArgument.NO_MULTIARCH,
				InstallArgument.NO_LOCK);
	}

	@Override
	protected void processCommands(List<String> commands) {
		commands.add("INSTALL");
		for (InstallArgument arg : arguments) {
			commands.add(arg.arg);
		}
	}

	@Override
	protected void processEnvironment(Map<String, String> environment) {

	}

}
