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
package de.hopmann.msc.commons.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import de.hopmann.msc.commons.model.DependencyType;

/**
 * Helper class to perform typical tasks related to R package
 * dependency-structures.
 * 
 * @param <T>
 */
public class DependencyMapHelper<T> {

	public interface DependencyMapCallable<T> extends
			Callable<Map<DependencyType, Set<T>>> {

	}

	private Map<DependencyType, Set<T>> cachedDependencyMap;
	private Callable<Map<DependencyType, Set<T>>> dependencyMapCallable;

	public DependencyMapHelper(
			Callable<Map<DependencyType, Set<T>>> dependencyMapCallable) {
		this.dependencyMapCallable = dependencyMapCallable;
	}

	public Set<T> getDependencies(DependencyType dependencyType) {
		Set<T> returnSet = getDependenciesMap().get(dependencyType);
		if (returnSet == null)
			return Collections.<T> emptySet();
		else {
			return returnSet;
		}
	}

	public Set<T> getDependencies(EnumSet<DependencyType> dependencyTypes) {
		Set<T> resultSet = new HashSet<T>();
		for (DependencyType depType : dependencyTypes) {
			resultSet.addAll(getDependencies(depType));
		}
		return resultSet;
	}

	public Map<DependencyType, Set<T>> getDependenciesMap() {
		if (cachedDependencyMap == null) {
			try {
				cachedDependencyMap = dependencyMapCallable.call();
			} catch (Exception e) {
				// TODO
				e.printStackTrace();
				throw new RuntimeException(
						"Could not lazily load dependency information", e);
			}
		}
		return cachedDependencyMap;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DependencyMapHelper)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		DependencyMapHelper<?> other = (DependencyMapHelper<?>) obj;
		return new EqualsBuilder().append(getDependenciesMap(),
				other.getDependenciesMap()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(getDependenciesMap()).toHashCode();
	}
}
