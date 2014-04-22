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

import java.io.StringWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import de.hopmann.msc.commons.messages.CheckTaskMessage;
import de.hopmann.msc.commons.messages.PackageInstallerMessage;
import de.hopmann.msc.commons.messages.PackageMessage;
import de.hopmann.msc.commons.model.SourceIdentifier;
import de.hopmann.msc.commons.qualifier.JMSDestination;
import de.hopmann.msc.commons.qualifier.JMSDestination.DestinationType;
import de.hopmann.msc.master.ejb.entity.PackageSource;
import de.hopmann.msc.master.ejb.entity.PackageContext;

/**
 * Interacts with worker agents.
 * 
 */
@ApplicationScoped
public class CheckTaskService {

	@Inject
	private Logger log;

	@Inject
	private PackageService packageService;

	@Inject
	private JMSContext jmsContext;

	@Inject
	@JMSDestination(DestinationType.SLAVE)
	private Queue buildQueue;

	private void sendOrder(CheckTaskMessage checkTask) throws JAXBException,
			JMSException {
		log.info("Sending build task");

		JMSProducer producer = jmsContext.createProducer();

		TextMessage message = jmsContext.createTextMessage();

		JAXBContext newInstance = JAXBContext
				.newInstance(CheckTaskMessage.class);
		StringWriter stringWriter = new StringWriter();
		newInstance.createMarshaller().marshal(checkTask, stringWriter);

		message.setText(stringWriter.toString());

		producer.send(buildQueue, message);

	}

	public void queueCheck(String packageName, PackageContext packageContext) {
		PackageSource packageSource = packageService.getPackageSource(
				packageName, packageContext);
		if (packageSource != null) {
			// package is known
			queueCheck(packageSource);
		} else {

			// Check task for new package, dependencies unknown, // TODO probing

			CheckTaskMessage checkTaskMessage = new CheckTaskMessage();

			checkTaskMessage.setContextIdRef(packageContext.getId());
			SourceIdentifier.SourceRepositoryIdentifier defaultRepository = new SourceIdentifier.SourceRepositoryIdentifier(
					"CRAN");
			checkTaskMessage.setDefaultRepository(defaultRepository);
			checkTaskMessage.setPackage(toPackageMessage(packageName));
			checkTaskMessage.setPackageInstaller(new PackageInstallerMessage(
					"3.0.2")); // TODO !

			try {
				sendOrder(checkTaskMessage);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void queueCheck(PackageSource packageSource) {

		CheckTaskMessage checkTaskMessage = new CheckTaskMessage();

		checkTaskMessage.setContextIdRef(packageSource.getRepository().getId());

		SourceIdentifier.SourceRepositoryIdentifier defaultRepository = new SourceIdentifier.SourceRepositoryIdentifier(
				"CRAN");
		checkTaskMessage.setDefaultRepository(defaultRepository);

		checkTaskMessage.setPackage(toPackageMessage(packageSource));

		checkTaskMessage.setPackageInstaller(new PackageInstallerMessage(
				"3.0.2")); // TODO !

		List<PackageSource> latestDependencies = packageService
				.getLatestDependencies(packageSource);

		for (PackageSource depPackageSource : latestDependencies) {
			checkTaskMessage.addDependency(toPackageMessage(depPackageSource));
		}
		// TODO use info of repo or other contexts

		// TODO
		// Test SVN, statically add Matrix dep to all tasks
		SourceIdentifier matrixSource = new SourceIdentifier("Subversion");
		matrixSource
				.setSourceLocation("svn://svn.r-forge.r-project.org/svnroot/matrix/pkg/Matrix");

		PackageMessage pkgMatrix = new PackageMessage();
		pkgMatrix.setSourceDescription(matrixSource);
		pkgMatrix.setName("Matrix");
		checkTaskMessage.addDependency(pkgMatrix);
		//

		try {
			sendOrder(checkTaskMessage);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private PackageMessage toPackageMessage(PackageSource packageSource) {
		PackageMessage packageMessage = new PackageMessage();

		if (packageSource.getSourceType() != null) {
			SourceIdentifier sourceIdentifier = new SourceIdentifier(
					packageSource.getSourceType());
			if (packageSource.getSourceLocation() != null) {
				sourceIdentifier.setSourceLocation(packageSource
						.getSourceLocation());
			}
			packageMessage.setSourceDescription(sourceIdentifier);
		}

		packageMessage.setName(packageSource.getPackageName());
		return packageMessage;

	}

	private PackageMessage toPackageMessage(String packageName) {
		PackageMessage packageMessage = new PackageMessage();
		packageMessage.setName(packageName);
		return packageMessage;

	}
}
