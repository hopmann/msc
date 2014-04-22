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
package de.hopmann.repositories.cran;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import de.hopmann.msc.commons.exception.PackageNotFoundException;
import de.hopmann.msc.commons.exception.PackageResolvingException;
import de.hopmann.msc.commons.exception.VersionFormatException;
import de.hopmann.msc.commons.model.DependencyInfo;
import de.hopmann.msc.commons.model.DependencyType;
import de.hopmann.msc.commons.model.PackageRepository;
import de.hopmann.msc.commons.model.PackageSource;
import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.commons.qualifier.CRAN;
import de.hopmann.msc.commons.qualifier.Configuration;
import de.hopmann.msc.commons.util.DispatchingExecutor;
import de.hopmann.repositories.commons.entity.DependencyEntity;
import de.hopmann.repositories.cran.service.CRANPackageListingService;

/**
 * Representing a CRAN-like repository. Provides efficient access to current
 * metadata and adapts to package archive contents.
 * 
 */
@ApplicationScoped
@CRAN
public class CRANRepository extends PackageRepository {

	private class SourceCacheItem {
		private Path path;

		private Set<SourcePathHolder> lockHolder = new HashSet<>(0);

		// TODO use for cleanup and to release unused sources
	}

	private class SourceExecutor extends
			DispatchingExecutor<PackageAccessor, SourceCacheItem, PathHolder> {

		private Map<PackageAccessor, SourceCacheItem> sourceCacheMap = new HashMap<>();

		private CloseableHttpClient httpclient = HttpClients.createDefault();
		private String SOURCE_EXTENSION = ".tar.gz";

		public SourceExecutor(ExecutorService executor) {
			super(executor);
		}

		@Override
		protected SourcePathHolder dispatch(SourceCacheItem cacheItem)
				throws Exception {
			return new SourcePathHolder(cacheItem);
		}

		private void downloadSource(PackageAccessor accessor,
				Path destinationDir, boolean archive) throws IOException,
				FileNotFoundException {
			HttpGet getPackageArchive = null;

			// TODO multiple repos
			String packageUri;
			if (!archive) {
				packageUri = sourceRepositories.get(0) + "src/contrib/"
						+ accessor.getPackageName() + "_"
						+ accessor.getSourceVersion().getVersionString()
						+ SOURCE_EXTENSION;
			} else {
				packageUri = sourceRepositories.get(0) + "src/contrib/Archive/"
						+ accessor.getPackageName() + "/"
						+ accessor.getPackageName() + "_"
						+ accessor.getSourceVersion().getVersionString()
						+ SOURCE_EXTENSION;
			}

			getPackageArchive = new HttpGet(packageUri);

			CloseableHttpResponse httpResponse = httpclient
					.execute(getPackageArchive);

			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				IOUtils.copy(httpResponse.getEntity().getContent(),
						FileUtils.openOutputStream(destinationDir.toFile()));
			} else {
				httpResponse.close();
				throw new FileNotFoundException("Package "
						+ accessor.getPackageName() + ", "
						+ accessor.getSourceVersion()
						+ " not found in repository");
			}
			httpResponse.close();
		}

		@Override
		protected SourceCacheItem load(PackageAccessor accessor)
				throws IOException {
			SourceCacheItem cacheItem = sourceCacheMap.get(accessor);
			if (cacheItem == null) {
				cacheItem = new SourceCacheItem();

				Path sourcePath = Paths.get(sourceCacheDirectory,
						accessor.getPackageName()
								+ "_"
								+ accessor.getSourceVersion()
										.getVersionString() + SOURCE_EXTENSION);

				if (!Files.exists(sourcePath)) {
					boolean archive = false;
					try {
						archive = packageService.getPackageByAccessor(accessor)
								.isArchived();
					} catch (PackageNotFoundException e1) {
						// Not found, maybe new, so use default
					}
					if (!archive) {
						try {
							log.info("CRAN: Downloading package " + accessor);
							downloadSource(accessor, sourcePath, false);
						} catch (FileNotFoundException e) {
							log.log(Level.INFO,
									"Package not found, now searching in source archive",
									e);
							packageService.setArchived(accessor);
							downloadSource(accessor, sourcePath, true);
						}
					} else {
						try {
							log.info("CRAN: Downloading package " + accessor
									+ " from archive");
							downloadSource(accessor, sourcePath, true);
						} catch (FileNotFoundException e) {
							log.log(Level.WARNING,
									"Package not found on CRAN, althoug in PACKAGES listing",
									e);
							packageService.setMissing(accessor);
						}
					}
				} else {
					log.info("CRAN: Package source exists " + accessor);
				}

				cacheItem.path = sourcePath;

				sourceCacheMap.put(accessor, cacheItem);
			}

			return cacheItem;
		}

	}

	private class SourcePathHolder extends PathHolder {

		private SourceCacheItem cacheItem;

		public SourcePathHolder(SourceCacheItem cacheItem) {
			this.cacheItem = cacheItem;
			cacheItem.lockHolder.add(this);
		}

		@Override
		public void close() {
			cacheItem.lockHolder.remove(this);
			// TODO sync and removal
		}

		@Override
		public Path getPath() {
			return cacheItem.path;
		}

	}

	private class CRANPackageAccessor implements PackageAccessor {

		private Map<DependencyType, Set<DependencyInfo>> dependenciesMap;
		private Version packageSourceVersion;
		private String packageName;

		public CRANPackageAccessor(String packageName,
				Version packageSourceVersion) throws VersionFormatException {
			this.packageName = packageName;
			this.packageSourceVersion = packageSourceVersion;
		}

		@Override
		public Map<DependencyType, Set<DependencyInfo>> getDeclaredDependenciesMap()
				throws PackageResolvingException {
			if (dependenciesMap == null) {
				dependenciesMap = new HashMap<>();
				List<DependencyEntity> declaredDependencies;
				try {
					declaredDependencies = packageService.getPackageByAccessor(
							this).getDependencies();
				} catch (PackageNotFoundException e) {
					throw new PackageResolvingException(e);
				}

				for (DependencyEntity dependencyEntity : declaredDependencies) {
					DependencyInfo dependencyInfo = new DependencyInfo(
							dependencyEntity.getDependingPackage(), null);// TODO
																			// version
																			// constr

					Set<DependencyInfo> dependencies = dependenciesMap
							.get(dependencyEntity.getType());
					if (dependencies == null) {
						dependencies = new HashSet<DependencyInfo>();
						dependenciesMap.put(dependencyEntity.getType(),
								dependencies);
					}
					dependencies.add(dependencyInfo);
				}
			}
			return dependenciesMap;
		};

		@Override
		public Future<PathHolder> acquireSource() {
			return sourceExecutor.submit(this);
		}

		@Override
		public String getPackageName() {
			return packageName;
		}

		@Override
		public Version getPackageVersion() throws PackageResolvingException {
			return getSourceVersion();
		}

		@Override
		public String getSourceLocation() {
			return null;
		}

		@Override
		public Version getSourceVersion() throws PackageResolvingException {
			if (packageSourceVersion == null) {
				try {
					packageSourceVersion = packageService
							.getPackageLatestPackageVersion(getPackageName())
							.getSourceVersion();
				} catch (PackageNotFoundException e) {
					// TODO download from archive
					throw new PackageResolvingException(e);
				}
			}
			return packageSourceVersion;
		}

		@Override
		public PackageSource getPackageSource() {
			return CRANRepository.this;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof CRANPackageAccessor)) {
				return false;
			}
			CRANPackageAccessor other = (CRANPackageAccessor) obj;
			return new EqualsBuilder()
					.append(getSourceVersion(), other.getSourceVersion())
					.append(getPackageName(), other.getPackageName())
					.isEquals();
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(getSourceVersion())
					.append(getPackageName()).toHashCode();
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this).append("packageName", packageName)
					.append("versionString", packageSourceVersion).toString();
		}

		@Override
		public boolean isAvailable() {
			try {
				return packageService.isAvailable(getPackageName(),
						getSourceVersion()); // TODO cache entity here
			} catch (PackageResolvingException e) {
				return false;
			}
		}

		@Override
		public String getOSType() {
			try {
				return packageService.getPackageByAccessor(this).getOsType();
			} catch (PackageNotFoundException e) {
				throw new PackageResolvingException(e);
			}
		}

	};

	private List<String> sourceRepositories = new ArrayList<String>();

	@Inject
	private Logger log;

	@Configuration(value = "sourceCacheDirectory", required = true)
	@Inject
	private String sourceCacheDirectory;

	private CRANPackageListingService packageService;

	@Resource
	private ManagedThreadFactory managedThreadFactory;

	private SourceExecutor sourceExecutor;

	CRANRepository() {

	}

	@Inject
	CRANRepository(CRANPackageListingService packageService) {
		sourceRepositories.add("http://cran.r-project.org/");
		this.packageService = packageService;
	}

	@Override
	public String getSourceType() {
		return "CRAN"; // TODO annotation
	}

	@PostConstruct
	private void init() {
		sourceExecutor = new SourceExecutor(
				(ThreadPoolExecutor) Executors.newFixedThreadPool(2,
						managedThreadFactory));
	}

	@Override
	public PackageAccessor getRepositoryAccessor(String packageName,
			String sourceLocation, Version sourceVersion)
			throws PackageResolvingException {
		try {
			return new CRANPackageAccessor(packageName, sourceVersion);
		} catch (VersionFormatException e) {
			throw new PackageResolvingException(e);
		}
	}

}
