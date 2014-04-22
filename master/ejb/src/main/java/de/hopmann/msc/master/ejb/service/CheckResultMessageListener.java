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

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import de.hopmann.msc.commons.messages.CheckResultMessage;
import de.hopmann.msc.commons.messages.ResultMessage;

/**
 * Message-Driven Bean implementation to receive package check results
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/MasterTopic"),
		@ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
		@ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "Check result updates"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class CheckResultMessageListener implements MessageListener {

	@Inject
	private Logger log;

	@Inject
	private PackageService packageService;

	public CheckResultMessageListener() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see MessageListener#onMessage(Message)
	 */
	public void onMessage(Message message) {

		log.info("Test Result");
		if (message instanceof TextMessage) {
			TextMessage textMessage = (TextMessage) message;

			try {
				log.info(textMessage.getText());

				Unmarshaller unmarshaller = JAXBContext.newInstance(
						CheckResultMessage.class).createUnmarshaller();

				ResultMessage resultMessage = (ResultMessage) unmarshaller
						.unmarshal(new StringReader(textMessage.getText()));

				if (resultMessage instanceof CheckResultMessage) {
					packageService
							.addCheckResult((CheckResultMessage) resultMessage);
				} else {

					// TODO Exception message
				}

			} catch (Exception e) {
				log.log(Level.SEVERE, "Error receiving message", e);
			}
		}
	}

}
