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
package de.hopmann.msc.slave.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import de.hopmann.msc.commons.exception.PackageNotFoundException;
import de.hopmann.msc.commons.messages.CheckResultMessage;
import de.hopmann.msc.commons.messages.CheckTaskMessage;
import de.hopmann.msc.commons.messages.ExceptionResultMessage;
import de.hopmann.msc.commons.messages.ResultMessage;
import de.hopmann.msc.commons.model.CheckResult;
import de.hopmann.msc.commons.model.DependencyType;
import de.hopmann.msc.commons.model.PackageSource.PathHolder;
import de.hopmann.msc.slave.entity.PackageInstallationEntity;
import de.hopmann.msc.slave.installer.PackageInstaller.InstallerResult;
import de.hopmann.msc.slave.installer.PackageInstallerHolder;
import de.hopmann.msc.slave.service.PackageInstallationBean.InstallationException;
import de.hopmann.msc.slave.service.ResolverBean.CheckTaskHolder;
import de.hopmann.msc.slave.service.ResolverBean.PackageResolved;
import de.hopmann.msc.slave.util.InstallationContext;

/**
 * Component managing actual package checks.
 *
 */
@ApplicationScoped
public class PackageCheckService {

	private PackageInstallationBean packageInstallationService;
	private ResolverBean resolverBean;

	PackageCheckService() {

	}

	@Inject
	PackageCheckService(PackageInstallationBean packageVariationEJB,
			ResolverBean resolver) {
		this.packageInstallationService = packageVariationEJB;
		this.resolverBean = resolver;
	}

	@Inject
	private Logger log;

	public ResultMessage checkTask(final CheckTaskMessage checkTaskMessage,
			PackageInstallerHolder installerHolder) {
		// TODO installerHolder

		CheckTaskHolder checkTaskHolder = resolverBean
				.resolveDescription(checkTaskMessage);

		try {
			return checkPackage(checkTaskHolder.getPackageModel(),
					checkTaskHolder.getPackageInstallerHolder());
		} catch (PackageNotFoundException e) {
			return null;// TODO
		}
	}

	public ResultMessage checkTask(final CheckTaskMessage checkTaskDescription) {
		CheckTaskHolder checkTaskHolder = resolverBean
				.resolveDescription(checkTaskDescription);

		try {
			return checkPackage(checkTaskHolder.getPackageModel(),
					checkTaskHolder.getPackageInstallerHolder());
		} catch (PackageNotFoundException e) {
			return null;// TODO
		}
	}

	private ResultMessage checkPackage(final PackageResolved packageResolved,
			final PackageInstallerHolder packageInstallerHolder)
			throws PackageNotFoundException {

		class CheckHolder {
			Future<PathHolder> sourceFuture;
			Future<InstallationContext> packageInstallationFuture;
			List<Future<InstallationContext>> checkDependencyFutures;
			List<InstallationContext> dependencyInstallations;
			PathHolder sourceDirectory;
			InstallationContext packageInstallation;
			InstallerResult<CheckResult> checkResult;

			private ResultMessage check() {
				// TODO implement special Future which allows concurrent access
				// to its InstallationContext during execution to return all
				// information obtained in the meantime if an unrelated error
				// occurred before get() was invoked.

				log.info("Preparing check of package "
						+ packageResolved.getPackageName());

				log.info("Acquiring Future source for package "
						+ packageResolved.getPackageName());
				sourceFuture = packageResolved.getPackageAccessor()
						.acquireSource();

				log.info("Acquiring Future installation for package "
						+ packageResolved.getPackageName());
				// Get installation of package
				try {
					packageInstallationFuture = packageInstallationService
							.acquireInstallation(packageResolved,
									packageInstallerHolder);
				} catch (PackageNotFoundException e) {
					return returnException(e);
				}

				log.info(packageResolved.getPackageName()
						+ " acquiring Future installations for depending packages to set up check libraries");
				Set<PackageResolved> checkDependencies = packageResolved
						.getDependencies().getDependencies(
								DependencyType.CMD_CHECK);
				// TODO handle SUGGESTS and ENHANCES separately to omit missing
				// packages instead of failing

				// Get installations of all check-dependencies
				List<String> corePackages = packageInstallerHolder
						.getCorePackageNames();
				Iterator<PackageResolved> dependenciesIterator = checkDependencies
						.iterator();
				while (dependenciesIterator.hasNext()) {
					// TODO XXX DRY
					PackageResolved nextDep = dependenciesIterator.next();
					if (corePackages.contains(nextDep.getPackageName())
							&& !nextDep.getPackageAccessor().isAvailable()) {
						// Package is provided and not available in resolved
						// context -> safe to exclude from check
						dependenciesIterator.remove();
						continue;
					}

					String depOsType = nextDep.getOSType();
					if (depOsType != null
							&& !depOsType.equals(packageInstallerHolder
									.getOsType())) {
						// package not supported by installer OS
						dependenciesIterator.remove();
						log.info("Dependency ignored because of os type "
								+ nextDep);
					}
				}
				checkDependencyFutures = new ArrayList<>(
						checkDependencies.size());
				try {
					for (PackageResolved dependency : checkDependencies) {
						// Request all required installations
						checkDependencyFutures.add(packageInstallationService
								.acquireInstallation(dependency,
										packageInstallerHolder));
					}
				} catch (PackageNotFoundException e) {
					return returnException(e);
				}

				log.info(packageResolved.getPackageName()
						+ " waiting for Future installation of all packages");
				dependencyInstallations = new ArrayList<>(
						checkDependencyFutures.size());
				try {
					for (Future<InstallationContext> checkDependencyFuture : checkDependencyFutures) {
						dependencyInstallations
								.add(checkDependencyFuture.get());
					}
				} catch (ExecutionException | InterruptedException e) {
					return returnException(e);
				}
				log.info("Package " + packageResolved.getPackageName()
						+ " installations ready");

				log.info("Waiting for Future source and installation of package "
						+ packageResolved.getPackageName());

				try {
					sourceDirectory = sourceFuture.get();
					packageInstallation = packageInstallationFuture.get();
				} catch (ExecutionException | InterruptedException e) {
					return returnException(e);
				}

				// Get library locations
				Set<Path> checkLibraryLocations = new HashSet<>(
						packageInstallation.getLibraryPaths());
				checkLibraryLocations.add(packageInstallation.getInstallation()
						.getLibraryPath());
				for (InstallationContext context : dependencyInstallations) {
					checkLibraryLocations.addAll(context.getLibraryPaths());
					checkLibraryLocations.add(context.getInstallation()
							.getLibraryPath());
				}

				// Check package
				try {
					this.checkResult = packageInstallerHolder
							.getPackageInstaller()
							.checkPackage(
									sourceDirectory.getPath(),
									packageInstallation.getInstallation()
											.getLibraryPath(),
									packageInstallationService
											.getPackageInstallationLogPath(packageInstallation
													.getInstallation()),
									packageInstallationService
											.getPackageCheckLogPath(packageInstallation
													.getInstallation()),
									checkLibraryLocations);

				} catch (IOException e) {
					return returnException(e);
				}

				log.info("Finished checking of package "
						+ packageResolved.getPackageName());

				return returnResult();
			}

			private void close() {
				if (sourceFuture != null) {
					sourceFuture.cancel(true);
				}
				if (packageInstallationFuture != null) {
					packageInstallationFuture.cancel(true);
				}
				if (checkDependencyFutures != null) {
					for (Future<?> future : checkDependencyFutures) {
						future.cancel(true);
					}
				}
				if (packageInstallation != null) {
					try {
						packageInstallation.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
					}
				}
				if (dependencyInstallations != null) {
					for (AutoCloseable closable : dependencyInstallations) {
						try {
							closable.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
						}
					}
				}
				if (sourceDirectory != null) {
					sourceDirectory.close();
				}
			}

			private ExceptionResultMessage returnException(Exception e) {
				log.log(Level.INFO, "Error while checking package", e);

				ExceptionResultMessage exceptionResultMessage = new ExceptionResultMessage();

				if (packageInstallation != null) {
					exceptionResultMessage.setPackage(resolverBean
							.toMessage(packageInstallation.getInstallation()));

					for (PackageInstallationEntity dependency : packageInstallation
							.getLibraryPackages()) {
						exceptionResultMessage.addInstalledPackage(resolverBean
								.toMessage(dependency));
					}
				} else {
					exceptionResultMessage.setPackage(resolverBean
							.toMessage(packageResolved.getPackageAccessor()));
				}

				Throwable cause = e;
				while (cause != null) {
					if (cause instanceof InstallationException) {
						exceptionResultMessage.setFailedPackage(resolverBean
								.toMessage(((InstallationException) cause)
										.getInstallationEntity()));
					}
					cause = cause.getCause();
				}

				// TODO maybe wrap check errors as exceptions

				return exceptionResultMessage;
			}

			private CheckResultMessage returnResult() {
				// stop execution / free resources
				// TODO maybe move to end
				close();

				// Return result message
				CheckResultMessage checkResultMessage = new CheckResultMessage();
				if (packageInstallation != null) {
					checkResultMessage.setPackage(resolverBean
							.toMessage(packageInstallation.getInstallation()));

					for (PackageInstallationEntity dependency : packageInstallation
							.getLibraryPackages()) {
						checkResultMessage
								.addInstallationDependency(resolverBean
										.toMessage(dependency));
					}
				} else {
					checkResultMessage.setPackage(resolverBean
							.toMessage(packageResolved.getPackageAccessor()));
				}

				if (dependencyInstallations != null) {
					Set<PackageInstallationEntity> checkDependencyInstallations = new HashSet<>();

					for (InstallationContext dependencyContext : dependencyInstallations) {
						checkDependencyInstallations.add(dependencyContext
								.getInstallation());
						checkDependencyInstallations.addAll(dependencyContext
								.getLibraryPackages());
					}

					for (PackageInstallationEntity dependency : checkDependencyInstallations) {
						checkResultMessage.addCheckDependency(resolverBean
								.toMessage(dependency));
					}
				}

				checkResultMessage.setPackageInstaller(resolverBean
						.toMessage(packageInstallerHolder));

				if (checkResult != null) {
					checkResultMessage.setCheckResult(checkResult.getResult());
					checkResultMessage.setOutputMessages(checkResult
							.getOutputMessages());
				}

				return checkResultMessage;
			}

		}

		return new CheckHolder().check();
	}
}
