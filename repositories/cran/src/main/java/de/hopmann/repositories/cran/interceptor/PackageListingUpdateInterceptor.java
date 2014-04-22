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
package de.hopmann.repositories.cran.interceptor;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import de.hopmann.repositories.cran.service.CRANPackageListingService;

/**
 * Interceptor for business methods requiring up-to-date package metadata
 * listings.
 * 
 */
@PackageListingUpdate
@Interceptor
public class PackageListingUpdateInterceptor {

	@Inject
	private CRANPackageListingService packageListingService;

	@AroundInvoke
	public Object updateListing(InvocationContext invocationContext)
			throws Exception {
		synchronized (packageListingService) {
			// TODO check synchronization because of proxy
			if (packageListingService.isUpdateRequired()) {
				packageListingService.updatePackageListing();
			}
		}
		return invocationContext.proceed();
	}

}
