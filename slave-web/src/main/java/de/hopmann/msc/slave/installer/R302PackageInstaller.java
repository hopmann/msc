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
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import de.hopmann.msc.commons.messages.CMDOutputMessage;
import de.hopmann.msc.commons.messages.CMDOutputMessage.CMDOutputStatus;
import de.hopmann.msc.commons.model.CheckResult;
import de.hopmann.msc.commons.model.InstallationResult;
import de.hopmann.msc.commons.qualifier.Configuration;
import de.hopmann.msc.commons.util.RCMDOutputReader;
import de.hopmann.msc.slave.util.RCheckBuilder;
import de.hopmann.msc.slave.util.RInstallBuilder;

/**
 * Implementation for R302 package deployments and checks
 * 
 */
@ApplicationScoped
@VersionQualifier(value = "3.0.2", dependencyMinVersion = "3.0.0", dependencyMaxVersion = "4.0.0")
public class R302PackageInstaller implements PackageInstaller {

	private List<String> corePackageNames = Arrays.asList(new String[] {
			"base", "boot", "class", "cluster", "codetools", "compiler",
			"datasets", "foreign", "graphics", "grDevices", "grid",
			"KernSmooth", "lattice", "MASS", "Matrix", "methods", "mgcv",
			"nlme", "nnet", "parallel", "rpart", "spatial", "splines", "stats",
			"stats4", "survival", "tcltk", "tools", "utils" });

	@Inject
	private Logger log;

	private Path workingDirectoryBasePath;

	private Path rExecuteablePath;

	R302PackageInstaller() {

	}

	@Inject
	R302PackageInstaller(
			@Configuration(value = "rWorkingDirectoryBase", required = true) String workingDirectoryBase) {
		workingDirectoryBasePath = Paths.get(workingDirectoryBase);
		rExecuteablePath = Paths
				.get("C:\\Program Files\\R\\R-3.0.2\\bin\\R.exe");
	}

	@Override
	public InstallerResult<InstallationResult> installPackage(
			Path sourceDirectoryPath, Path libraryDirectoryPath,
			Path logOutputPath, Set<Path> libPath) throws IOException {
		log.info("Installing package from directory " + sourceDirectoryPath
				+ " into library " + libraryDirectoryPath);

		RCMDOutputReader outputReader = new RInstallBuilder(
				sourceDirectoryPath, workingDirectoryBasePath)
				.setEnvironment("GDAL_HOME", "C:/Rtools/local")
				.setSourceLibraries(libPath)
				.setRExecutablePath(rExecuteablePath)
				.setTargetLibraryPath(libraryDirectoryPath)
				.setLogPrintStream(new PrintStream(logOutputPath.toFile()))
				.start();
		// TODO r executable, commands, envir

		InstallerResult<InstallationResult> result = new InstallerResult<>();

		CMDOutputMessage cMDOutputMessage = null;
		while ((cMDOutputMessage = outputReader.readMessage()) != null) {
			result.addOutputMessage(cMDOutputMessage);
		}
		// TODO return code

		outputReader.close();

		result.setResult(new InstallationResult(!outputReader.isDone()));

		return result;

	}

	@Override
	public InstallerResult<CheckResult> checkPackage(Path sourceDirectory,
			Path installationLibraryPath, Path installationLogPath,
			Path logOutputPath, Set<Path> libPath) throws IOException {
		log.info("Checking package from directory " + sourceDirectory);

		RCMDOutputReader outputReader = new RCheckBuilder(sourceDirectory,
				workingDirectoryBasePath).setRExecutablePath(rExecuteablePath)
				.setInstallationLogPath(installationLogPath)
				.setTargetLibraryPath(installationLibraryPath)
				.setSourceLibraries(libPath)
				.setLogPrintStream(new PrintStream(logOutputPath.toFile()))
				.start();

		InstallerResult<CheckResult> result = new InstallerResult<>();

		CMDOutputMessage cMDOutputMessage = null;
		while ((cMDOutputMessage = outputReader.readMessage()) != null) {
			result.addOutputMessage(cMDOutputMessage);
		}
		// TODO return code

		outputReader.close();

		CheckResult checkResult = new CheckResult();
		checkResult.setErrorCount(outputReader
				.getStatusCount(CMDOutputStatus.ERROR));
		checkResult.setWarningCount(outputReader
				.getStatusCount(CMDOutputStatus.WARNING));
		checkResult.setNoteCount(outputReader
				.getStatusCount(CMDOutputStatus.NOTE));
		checkResult.setSkippedCount(outputReader
				.getStatusCount(CMDOutputStatus.SKIPPED));

		result.setResult(checkResult);

		return result;
	}

	@Override
	public List<String> getCorePackageNames() {
		return corePackageNames;
	}

}
