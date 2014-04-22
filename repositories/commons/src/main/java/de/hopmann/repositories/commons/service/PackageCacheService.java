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
package de.hopmann.repositories.commons.service;

import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import de.hopmann.msc.commons.exception.PackageNotFoundException;
import de.hopmann.msc.commons.model.PackageSource.PackageAccessor;
import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.commons.model.Version_;
import de.hopmann.repositories.commons.entity.DependencyEntity;
import de.hopmann.repositories.commons.entity.PackageEntity;
import de.hopmann.repositories.commons.entity.PackageEntity_;

@Transactional
// TODO merge to CRAN repo
public abstract class PackageCacheService<T extends PackageEntity> {

	@Inject
	protected Logger log;

	@Inject
	protected EntityManager entityManager;

	protected Class<T> packageEntityClass;

	@Inject
	public PackageCacheService() {
		packageEntityClass = getPackageEntityClass();
	}

	protected abstract Class<T> getPackageEntityClass();

	protected void setPackageEntityClass(Class<T> packageEntityClass) {
		this.packageEntityClass = packageEntityClass;
	}

	public T getPackageByName(String pkgName) throws PackageNotFoundException {
		try {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<T> query = cb.createQuery(packageEntityClass);
			Root<T> p = query.from(packageEntityClass);
			query.where(cb.equal(p.get(PackageEntity_.name), pkgName));

			return entityManager.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			throw new PackageNotFoundException("Package " + pkgName
					+ " not found");
		}
	}

	public T getPackageByPackageVersion(String packageName,
			Version packageVersion) throws PackageNotFoundException {
		try {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<T> query = cb.createQuery(packageEntityClass);
			Root<T> p = query.from(packageEntityClass);
			query.where(cb.and(
					//
					cb.equal(p.get(PackageEntity_.name), packageName),
					cb.equal(
							p.get(PackageEntity_.version).get(
									Version_.versionNumber),
							packageVersion.getVersionNumber())));

			return entityManager.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			throw new PackageNotFoundException("Package " + packageName
					+ " not found");
		}
	}

	public T getPackageByAccessor(PackageAccessor packageAccessor)
			throws PackageNotFoundException {
		// TODO sync VersionModel/-Entity

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> query = cb.createQuery(packageEntityClass);
		Root<T> p = query.from(packageEntityClass);
		query.where(cb.and(
				cb.equal(p.get(PackageEntity_.name),
						packageAccessor.getPackageName()),
				cb.equal(p.get(PackageEntity_.version),
						packageAccessor.getSourceVersion())));

		List<T> resultList = entityManager.createQuery(query).getResultList();

		// TODO check uniqueness
		if (resultList.isEmpty()) {
			throw new PackageNotFoundException("Package "
					+ packageAccessor.getPackageName() + " not found");
		} else {
			return resultList.get(0);
		}
	}

	public void addPackage(T newEntity) {
		entityManager.persist(newEntity);
	}

	public T getPackageLatestPackageVersion(String pkgName)
			throws PackageNotFoundException {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> query = cb.createQuery(packageEntityClass);
		Root<T> p = query.from(packageEntityClass);
		query.where(cb.equal(p.get(PackageEntity_.name), pkgName));
		query.orderBy(cb.desc(p.get(PackageEntity_.version).get(
				Version_.versionNumber)));

		List<T> resultList = entityManager.createQuery(query).setMaxResults(1)
				.getResultList();
		if (resultList.isEmpty()) {
			throw new PackageNotFoundException("Package " + pkgName
					+ " not found");
		} else {
			return resultList.get(0);
		}

	}

	public List<DependencyEntity> getDeclaredDependenciesByAccessor(
			PackageAccessor packageAccessor) throws PackageNotFoundException {
		T packageEntity = getPackageByAccessor(packageAccessor);
		return packageEntity.getDependencies();
	}

	// public boolean isAvailable(String packageName, Version sourceVersion) {
	// CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	// CriteriaQuery<PackageEntity> query = cb
	// .createQuery(PackageEntity.class);
	// Root<PackageEntity> p = query.from(PackageEntity.class);
	// query.where(cb.and(
	// //
	// cb.equal(p.get(PackageEntity_.name), packageName),
	// cb.equal(
	// p.get(PackageEntity_.version).get(
	// Version_.versionNumber),
	// sourceVersion.getVersionNumber())));
	//
	// List<PackageEntity> resultList = entityManager.createQuery(query)
	// .getResultList();
	// return !resultList.isEmpty();
	// }

	public void removePackage(T packageEntity) {
		entityManager.remove(packageEntity);
	}

}
