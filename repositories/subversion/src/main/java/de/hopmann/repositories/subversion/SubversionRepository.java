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
package de.hopmann.repositories.subversion;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import de.hopmann.msc.commons.model.PackageLocation;
import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.commons.qualifier.Configuration;

@ApplicationScoped
public class SubversionRepository extends PackageLocation {
	private static int NEXT_WORKING_DIR_ID = 0;

	private static class AccessorCacheItem {
		private String location;
		private Long revision;

		public AccessorCacheItem(String location, Long revision) {
			this.location = location;
			this.revision = revision;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof AccessorCacheItem)) {
				return false;
			}
			AccessorCacheItem other = (AccessorCacheItem) obj;
			return new EqualsBuilder().append(location, other.location)
					.append(revision, other.revision).isEquals();
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(location).append(revision)
					.toHashCode();
		}

	}

	private Map<AccessorCacheItem, SubversionPackageAccessor> accessorCacheMap = new HashMap<>();

	@Inject
	private Logger log;

	@Configuration(value = "subversionCacheDirectory", required = true)
	@Inject
	private String subversionCacheDirectory;

	@Resource
	private ManagedThreadFactory managedThreadFactory;

	@Inject
	SubversionRepository() {

	}

	@Override
	public String getSourceType() {
		return "Subversion"; // TODO annotation
	}

	@PostConstruct
	private void init() {
		// sourceExecutor = new SourceExecutor(
		// (ThreadPoolExecutor) Executors.newFixedThreadPool(2,
		// managedThreadFactory));
	}

	@Override
	public PackageAccessor getRepositoryAccessor(String location,
			Version sourceVersion) {

		Long revisionNumber = null;
		if (sourceVersion != null) {
			if (sourceVersion.getVersionString() != null
					&& !sourceVersion.getVersionString().isEmpty()) {
				revisionNumber = Long.parseLong(sourceVersion
						.getVersionString());
			} else if (sourceVersion.getVersionNumber() != null) {
				revisionNumber = sourceVersion.getVersionNumber().longValue();
			}
		}
		AccessorCacheItem cacheItem = new AccessorCacheItem(location,
				revisionNumber);

		SubversionPackageAccessor packageAccessor = accessorCacheMap
				.get(cacheItem);
		if (packageAccessor == null) {

			int workingDirId = NEXT_WORKING_DIR_ID++;

			packageAccessor = new SubversionPackageAccessor(location,
					revisionNumber, Paths.get(subversionCacheDirectory,
							workingDirId + ""), this);
			accessorCacheMap.put(cacheItem, packageAccessor);
		}

		return packageAccessor;
	}
}
