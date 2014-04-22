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
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.SystemUtils;

import de.hopmann.msc.commons.util.RCMDOutputReader;

/**
 * Generic superclass for interfacing with R CMD tool. Handles settings for
 * reliably checking packages, such as collation, envinronment variables and the
 * unique library management.
 * 
 * @param <T>
 */
public abstract class RCMDBuilder<T extends RCMDBuilder<T>> {

	public static class RCMDException extends Exception {

		private static final long serialVersionUID = 1L;

		public RCMDException() {
			super();
		}

		public RCMDException(String message) {
			super(message);
		}

		public RCMDException(String message, Throwable cause) {
			super(message, cause);
		}

		public RCMDException(Throwable cause) {
			super(cause);
		}

	}

	private static final String R_LIBS_ENVIR = "R_LIBS";
	private static final String R_LANGUAGE_ENVIR = "LANGUAGE";
	private static final String CYQWIN_ENVIR = "CYGWIN";
	private static final String LC_ALL = "LC_ALL";

	private static final AtomicInteger NEXT_WORKINGDIR_ID = new AtomicInteger(0);
	private static final ThreadLocal<Integer> workingDirectoryId = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return NEXT_WORKINGDIR_ID.getAndIncrement();
		}
	};

	protected Path packagePath;
	protected Path workingDirectoryPath;
	protected Path rExecutablePath;
	protected Collection<Path> sourceLibraryPaths = Collections.emptyList();

	protected PrintStream logPrintStream;
	protected Path targetLibraryPath;
	private Map<String, String> customEnvironment = new HashMap<>();

	public RCMDBuilder(Path packagePath, Path workingDirectoryBasePath)
			throws IOException {
		this.packagePath = packagePath;
		workingDirectoryPath = workingDirectoryBasePath
				.resolve(workingDirectoryId.get() + "");
		Files.createDirectories(workingDirectoryPath);
	}

	protected abstract void processCommands(List<String> commands);

	protected abstract void processEnvironment(Map<String, String> environment);

	@SuppressWarnings("unchecked")
	public T setLogPrintStream(PrintStream logPrintStream) {
		this.logPrintStream = logPrintStream;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setRExecutablePath(Path rExecutablePath) {
		this.rExecutablePath = rExecutablePath;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setSourceLibraries(Collection<Path> sourceLibraries) {
		this.sourceLibraryPaths = sourceLibraries;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	private T setWorkingDirectoryPath(Path path) {
		this.workingDirectoryPath = path;
		return (T) this;
	}

	public T setWorkingDirectoryPath(String pathString) {
		return setWorkingDirectoryPath(Paths.get(pathString));
	}

	@SuppressWarnings("unchecked")
	public T setTargetLibraryPath(Path libPath) {
		this.targetLibraryPath = libPath;
		return (T) this;
	}

	public T setTargetLibraryPath(String libPath) {
		return setTargetLibraryPath(Paths.get(libPath));
	}

	@SuppressWarnings("unchecked")
	public T setEnvironment(String key, String value) {
		customEnvironment.put(key, value);
		return (T) this;
	}

	public RCMDOutputReader start() throws IOException {

		List<String> commands = new ArrayList<String>();
		commands.add(rExecutablePath.toAbsolutePath().toString());
		commands.add("CMD");

		processCommands(commands);

		if (targetLibraryPath != null) {
			commands.add("--library="
					+ targetLibraryPath.toAbsolutePath().toString());
		}

		commands.add(packagePath.toAbsolutePath().toString());

		ProcessBuilder cmdProcessBuilder = new ProcessBuilder(commands)
				.directory(workingDirectoryPath.toAbsolutePath().toFile())
				.redirectErrorStream(true);

		if (!sourceLibraryPaths.isEmpty()) {
			StringBuilder rlibs = new StringBuilder();

			for (Path libPath : sourceLibraryPaths) {
				if (rlibs.length() != 0) {
					// Add separator char
					rlibs.append(SystemUtils.IS_OS_WINDOWS ? ";" : ":");
				}
				rlibs.append(libPath.toAbsolutePath().toString());
			}
			Map<String, String> processEnvironment = cmdProcessBuilder
					.environment();
			processEnvironment.put(R_LIBS_ENVIR, rlibs.toString());
			processEnvironment.put(R_LANGUAGE_ENVIR, "en");
			processEnvironment.put(LC_ALL, "English");
			processEnvironment.put(CYQWIN_ENVIR, "nodosfilewarning");
			for (Entry<String, String> envirEntry : customEnvironment
					.entrySet()) {
				processEnvironment.put(envirEntry.getKey(),
						envirEntry.getValue());
			}
		}

		processEnvironment(cmdProcessBuilder.environment());

		final Process cmdProcess = cmdProcessBuilder.start();

		// BufferedReader processOut = new BufferedReader(
		// new InputStreamReader(installProcess.getInputStream()));
		// while (processOut.readLine() != null) { }

		// IOUtils.copy(installProcess.getInputStream(), System.out);

		return new RCMDOutputReader(cmdProcess, logPrintStream) {
			@Override
			public void close() throws IOException {
				try {
					cmdProcess.waitFor();// XXX or .destroy()?
				} catch (InterruptedException e) {

				}
				super.close();
			}
		};

	}

}
