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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.transaction.Transactional;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import de.hopmann.msc.commons.exception.PackageNotFoundException;
import de.hopmann.msc.commons.model.DependencyType;
import de.hopmann.msc.commons.model.InstallationResult;
import de.hopmann.msc.commons.model.PackageSource.PackageAccessor;
import de.hopmann.msc.commons.model.PackageSource.PathHolder;
import de.hopmann.msc.commons.model.Version_;
import de.hopmann.msc.commons.qualifier.Configuration;
import de.hopmann.msc.commons.util.DispatchingExecutor.SimpleDispatchingExecutor;
import de.hopmann.msc.commons.util.FutureHelper;
import de.hopmann.msc.slave.entity.PackageInstallationEntity;
import de.hopmann.msc.slave.entity.PackageInstallationEntity_;
import de.hopmann.msc.slave.entity.PackageInstallerEntity;
import de.hopmann.msc.slave.entity.PackageInstallerEntity_;
import de.hopmann.msc.slave.installer.PackageInstaller;
import de.hopmann.msc.slave.installer.PackageInstaller.InstallerResult;
import de.hopmann.msc.slave.installer.PackageInstallerHolder;
import de.hopmann.msc.slave.service.ResolverBean.PackageResolved;
import de.hopmann.msc.slave.util.InstallationContext;

/**
 * Shared component managing package deployments.
 * 
 */
@ApplicationScoped
@Transactional
public class PackageInstallationBean {

	public class InstallationException extends Exception {

		private static final long serialVersionUID = 1L;

		private PackageInstallationEntity installationEntity;

		public InstallationException(String message,
				PackageInstallationEntity installationEntity) {
			super(message);
			this.installationEntity = installationEntity;
		}

		public InstallationException(String string, Exception e,
				PackageInstallationEntity installationEntity) {
			super(string, e);
			this.installationEntity = installationEntity;
		}

		public PackageInstallationEntity getInstallationEntity() {
			return installationEntity;
		}
	}

	public class DependencyInstallationException extends Exception {

		private static final long serialVersionUID = 1L;

		private PackageResolved packageResolved;

		public DependencyInstallationException(String message,
				PackageResolved packageResolved) {
			super(message);
			this.packageResolved = packageResolved;
		}

		public DependencyInstallationException(String string, Exception e,
				PackageResolved packageResolved) {
			super(string, e);
			this.packageResolved = packageResolved;
		}

		public PackageResolved getAffectedPackage() {
			return packageResolved;
		}
	}

	/**
	 * Threading facility for limiting concurrency in package deployments. Makes
	 * sure that scheduled deployments are reused acrossdifferent threads.
	 * 
	 */
	private class InstallationExecutor
			extends
			SimpleDispatchingExecutor<InstallationTaskInformation, PackageInstallationEntity> {

		public InstallationExecutor(ExecutorService executor) {
			super(executor);
		}

		@Override
		protected PackageInstallationEntity load(
				InstallationTaskInformation value) throws Exception {

			PathHolder packageInstallationLibraryPath = getPackageInstallationLibraryPath(value.installationEntity);
			value.context.registerPathHolder(packageInstallationLibraryPath);
			value.installationEntity
					.setLibraryPath(packageInstallationLibraryPath.getPath()); // Valid
																				// until
																				// context
																				// is
																				// closed

			java.nio.file.Path descrPath = packageInstallationLibraryPath
					.getPath().resolve(value.packageAccessor.getPackageName())
					.resolve("DESCRIPTION");

			if (!Files.exists(descrPath)) {
				// Rebuild since there is no DESCRIPTION file in package
				// installation of library

				log.info("Package " + value.packageAccessor.getPackageName()
						+ " requires rebuild");

				PathHolder sourceDirectory = value.packageAccessor
						.acquireSource().get();

				if (!Files
						.isDirectory(packageInstallationLibraryPath.getPath())) {
					Files.createDirectories(packageInstallationLibraryPath
							.getPath());
					// TODO check result/exceptions
				}

				// Get log path
				java.nio.file.Path installationLogPath = getPackageInstallationLogPath(value.installationEntity);
				if (!Files.exists(installationLogPath)) {
					Files.createDirectories(installationLogPath.getParent());
				}

				log.info("Installing package "
						+ value.packageAccessor.getPackageName());
				InstallerResult<InstallationResult> installerResult = value
						.getPackageInstaller().installPackage(
								sourceDirectory.getPath(),
								packageInstallationLibraryPath.getPath(),
								installationLogPath,
								value.context.getLibraryPaths());

				if (!installerResult.getOutputMessages().isEmpty()) {
					value.installationEntity.setOutputMessages(installerResult
							.getOutputMessages());
				}
				boolean isFailed = installerResult.getResult().isFailed();

				value.installationEntity.setFailed(isFailed);

				// TODO save result

				// Release source
				sourceDirectory.close();

				log.info("Finished installation of package "
						+ value.packageAccessor.getPackageName());

				if (isFailed) {
					throw new InstallationException("Installation failed",
							value.installationEntity);
				}
			}

			log.info("Returning installation of package "
					+ value.packageAccessor.getPackageName());
			return value.installationEntity;
		}

	}

	/**
	 * Denotes a currently scheduled package deployment
	 * 
	 */
	private static class InstallationTaskInformation {
		private PackageAccessor packageAccessor;
		// private Set<File> libPath;
		private PackageInstallerHolder packageInstallerHolder;
		private PackageInstallationEntity installationEntity;
		private InstallationContext context;

		public InstallationTaskInformation(PackageAccessor packageModel,
				PackageInstallerHolder packageInstallerHolder,
				// Set<File> libPath,
				PackageInstallationEntity installationEntity,
				InstallationContext context) {
			this.packageAccessor = packageModel;
			this.packageInstallerHolder = packageInstallerHolder;
			// this.libPath = libPath;
			this.installationEntity = installationEntity;
			this.context = context;
		}

		public PackageInstaller getPackageInstaller() {
			return packageInstallerHolder.getPackageInstaller();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof InstallationTaskInformation)) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			InstallationTaskInformation other = (InstallationTaskInformation) obj;
			return new EqualsBuilder()
					.append(installationEntity, other.installationEntity)
					.append(packageInstallerHolder,
							other.packageInstallerHolder).isEquals();
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(installationEntity)
					.append(packageInstallerHolder).toHashCode();
		}
	}

	@Inject
	private Logger log;

	@Resource
	private ManagedThreadFactory managedThreadFactory;;

	private InstallationExecutor installationExecutor;

	@Inject
	private EntityManager entityManager;

	private java.nio.file.Path libraryDirectoryBasePath;

	PackageInstallationBean() {

	}

	@Inject
	PackageInstallationBean(
			@Configuration(value = "libraryDirectoryBase", required = true) String libraryDirectoryBase) {
		this.libraryDirectoryBasePath = Paths.get(libraryDirectoryBase);
	}

	public Future<InstallationContext> acquireInstallation(
			PackageResolved packageResolved,
			PackageInstallerHolder packageInstallerHolder)
			throws PackageNotFoundException {
		// TODO exception, close context to rollback on error

		final InstallationContext context = new InstallationContext();

		final Future<PackageInstallationEntity> installationFuture = acquireInstallation(
				packageResolved, packageInstallerHolder, context);

		return new Future<InstallationContext>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				boolean cancelled = installationFuture
						.cancel(mayInterruptIfRunning);
				try {
					// Close installation context to release resources
					context.close();
				} catch (Exception e) {
					log.log(Level.WARNING,
							"Could not close installation context", e);
				}
				return cancelled;
			}

			@Override
			public InstallationContext get() throws InterruptedException,
					ExecutionException {
				context.setInstallation(installationFuture.get());
				return context;
			}

			@Override
			public InstallationContext get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
				context.setInstallation(installationFuture.get(timeout, unit));
				return context;
			}

			@Override
			public boolean isCancelled() {
				return installationFuture.isCancelled();
			}

			@Override
			public boolean isDone() {
				return installationFuture.isDone();
			}
		};

	}

	public Future<PackageInstallationEntity> acquireInstallation(
			PackageResolved packageResolved,// final Set<File> libPath,
			PackageInstallerHolder packageInstallerHolder,
			InstallationContext context) throws PackageNotFoundException {

		PackageInstallationEntity libraryPackage = context
				.getLibraryPackage(packageResolved.getPackageName());
		if (libraryPackage != null) {
			log.info("Returning package from context library: "
					+ libraryPackage.getPackageName());
			return FutureHelper.createImmediateFuture(libraryPackage);
		}

		final Set<PackageResolved> installationDependencies = packageResolved
				.getDependencies().getDependencies(DependencyType.CMD_INSTALL);

		// Exclude missing, but provided packages from dependencies
		List<String> corePackages = packageInstallerHolder
				.getCorePackageNames();
		Iterator<PackageResolved> dependenciesIterator = installationDependencies
				.iterator();
		while (dependenciesIterator.hasNext()) {
			// TODO XXX DRY
			PackageResolved nextDep = dependenciesIterator.next();
			if (corePackages.contains(nextDep.getPackageName())
					&& !nextDep.getPackageAccessor().isAvailable()) {
				// Package is provided and not available in resolved context ->
				// safe to exclude
				dependenciesIterator.remove();
				continue;
			}
			String depOsType = nextDep.getOSType();
			if (depOsType != null
					&& !depOsType.equals(packageInstallerHolder.getOsType())) {
				// package not supported by installer OS
				dependenciesIterator.remove();
				log.info("Dependency ignored because of os type " + nextDep);
			}
		}

		if (installationDependencies.isEmpty()) {
			// Package has no dependencies, directly return Future of
			// installation process
			log.info(packageResolved.getPackageName()
					+ " has no dependencies and Future of installation is returned");
			return getInstallation(packageResolved, null,
					packageInstallerHolder, context);
		} else {
			// Package has dependencies, collect instances of them to find
			// corresponding variant of this package

			Map<PackageResolved, Future<PackageInstallationEntity>> dependencyFutures = new HashMap<>(
					4); // TODO initial value

			// Trigger retrieval of installation for each dependency
			for (PackageResolved dependency : installationDependencies) {
				log.info(packageResolved.getPackageName()
						+ " acquiring Future installation for depending package "
						+ dependency.getPackageName());
				dependencyFutures.put(
						dependency,
						acquireInstallation(dependency, packageInstallerHolder,
								context));
			}

			// Getting results of Future installations
			Set<PackageInstallationEntity> dependencies = new HashSet<>(
					dependencyFutures.size());
			try {
				for (Entry<PackageResolved, Future<PackageInstallationEntity>> dependencyFutureEntry : dependencyFutures
						.entrySet()) {
					log.info(packageResolved.getPackageName()
							+ " waiting for Future installation result of package "
							+ dependencyFutureEntry.getKey().getPackageName());
					PackageInstallationEntity dependencyEntity = dependencyFutureEntry
							.getValue().get();
					context.addLibPackage(dependencyEntity);

					dependencies.add(dependencyEntity);
				}
			} catch (ExecutionException | InterruptedException e) {
				log.log(Level.SEVERE,
						"Installation of depending package failed", e);
				return FutureHelper
						.createExceptionFuture(new DependencyInstallationException(
								"Installation of a dependency failed", e,
								packageResolved));
			}

			return getInstallation(packageResolved, dependencies,
					packageInstallerHolder, context);
		}

	}

	private PackageInstallationEntity addInstallationEntity(
			PackageAccessor packageAccessor,
			Set<PackageInstallationEntity> requiredDependencies,
			PackageInstallerHolder packageInstallerHolder) {
		PackageInstallationEntity entity = new PackageInstallationEntity();

		entity.setPackageName(packageAccessor.getPackageName());
		entity.setPackageVersion(packageAccessor.getPackageVersion());
		entity.setSourceLocation(packageAccessor.getSourceLocation());
		entity.setSourceType(packageAccessor.getPackageSource().getSourceType());
		entity.setSourceVersion(packageAccessor.getSourceVersion());

		entity.addDependencies(requiredDependencies);
		entity.setPackageInstaller(new PackageInstallerEntity(
				packageInstallerHolder));

		entityManager.persist(entity);

		return entity;
	}

	/**
	 * @param packageModel
	 *            Package to install.
	 * @param requiredDependencies
	 *            Strong dependencies.
	 * @param libPath
	 *            Library path information required to install this package.
	 *            Contains all installation of transitive dependencies.
	 * @param packageInstallerHolder
	 * @throws PackageNotFoundException
	 */
	private Future<PackageInstallationEntity> getInstallation(
			final PackageResolved packageModel,
			Set<PackageInstallationEntity> requiredDependencies,
			// final Set<File> libPath,
			PackageInstallerHolder packageInstallerHolder,
			InstallationContext context) {

		if (requiredDependencies == null) {
			requiredDependencies = Collections.emptySet();
		}
		// TODO find critical dependencies

		PackageInstallationEntity installationEntity = getInstallationEntity(
				packageModel, requiredDependencies, packageInstallerHolder);
		if (installationEntity == null) {
			log.info("No cached installation of package "
					+ packageModel.getPackageName() + ", adding new one");
			installationEntity = addInstallationEntity(
					packageModel.getPackageAccessor(), requiredDependencies,
					packageInstallerHolder);
		}

		if (Boolean.TRUE.equals(installationEntity.isFailed())) {
			return FutureHelper
					.createExceptionFuture(new InstallationException(
							"Installation of package "
									+ installationEntity.getPackageName()
									+ " failed previously", installationEntity));
		} else {
			return installationExecutor.submit(new InstallationTaskInformation(
					packageModel.getPackageAccessor(), packageInstallerHolder,
					// libPath,
					installationEntity, context));
		}

		// TODO synchronization?
	}

	public PackageInstallationEntity getInstallationEntity(
			PackageResolved packageModel,
			Set<PackageInstallationEntity> requiredDependencies,
			PackageInstallerHolder packageInstallerHolder) {
		try {

			// TODO repository version, flavor, etc.

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<PackageInstallationEntity> query = cb
					.createQuery(PackageInstallationEntity.class);
			Root<PackageInstallationEntity> p = query
					.from(PackageInstallationEntity.class);
			Path<PackageInstallerEntity> pInstaller = p
					.get(PackageInstallationEntity_.packageInstaller);

			List<Predicate> predicates = new ArrayList<Predicate>();

			predicates.add(cb.equal(
					p.get(PackageInstallationEntity_.packageName),
					packageModel.getPackageName()));
			predicates.add(cb.equal(
					p.get(PackageInstallationEntity_.sourceVersion).get(
							Version_.versionNumber), packageModel
							.getPackageAccessor().getSourceVersion()
							.getVersionNumber()));

			predicates.add(cb.between(
					pInstaller.get(PackageInstallerEntity_.version).get(
							Version_.versionNumber),
					packageInstallerHolder.getDependencyMinVersion(),
					packageInstallerHolder.getDependencyMaxVersion()));

			// TODO flavor arch

			if (requiredDependencies.isEmpty()) {
				query.where(cb.and(predicates.toArray(new Predicate[] {})));
			} else {
				SetJoin<PackageInstallationEntity, PackageInstallationEntity> join = p
						.join(PackageInstallationEntity_.actualDependencies);

				predicates.add(join.in(requiredDependencies));

				// Expression<Set<PackageInstallationEntity>> pDependencies = p
				// .get(PackageInstallationEntity_.actualDependencies);
				// predicates.add(pDependencies.in(requiredDependencies));

				query.where(cb.and(predicates.toArray(new Predicate[] {})));
				Path<Long> pId = p.get(PackageInstallationEntity_.id);
				query.groupBy(pId);
				query.having(cb.ge(cb.count(pId), requiredDependencies.size()));
				query.distinct(true);
			}

			return entityManager.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public java.nio.file.Path getPackageInstallationLogPath(
			PackageInstallationEntity installationEntity) {

		java.nio.file.Path installationPath = installationEntity
				.getLibraryPath()
				.resolve(installationEntity.getPackageName() + "")
				.resolve("install.log");

		return installationPath;

	}

	public java.nio.file.Path getPackageCheckLogPath(
			PackageInstallationEntity installationEntity) {

		java.nio.file.Path installationPath = installationEntity
				.getLibraryPath()
				.resolve(installationEntity.getPackageName() + "")
				.resolve("check.log");

		return installationPath;

	}

	public PathHolder getPackageInstallationLibraryPath(
			PackageInstallationEntity installationEntity) {
		java.nio.file.Path installationPath = libraryDirectoryBasePath
				.resolve(installationEntity.getId() + "");

		return PathHolder.ofPath(installationPath);
	}

	@PostConstruct
	private void init() {
		installationExecutor = new InstallationExecutor(
				Executors.newFixedThreadPool(3, managedThreadFactory)); // TODO
																		// adjust
	}
}
