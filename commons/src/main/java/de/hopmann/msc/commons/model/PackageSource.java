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
package de.hopmann.msc.commons.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public abstract class PackageSource {
	public interface PathCloseable extends AutoCloseable {
		@Override
		public void close();
	}

	/**
	 * Class wrapping a {@link Path} to allow individual locking. Each returned
	 * instance must be specific for the corresponding caller. The caller has to
	 * release this specific instance eventually by invoking the
	 * {@link PathHolder#close()} method.
	 * 
	 */
	public static abstract class PathHolder implements PathCloseable {
		// XXX interface?
		public abstract Path getPath();

		public static PathHolder ofPath(final Path path) {
			return new PathHolder() {

				@Override
				public void close() {

				}

				@Override
				public Path getPath() {
					return path;
				}
			};
		}

		public static PathHolder ofPath(String libraryPath) {
			if (libraryPath == null) {
				return ofPath((Path) null);
			} else {
				return ofPath(Paths.get(libraryPath));
			}
		}
	}

	public interface PackageAccessor {
		// TODO async
		Map<DependencyType, Set<DependencyInfo>> getDeclaredDependenciesMap();

		// TODO cancellation
		Future<PathHolder> acquireSource();

		String getPackageName();

		Version getPackageVersion();

		String getSourceLocation();

		Version getSourceVersion();

		PackageSource getPackageSource();
			
		String getOSType();

		boolean isAvailable();
	}

	public abstract String getSourceType();

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PackageSource)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		PackageSource other = (PackageSource) obj;
		return new EqualsBuilder().append(getSourceType(),
				other.getSourceType()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(getSourceType()).toHashCode();
	}
}
