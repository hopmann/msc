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
package de.hopmann.repositories.cran.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import de.hopmann.msc.commons.exception.PackageNotFoundException;
import de.hopmann.msc.commons.model.PackageSource.PackageAccessor;
import de.hopmann.msc.commons.model.Version;
import de.hopmann.msc.commons.model.Version_;
import de.hopmann.msc.commons.qualifier.Configuration;
import de.hopmann.msc.commons.util.ControlFile;
import de.hopmann.repositories.commons.entity.PackageEntity_;
import de.hopmann.repositories.commons.service.PackageCacheService;
import de.hopmann.repositories.cran.entity.CRANPackageEntity;
import de.hopmann.repositories.cran.entity.CRANPackageEntity_;
import de.hopmann.repositories.cran.interceptor.PackageListingUpdate;

/**
 * Service for accessing package listing entities.
 * 
 */
@ApplicationScoped
@Transactional
public class CRANPackageListingService extends
		PackageCacheService<CRANPackageEntity> {

	private CloseableHttpClient httpclient = HttpClients.createDefault();

	private String[] cranRepositories;

	private boolean listingUpdateRequired = false;

	public CRANPackageListingService() {

	}

	@Inject
	public CRANPackageListingService(
			@Configuration(value = "cranRepositories", required = true) String[] cranRepositories) {
		this.cranRepositories = cranRepositories;
	}

	@Override
	protected Class<CRANPackageEntity> getPackageEntityClass() {
		return CRANPackageEntity.class;
	}

	@Transactional(TxType.REQUIRES_NEW)
	public void updatePackageListing() {
		// TODO make sure called only once

		// TODO handle multiple repos
		// for (String sourceRepository : cranRepositories) {

		log.info("Updating package listing");

		String sourceRepository = cranRepositories[0];

		HttpGet getListing = new HttpGet(sourceRepository
				+ "src/contrib/PACKAGES");

		try {
			CloseableHttpResponse httpResponse = httpclient.execute(getListing);

			ControlFile<CRANPackageDescriptionRecord> controlFile = new ControlFile<CRANPackageDescriptionRecord>(
					CRANPackageDescriptionRecord.class, new InputStreamReader(
							httpResponse.getEntity().getContent()));

			log.info("clearing listing");
			try {
				removeCurrentListing();
			} catch (Exception e) {
				e.printStackTrace();
				controlFile.close();
				return;
			}
			log.info("adding packages");

			List<String> failedRecords = new ArrayList<String>();
			CRANPackageDescriptionRecord record = null;
			while ((record = controlFile.readRecord()) != null) {
				if (record.getPackageName() == null)
					continue;
				try {
					addPackage(record.buildEntity());
				} catch (Exception e) {
					if (record.getPackageName() != null) {
						failedRecords.add(record.getPackageName());
					}
				}
			}

			controlFile.close();

			log.warning("Adding of following package entries failed: "
					+ failedRecords.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		// }

		log.info("Finished updating package listing");
	}

	public void removeCurrentListing() {
		// TODO Bulk removal, JPQL not cascading
		// entityManager.createQuery("DELETE FROM DependencyEntity").executeUpdate();
		// or
		// entityManager.createQuery("DELETE FROM PackageEntity").executeUpdate();

		// Alternative

		// CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		//
		// CriteriaDelete<PackageEntity> delete = cb
		// .createCriteriaDelete(PackageEntity.class);
		// Root<PackageEntity> p = delete.from(PackageEntity.class);
		// delete.where(cb.isFalse(p.get(PackageEntity_.archived)));
		//
		// entityManager.createQuery(delete).executeUpdate();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<CRANPackageEntity> query = cb
				.createQuery(CRANPackageEntity.class);
		Root<CRANPackageEntity> p = query.from(CRANPackageEntity.class);
		query.where(cb.isFalse(p.get(CRANPackageEntity_.archived)));

		List<CRANPackageEntity> resultList = entityManager.createQuery(query)
				.getResultList();

		for (CRANPackageEntity cRANPackageEntity : resultList) {
			entityManager.remove(cRANPackageEntity);
		}
	}

	public int getPackageListingCount() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Number> query = cb.createQuery(Number.class);
		query.select(cb.count(query.from(CRANPackageEntity.class)));
		return entityManager.createQuery(query).getSingleResult().intValue();
	}

	public boolean isUpdateRequired() {
		if (listingUpdateRequired) {
			listingUpdateRequired = false;
			return true;
		}
		return getPackageListingCount() == 0;
	}

	@PackageListingUpdate
	public boolean isAvailable(String packageName, Version sourceVersion) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<CRANPackageEntity> query = cb
				.createQuery(CRANPackageEntity.class);
		Root<CRANPackageEntity> p = query.from(CRANPackageEntity.class);
		query.where(cb.and(
				//
				cb.equal(p.get(PackageEntity_.name), packageName),
				cb.equal(
						p.get(PackageEntity_.version).get(
								Version_.versionNumber),
						sourceVersion.getVersionNumber())));

		List<CRANPackageEntity> resultList = entityManager.createQuery(query)
				.getResultList();
		return !resultList.isEmpty();
	}

	public void setArchived(PackageAccessor packageAccessor) {
		try {
			CRANPackageEntity packageEntity = getPackageByAccessor(packageAccessor);
			packageEntity.setArchived(true);
			entityManager.merge(packageEntity);
		} catch (PackageNotFoundException e) {
			// Add new one with archive set to true
			addPackage(new CRANPackageEntity(packageAccessor.getPackageName(),
					packageAccessor.getPackageVersion(), true));
		}

		this.listingUpdateRequired = true;
	}

	public void setMissing(PackageAccessor packageAccessor) {
		try {
			removePackage(getPackageByAccessor(packageAccessor));
		} catch (PackageNotFoundException e) {

		}
	}

	@Override
	@PackageListingUpdate
	public CRANPackageEntity getPackageLatestPackageVersion(String pkgName)
			throws PackageNotFoundException {
		return super.getPackageLatestPackageVersion(pkgName);
	}

}
