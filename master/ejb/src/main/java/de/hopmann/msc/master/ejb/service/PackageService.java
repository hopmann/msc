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
package de.hopmann.msc.master.ejb.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.Transactional;

import de.hopmann.msc.commons.messages.CheckResultMessage;
import de.hopmann.msc.commons.messages.PackageResultMessage;
import de.hopmann.msc.commons.model.CheckResult;
import de.hopmann.msc.commons.model.InstallationResult;
import de.hopmann.msc.master.ejb.entity.PackageContext;
import de.hopmann.msc.master.ejb.entity.PackageContext_;
import de.hopmann.msc.master.ejb.entity.PackageResult;
import de.hopmann.msc.master.ejb.entity.PackageResult.PackageResultType;
import de.hopmann.msc.master.ejb.entity.PackageResult_;
import de.hopmann.msc.master.ejb.entity.PackageSource;
import de.hopmann.msc.master.ejb.entity.PackageSource_;

/**
 * Provides access to package context contents
 * 
 */
@ApplicationScoped
@Transactional
public class PackageService implements Serializable {

	private static final long serialVersionUID = 1L;

	@PersistenceContext(unitName = "master")
	private EntityManager entityManager;

	@Inject
	private Logger log;

	public PackageService() {

	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void addCheckResult(CheckResultMessage checkResultMessage) {

		Long contextIdRef = checkResultMessage.getContextIdRef();
		if (contextIdRef == null) {
			log.info("Check result message invalid, no context id");
			return;
		}

		PackageContext contextEntity = getPackageRepositoryById(contextIdRef);
		if (contextEntity == null) {
			log.info("Check result referencing non-exisiting context");
			return;
		}

		PackageResultMessage checkPackage = checkResultMessage
				.getPackageDescription();
		Set<PackageResultMessage> dependencies = checkResultMessage
				.getInstallationDependencies();
		// TODO installation or check dependencies

		Long maxRevisionAll = null;
		boolean newContext = false; // Indicates if any dependency is newer than
									// the known ones
		Set<PackageResultMessage> checkDependencies = new HashSet<>(
				dependencies);
		checkDependencies.add(checkPackage);

		Set<PackageResult> packageResults = new HashSet<>(dependencies.size());
		PackageSource packageSource = getPackageSource(checkPackage,
				contextEntity);
		PackageResult previousPackageInstallation = null;
		for (PackageResultMessage dependency : checkDependencies) {

			PackageSource depPackageSource = getPackageSource(dependency,
					contextEntity);

			long maxRevisionDep = depPackageSource.getMaxRevisionNumber();
			// TODO source type/location

			PackageResult maxPackageInstallation = getPackageInstallation(
					depPackageSource, maxRevisionDep);
			if (maxRevisionAll == null || maxRevisionDep > maxRevisionAll) {
				if (maxRevisionAll != null) {
					newContext = true;
				}
				maxRevisionAll = maxRevisionDep;
			}

			if (maxPackageInstallation == null
					|| maxPackageInstallation.getSourceVersion().compareTo(
							dependency.getSourceVersion()) < 0) {
				// New installation or higher version number
				newContext = true;
				break;
			} else if (maxPackageInstallation.getSourceVersion().compareTo(
					dependency.getSourceVersion()) > 0) {
				// Incompatible versions used, a dependency has already been
				// updated
				throw new IllegalStateException("Version older"); // TODO
			} else {
				// Same version as before, store result to reference
				// installations if no new context event occurs
				if (dependency == checkPackage) {
					previousPackageInstallation = maxPackageInstallation;
				} else {
					packageResults.add(maxPackageInstallation);
				}
			}
		}

		if (newContext) {
			// New versions detected -> increase revision number for new
			// depending installations
			log.info("New state for package " + checkPackage.getPackageName());
			packageResults.clear();
			long newRevision = incrementAndGetContextRevisionNumber(contextEntity);

			for (PackageResultMessage newInstallation : dependencies) {
				// Add all new installations
				PackageResult resultEntity = addPackageInstallation(
						newInstallation, contextEntity, newRevision);
				packageResults.add(entityManager.merge(resultEntity));
				// TODO merge via cascade
			}

			addPackageCheck(packageSource, checkPackage, packageResults,
					newRevision, checkResultMessage.getCheckResult());
		} else {
			// Context state not affected by installation results

			if (previousPackageInstallation.getInstallationType() != PackageResultType.CHECK) {
				// State is up to date, but package was not yet checked

				log.info("Promoting previous installation result to check result for package "
						+ checkPackage.getPackageName());

				promoteToPackageCheck(previousPackageInstallation,
						packageResults, checkResultMessage.getCheckResult());
			} else {
				// No action, no new information
				log.info("Check result for package "
						+ checkPackage.getPackageName()
						+ " contained no new information");
			}

		}

		log.info("processed package "
				+ checkResultMessage.getPackageDescription().getPackageName());
	}

	private void promoteToPackageCheck(PackageResult packageResult,
			Set<PackageResult> dependencies, CheckResult checkResult) {

		packageResult.setInstallationType(PackageResultType.CHECK);
		packageResult.addDependencies(dependencies);
		packageResult.setCheckResult(checkResult);

		entityManager.persist(packageResult);
	}

	private PackageResult addPackageCheck(PackageSource packageSource,
			PackageResultMessage packageResult,
			Set<PackageResult> dependencies, long revision,
			CheckResult checkResult) {

		PackageResult result = addPackageInstallation(packageSource,
				packageResult, revision, null);
		promoteToPackageCheck(result, dependencies, checkResult);

		entityManager.persist(result);

		return result;
	}

	public PackageContext addPackageContext(String name) {
		PackageContext contextEntity = new PackageContext(name);
		entityManager.persist(contextEntity);
		return contextEntity;
	}

	private PackageResult addPackageInstallation(PackageSource packageSource,
			PackageResultMessage packageResultMessage, long revision,
			InstallationResult installationResult) {

		PackageResult packageResult = new PackageResult(
				PackageResultType.INSTALLATION);

		packageResult.setSourceLocation(packageResultMessage
				.getSourceLocation());
		// installationResultEntity.setPackageName(packageResult.getPackageName());
		// TODO maybe name and package version

		packageResult.setSourceType(packageResultMessage.getSourceType());
		packageResult.setSourceVersion(packageResultMessage.getSourceVersion());
		packageResult.setPackageSource(packageSource);

		packageResult.setRevision(revision);
		packageResult.setInstallationResult(installationResult);

		entityManager.persist(packageResult);

		return packageResult;
	}

	private PackageResult addPackageInstallation(
			PackageResultMessage packageResult, PackageContext contextEntity,
			long revision) {
		PackageSource packageSource = getPackageSource(packageResult,
				contextEntity);

		return addPackageInstallation(packageSource, packageResult, revision,
				packageResult.getInstallationResult());
	}

	private PackageSource addPackageSource(PackageResultMessage packageResult,
			PackageContext contextEntity) {

		PackageSource packageSource = new PackageSource();

		packageSource.setSourceLocation(packageResult.getSourceLocation());
		packageSource.setPackageName(packageResult.getPackageName());
		packageSource.setSourceType(packageResult.getSourceType());

		packageSource.setRepository(contextEntity);

		entityManager.persist(packageSource);

		return packageSource;
	}

	public List<PackageContext> getAllRepositories() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageContext> query = cb
				.createQuery(PackageContext.class);

		Root<PackageContext> r = query.from(PackageContext.class);
		query.select(r);

		return entityManager.createQuery(query).getResultList();
	}

	public PackageSource getPackageByName(String packageName,
			PackageContext contextEntity) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageSource> query = cb
				.createQuery(PackageSource.class);

		Root<PackageSource> entityRoot = query.from(PackageSource.class);

		query.select(entityRoot).where(
				cb.and(cb.equal(
						entityRoot.get(PackageSource_.repositoryEntity),
						contextEntity),
						cb.equal(entityRoot.get(PackageSource_.packageName),
								packageName)));

		return getSingleOrNull(query);

	}

	public PackageResult getPackageInstallation(PackageSource packageEntity,
			long revisionNumber) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageResult> query = cb
				.createQuery(PackageResult.class);

		Root<PackageResult> entityRoot = query.from(PackageResult.class);

		query.select(entityRoot).where(
				cb.and(cb.equal(entityRoot.get(PackageResult_.packageSource),
						packageEntity),
						cb.equal(entityRoot.get(PackageResult_.revision),
								revisionNumber)));

		return getSingleOrNull(query);
	}

	public void getPackageInstallation(String sourceType,
			String sourceLocation, long sourceVersionNumber) {

	}

	public PackageContext getPackageRepositoryByName(String name) {
		return getSingleEntityByAttribute(PackageContext.class,
				PackageContext_.name, name);
	}

	public PackageContext getPackageRepositoryById(Long id) {
		return getSingleEntityByAttribute(PackageContext.class,
				PackageContext_.id, id);
	}

	public PackageSource getPackageSource(PackageResultMessage packageResult,
			PackageContext contextEntity) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageSource> query = cb
				.createQuery(PackageSource.class);

		Root<PackageSource> p = query.from(PackageSource.class);

		// TODO better distinction between source repo and source location

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(cb.equal(p.get(PackageSource_.sourceType),
				packageResult.getSourceType()));
		predicates.add(cb.equal(p.get(PackageSource_.repositoryEntity),
				contextEntity));

		if (packageResult.getSourceLocation() != null) {
			// Source location
			predicates.add(cb.equal(p.get(PackageSource_.sourceLocation),
					packageResult.getSourceLocation()));
		} else {
			// Source repo
			predicates.add(cb.equal(p.get(PackageSource_.packageName),
					packageResult.getPackageName()));
		}

		query.select(p).where(predicates.toArray(new Predicate[0]));

		PackageSource sourceEntity = getSingleOrNull(query);
		if (sourceEntity == null) {
			return addPackageSource(packageResult, contextEntity);
		} else {
			return sourceEntity;
		}
	}

	public PackageSource getPackageSource(String packageName,
			PackageContext repositoryEntity) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageSource> query = cb
				.createQuery(PackageSource.class);

		Root<PackageSource> p = query.from(PackageSource.class);

		query.select(p).where(
				cb.and(cb.equal(p.get(PackageSource_.repositoryEntity),
						repositoryEntity), cb.equal(
						p.get(PackageSource_.packageName), packageName)));

		return getSingleOrNull(query);
	}

	// public InstallationResult getInstallationResult(
	// PackageResultMessage packageResult,
	// Repository contextEntity) {
	//
	// PackageSource packageSourceEntity = getPackageSource(
	// packageResult, contextEntity);
	//
	// CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	// CriteriaQuery<InstallationResult> query = cb
	// .createQuery(InstallationResult.class);
	//
	// Root<InstallationResult> i = query
	// .from(InstallationResult.class);
	//
	// query.select(i).where(
	// cb.and(cb.equal(
	// i.get(InstallationResultEntity_.owningPackageEntity),
	// packageSourceEntity), cb.equal(
	// i.get(InstallationResultEntity_.sourceLocation),
	// packageResult.getSourceLocation()), cb.equal(
	// i.get(InstallationResultEntity_.sourceType),
	// packageResult.getSourceType()), cb.equal(
	// i.get(InstallationResultEntity_.sourceVersion).get(
	// Version_.versionNumber), packageResult
	// .getSourceVersion().getVersionNumber())));
	//
	// InstallationResult installationResult = getSingleOrNull(query);
	// if (installationResult == null) {
	// return addPackageInstallation(packageSourceEntity, packageResult);
	// } else {
	// return installationResult;
	// }
	//
	// }

	protected <E, V> E getSingleEntityByAttribute(Class<E> entityClass,
			SingularAttribute<? super E, V> attribute, V attributeValue) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<E> query = cb.createQuery(entityClass);

		Root<E> entityRoot = query.from(entityClass);

		query.select(entityRoot).where(
				cb.equal(entityRoot.get(attribute), attributeValue));

		return getSingleOrNull(query);
	}

	protected <T> T getSingleOrNull(CriteriaQuery<T> query) {
		try {
			return entityManager.createQuery(query).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public boolean hasPackageSource(String packageName) {
		return !getAllPackageSources(packageName).isEmpty();
	}

	public long incrementAndGetContextRevisionNumber(
			PackageContext contextEntity) {

		long newRevision = contextEntity.getRevisionNumber() + 1;

		contextEntity.setRevisionNumber(newRevision);
		entityManager.merge(contextEntity);
		return newRevision;
	}

	public List<PackageSource> getAllPackageSources(String packageName) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageSource> query = cb
				.createQuery(PackageSource.class);

		Root<PackageSource> p = query.from(PackageSource.class);

		query.select(p).where(
				cb.equal(p.get(PackageSource_.packageName), packageName));

		return entityManager.createQuery(query).getResultList();
	}

	public List<PackageResult> getPackageResultRange(
			PackageSource packageSource, long minRevision, long maxRevision) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageResult> query = cb
				.createQuery(PackageResult.class);

		Root<PackageResult> i = query.from(PackageResult.class);

		query.select(i).where(
				cb.and(cb.equal(i.get(PackageResult_.packageSource),
						packageSource)),
				cb.between(i.get(PackageResult_.revision), minRevision,
						maxRevision));

		return entityManager.createQuery(query).getResultList();
	}

	public List<PackageResult> getPackageResults(PackageSource packageSource,
			PackageResultType type) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageResult> query = cb
				.createQuery(PackageResult.class);

		Root<PackageResult> pr = query.from(PackageResult.class);

		query.select(pr).where(
				cb.equal(pr.get(PackageResult_.installationType), type),
				cb.equal(pr.get(PackageResult_.packageSource), packageSource));
		return entityManager.createQuery(query).getResultList();
	}

	public List<PackageResult> getDependencies(PackageResult packageResult) {
		List<PackageResult> result = entityManager.merge(packageResult)
				.getDependencies();
		result.size();
		return result;
	}

	public List<String> getAllManagedPackages() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<String> query = cb.createQuery(String.class);

		Root<PackageSource> ps = query.from(PackageSource.class);

		query.select(ps.get(PackageSource_.packageName));
		return entityManager.createQuery(query).getResultList();
	}

	public List<PackageSource> getLatestDependencies(PackageSource packageSource) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<PackageResult> query = cb
				.createQuery(PackageResult.class);

		Root<PackageResult> pr = query.from(PackageResult.class);
		pr.fetch(PackageResult_.packageSource);

		query.select(pr);
		query.where(cb.not(cb.isEmpty(pr.get(PackageResult_.dependencies))),
				cb.equal(pr.get(PackageResult_.packageSource), packageSource));
		query.orderBy(cb.desc(pr.get(PackageResult_.revision)));

		List<PackageResult> resultList = entityManager.createQuery(query)
				.getResultList();
		if (resultList.isEmpty()) {
			return null;
		} else {
			List<PackageSource> result = new ArrayList<>();

			for (PackageResult packageResult : resultList) {
				result.add(packageResult.getPackageSource());
			}

			return result;
		}
	}

}
