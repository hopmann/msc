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
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ConnectionProducer {

	@Produces
	@ApplicationScoped
	public static ConnectionFactory createConnectionFactory()
			throws NamingException {
		InitialContext context = new InitialContext();
		ConnectionFactory lookup = (ConnectionFactory) context
				.lookup("java:comp/DefaultJMSConnectionFactory");
		// Should safely work even in ear/war lib; lookup standardized by EE7
		System.out.println("CF lookup: " + Boolean.toString(lookup != null));
		return lookup;
	}

	/**
	 * Produces a new JMS {@link Connection} from an application-shared
	 * {@link ConnectionFactory}.
	 * 
	 * @param ConnectionFactory
	 * @return
	 * @throws JMSException
	 */
	@Produces
	public static Connection createConnection(
			ConnectionFactory ConnectionFactory) throws JMSException {
		Connection connection = ConnectionFactory.createConnection();
		connection.start(); // TODO maybe somehow call later
		return connection;
	}

	/**
	 * Produces a JMS {@link Session} of an individual JMS {@link Connection}.
	 * 
	 * @param conn
	 * @return
	 * @throws JMSException
	 */
	@Produces
	public static Session createSessionAutoAck(Connection conn)
			throws JMSException {
		return conn.createSession(Session.AUTO_ACKNOWLEDGE);
	}


	public static void closeConnection(@Disposes Connection conn)
			throws JMSException {
		conn.close();
	}

	public static void closeSession(@Disposes Session session)
			throws JMSException {
		session.close();
	}
}
