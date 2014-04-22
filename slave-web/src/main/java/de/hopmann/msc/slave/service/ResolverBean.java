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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import de.hopmann.msc.commons.exception.PackageNotFoundException;
import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.messages.CheckTaskMessage;
import de.hopmann.msc.commons.messages.PackageInstallerMessage;
import de.hopmann.msc.commons.messages.PackageMessage;
import de.hopmann.msc.commons.messages.PackageResultMessage;
import de.hopmann.msc.commons.model.DependencyInfo;
import de.hopmann.msc.commons.model.DependencyType;
import de.hopmann.msc.commons.model.InstallationResult;
import de.hopmann.msc.commons.model.PackageLocation;
import de.hopmann.msc.commons.model.PackageRepository;
import de.hopmann.msc.commons.model.PackageSource;
import de.hopmann.msc.commons.model.PackageSource.PackageAccessor;
import de.hopmann.msc.commons.model.SourceIdentifier;
import de.hopmann.msc.commons.model.SourceIdentifier.SourceRepositoryIdentifier;
import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.commons.service.IRepositoryService;
import de.hopmann.msc.commons.util.DependencyMapHelper;
import de.hopmann.msc.commons.util.DependencyMapHelper.DependencyMapCallable;
import de.hopmann.msc.commons.util.VersionHelper;
import de.hopmann.msc.slave.entity.PackageInstallationEntity;
import de.hopmann.msc.slave.installer.PackageInstallerHolder;

/**
 * This class translates between business objects and wrapper objects for
 * messaging purposes. Conveys abstract tasks and definitions into the actual
 * data model.
 * 
 */
@ApplicationScoped
public class ResolverBean {

	public interface CheckTaskHolder {
		PackageInstallerHolder getPackageInstallerHolder();

		PackageResolved getPackageModel();
	}

	public class PackageResolved {
		private PackageAccessor packageAccessorResolved;
		private final ResolverContext resolverContext;

		private DependencyMapHelper<DependencyInfo> declaredDependencies = new DependencyMapHelper<DependencyInfo>(
				new DependencyMapCallable<DependencyInfo>() {
					@Override
					public Map<DependencyType, Set<DependencyInfo>> call()
							throws Exception {
						return getPackageAccessor()
								.getDeclaredDependenciesMap();
					}
				});

		private DependencyMapHelper<PackageResolved> actualDependencies = new DependencyMapHelper<PackageResolved>(
				new DependencyMapCallable<PackageResolved>() {
					@Override
					public Map<DependencyType, Set<PackageResolved>> call()
							throws Exception {

						Map<DependencyType, Set<PackageResolved>> resultMap = new HashMap<>();

						for (Entry<DependencyType, Set<DependencyInfo>> entry : declaredDependencies
								.getDependenciesMap().entrySet()) {

							Set<PackageResolved> resultSet = new HashSet<>();

							for (DependencyInfo dependency : entry.getValue()) {
								// Iterate over all declared dependencies

								// Look for already resolved dependencies
								PackageResolved contextDependency = resolverContext
										.getDependency(dependency
												.getPackageName());

								if (contextDependency != null) {
									// TODO versionConstraint warning

									resultSet.add(contextDependency);
								} else {
									resultSet.add(new PackageResolved(
											dependency, resolverContext));
								}
							}
							resultMap.put(entry.getKey(), resultSet);
						}

						return resultMap;
					}
				});

		private String packageNameResolved;
		private PackageMessage packageMessage;

		public PackageResolved(DependencyInfo dependency,
				ResolverContext resolverContext) {
			this.resolverContext = resolverContext;
			try {
				packageAccessorResolved = resolverContext
						.getDefaultRepository().getRepositoryAccessor(
								dependency.getPackageName(),
								Version.fromRVersion(dependency
										.getPackageVersion()));
			} catch (PackageNotFoundException | VersionFormatException e) {
				throw new IllegalArgumentException(e);
			}
			resolverContext.registerDependency(this);
		}

		PackageResolved(PackageMessage packageMessage,
				ResolverContext resolverContext) {
			this.packageMessage = packageMessage;
			this.resolverContext = resolverContext;
			// XXX osType only identifiable by failed installation
			resolverContext.registerDependency(this);
		}

		public String getOSType() {
			return getPackageAccessor().getOSType();
		}

		public PackageAccessor getPackageAccessor() {
			if (packageAccessorResolved == null) {
				SourceIdentifier sourceIdentifier = packageMessage
						.getSourceDescription();
				if (sourceIdentifier == null
						|| sourceIdentifier instanceof SourceRepositoryIdentifier) {
					packageAccessorResolved = resolveAccessor(
							(SourceRepositoryIdentifier) sourceIdentifier,
							resolverContext);
				} else {
					packageAccessorResolved = resolveAccessor(sourceIdentifier,
							resolverContext);
				}

			}

			return packageAccessorResolved;
		}

		public String getPackageName() {
			if (packageNameResolved == null) {
				packageNameResolved = getPackageAccessor().getPackageName();
			}
			return packageNameResolved;
		}

		private PackageAccessor resolveAccessor(
				SourceIdentifier locationIdentifier,
				ResolverContext resolverContext) {
			PackageLocation packageLocation = resolveDescription(locationIdentifier);

			PackageAccessor packageAccessor = packageLocation
					.getRepositoryAccessor(
							locationIdentifier.getSourceLocation(),
							locationIdentifier.getSourceVersion());
			// TODO version string or number?

			return packageAccessor;
		}

		private PackageAccessor resolveAccessor(
				SourceRepositoryIdentifier repositoryIdentifier,
				ResolverContext resolverContext) {

			if (packageMessage.getName() != null) {
				if (repositoryIdentifier != null) {
					PackageRepository packageRepository = resolveDescription(repositoryIdentifier);
					try {
						return packageRepository.getRepositoryAccessor(
								packageMessage.getName(),
								repositoryIdentifier.getSourceLocation(),
								repositoryIdentifier.getSourceVersion());
					} catch (PackageNotFoundException e) {
						throw new IllegalArgumentException(e);
					}
				} else if (resolverContext.getDefaultRepository() != null) {
					DefaultRepositoryHolder defaultRepository = resolverContext
							.getDefaultRepository();
					try {
						return defaultRepository.getRepositoryAccessor(
								packageMessage.getName(), null);
					} catch (PackageNotFoundException e) {
						throw new IllegalArgumentException(e);
					}
				} else {
					throw new IllegalArgumentException("No repository set");
				}
			} else {
				throw new IllegalArgumentException("No name set");
			}

		}

		public DependencyMapHelper<PackageResolved> getDependencies() {
			return actualDependencies;
		}

	}

	private class DefaultRepositoryHolder {

		PackageRepository defaultPackageRepository;
		String defaultLocation;

		public DefaultRepositoryHolder(String defaultLocation,
				PackageRepository defaultPackageRepository) {
			this.defaultLocation = defaultLocation;
			this.defaultPackageRepository = defaultPackageRepository;
		}

		public PackageAccessor getRepositoryAccessor(String packageName,
				Version sourceVersion) throws PackageNotFoundException {
			return defaultPackageRepository.getRepositoryAccessor(packageName,
					defaultLocation, sourceVersion);
		}

	}

	interface ResolverContext {

		/**
		 * Returns the {@link DefaultRepositoryHolder} to use as default setting
		 * 
		 * @return
		 */
		DefaultRepositoryHolder getDefaultRepository();

		/**
		 * Returns a {@link PackageDescription} which is already resolved in
		 * this context by its name
		 * 
		 * @param name
		 *            Package name
		 * @return
		 */
		PackageResolved getDependency(String name);

		/**
		 * Registers a resolved {@link PackageDescription} in this context to be
		 * reused
		 * 
		 * @param packageResolved
		 */
		void registerDependency(PackageResolved packageResolved);
	}

	private IRepositoryService repositoryService;

	private PackageInstallerBean packageInstallerBean;

	ResolverBean() {
		// TODO Auto-generated constructor stub
	}

	@Inject
	ResolverBean(IRepositoryService repositoryService,
			PackageInstallerBean packageInstallerBean) {
		this.repositoryService = repositoryService;
		this.packageInstallerBean = packageInstallerBean;
	}

	public CheckTaskHolder resolveDescription(
			final CheckTaskMessage checkTaskMessage) {

		final DefaultRepositoryHolder defaultRepository;

		if (checkTaskMessage.getDefaultRepository() != null) {
			defaultRepository = new DefaultRepositoryHolder(checkTaskMessage
					.getDefaultRepository().getSourceLocation(),
					resolveDescription(checkTaskMessage.getDefaultRepository()));
		} else {
			defaultRepository = null;
		}

		// Create context based on information of supplied CheckTaskDescription
		final ResolverContext resolverContext = new ResolverContext() {

			Map<String, PackageResolved> packageCache = new HashMap<>();

			@Override
			public DefaultRepositoryHolder getDefaultRepository() {
				return defaultRepository;
			}

			@Override
			public PackageResolved getDependency(String name) {
				return packageCache.get(name);
			}

			@Override
			public void registerDependency(PackageResolved packageModel) {
				// TODO async?
				packageCache.put(packageModel.getPackageName(), packageModel);
			}

		};

		if (checkTaskMessage.getDependencies() != null) {
			for (PackageMessage packageMessage : checkTaskMessage
					.getDependencies()) {
				resolveDescription(packageMessage, resolverContext);
			}
		}
		try {
			return new CheckTaskHolder() {
				PackageResolved mPackageModel = resolveDescription(
						checkTaskMessage.getPackageDescription(),
						resolverContext);

				PackageInstallerHolder mPackageInstallerHolder = resolveDescription(checkTaskMessage
						.getPackageInstaller());

				@Override
				public PackageInstallerHolder getPackageInstallerHolder() {
					return mPackageInstallerHolder;
				}

				@Override
				public PackageResolved getPackageModel() {
					return mPackageModel;
				}
			};
		} catch (VersionFormatException e) {
			// TODO
			e.printStackTrace();
			return null;
		}
	}

	private PackageLocation resolveDescription(
			SourceIdentifier locationIdentifier) {

		PackageSource packageSource = repositoryService
				.getPackageSource(locationIdentifier.getSourceType());
		if (packageSource instanceof PackageLocation) {
			return (PackageLocation) packageSource;
		}

		throw new IllegalArgumentException("Package source not found, "
				+ locationIdentifier.getSourceType());
	}

	protected PackageInstallerHolder resolveDescription(
			PackageInstallerMessage packageInstallerMessage)
			throws VersionFormatException {
		return packageInstallerBean
				.getPackageInstaller(packageInstallerMessage);
	}

	private PackageResolved resolveDescription(PackageMessage packageMessage,
			ResolverContext resolverContext) {
		return new PackageResolved(packageMessage, resolverContext);
	}

	public PackageRepository resolveDescription(
			SourceRepositoryIdentifier sourceMessage) {
		PackageSource packageSource = repositoryService
				.getPackageSource(sourceMessage.getSourceType());
		if (packageSource instanceof PackageRepository) {
			return (PackageRepository) packageSource;
		}

		throw new IllegalArgumentException("Package repository not found, "
				+ sourceMessage.getSourceType());
	}

	public PackageResultMessage toMessage(
			PackageInstallationEntity installationEntity) {

		PackageResultMessage packageMessage = new PackageResultMessage();
		packageMessage.setName(installationEntity.getPackageName());
		packageMessage
				.setPackageVersion(installationEntity.getPackageVersion());

		packageMessage.setSourceVersion(installationEntity.getSourceVersion());
		packageMessage.setSourceType(installationEntity.getSourceType());
		packageMessage
				.setSourceLocation(installationEntity.getSourceLocation());

		packageMessage.setInstallationResult(new InstallationResult(
				Boolean.TRUE.equals(installationEntity.isFailed())));

		packageMessage
				.setOutputMessages(installationEntity.getOutputMessages());

		return packageMessage;
	}

	public PackageInstallerMessage toMessage(
			PackageInstallerHolder installerHolder) {
		PackageInstallerMessage installerMessage = new PackageInstallerMessage(
				VersionHelper.getVersionString(installerHolder.getVersion()));

		installerMessage.setArchitecture(installerHolder.getArchitecture());
		installerMessage.setFlavor(installerHolder.getFlavor());

		return installerMessage;
	}

	public PackageResultMessage toMessage(PackageAccessor packageAccessor) {
		PackageResultMessage packageMessage = new PackageResultMessage();
		packageMessage.setName(packageAccessor.getPackageName());
		packageMessage.setPackageVersion(packageAccessor.getPackageVersion());

		packageMessage.setSourceVersion(packageAccessor.getSourceVersion());
		packageMessage.setSourceType(packageAccessor.getPackageSource()
				.getSourceType());
		packageMessage.setSourceLocation(packageAccessor.getSourceLocation());

		return packageMessage;
	}

}
