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
package de.hopmann.msc.commons.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.hopmann.msc.commons.qualifier.JMSDestination;
import de.hopmann.msc.commons.qualifier.JMSDestination.DestinationType;

@ApplicationScoped
public class DestinationProducer {

	@Produces
	@JMSDestination(DestinationType.MASTER)
	public static Topic createMasterTopic() throws NamingException {
		InitialContext context = new InitialContext();
		Topic lookup = (Topic) context.lookup("jms/MasterTopic");
		System.out.println("Topic lookup: " + Boolean.toString(lookup != null));
		return lookup;
	}

	@Produces
	@JMSDestination(DestinationType.SLAVE)
	public static Queue createSlaveQueue() throws NamingException {
		InitialContext context = new InitialContext();
		Queue lookup = (Queue) context.lookup("jms/SlaveQueue");
		System.out.println("Queue lookup: " + Boolean.toString(lookup != null));
		return lookup;
	}

}
