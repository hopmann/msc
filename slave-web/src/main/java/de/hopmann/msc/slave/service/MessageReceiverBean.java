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
package de.hopmann.msc.slave.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import de.hopmann.msc.commons.messages.CheckTaskMessage;
import de.hopmann.msc.commons.messages.ResultMessage;
import de.hopmann.msc.commons.qualifier.JMSDestination;
import de.hopmann.msc.commons.qualifier.JMSDestination.DestinationType;
import de.hopmann.msc.slave.installer.PackageInstallerHolder;

/**
 * Class handles the receiving of checking task messages. Core purpose is to
 * overcome limitations in Java EE full profile JMS integration, preventing
 * usage of listeners and programmatic creation of multiple message consumers on
 * a single queue. Balances threads and resources.
 */
@Singleton
@Startup
public class MessageReceiverBean {

	@Inject
	private Logger log;

	@Inject
	private Instance<Session> jmsSessionInstance;

	@Inject
	@JMSDestination(DestinationType.SLAVE)
	private Queue slaveQueue;

	@Inject
	@JMSDestination(DestinationType.MASTER)
	private Topic masterTopic;

	@Inject
	private PackageInstallerBean packageInstallerBean;

	@Inject
	private PackageCheckService checkProvider;

	private ExecutorService processMessageExecutor;
	private Semaphore processBoundSemaphore;

	@Resource
	private ManagedThreadFactory managedThreadFactory;

	private ArrayBlockingQueue<ResultMessage> resultMessageQueue = new ArrayBlockingQueue<>(
			10, true);

	public MessageReceiverBean() {

	}

	/**
	 * Represents a single worker to concurrently process check task messages.
	 * 
	 */
	private class MessageReceiver implements Runnable, AutoCloseable {
		private MessageConsumer consumer;
		private PackageInstallerHolder installerHolder;
		private Session session;

		public MessageReceiver(PackageInstallerHolder installerHolder)
				throws JMSException {
			this.installerHolder = installerHolder;
		}

		private void setupConsumer() throws JMSException {
			this.session = jmsSessionInstance.get();
			this.consumer = session.createConsumer(slaveQueue);
			// TODO selector
		}

		@Override
		public void run() {
			try {
				setupConsumer();
			} catch (JMSException e) {
				log.log(Level.SEVERE,
						"Could not set-up message consumer for installer "
								+ installerHolder, e);
				return;
			}

			while (true) {
				try {
					receiveMessage();
				} catch (JMSException | InterruptedException e) {
					log.log(Level.WARNING, "Message Receiver for installer "
							+ packageInstallerBean + " closed", e);
					break;
				} catch (Exception e) {
					// TODO return error
					log.log(Level.SEVERE, "Message contains error", e);
				}
			}

			try {
				close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void receiveMessage() throws JMSException, InterruptedException {
			Message message = consumer.receive();

			if (message == null) {
				throw new InterruptedException("Consumer closed");
			}

			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;

				CheckTaskMessage taskMessage;
				try {
					Unmarshaller unmarshaller = JAXBContext.newInstance(
							CheckTaskMessage.class).createUnmarshaller();

					taskMessage = (CheckTaskMessage) unmarshaller
							.unmarshal(new StringReader(textMessage.getText()));

				} catch (JAXBException e) {
					// TODO
					throw new IllegalArgumentException(
							"Message contains no valid check task representation",
							e);
				}

				processBoundSemaphore.acquire();
				try {
					processMessageExecutor
							.submit(new CheckTaskMessageProcessor(taskMessage,
									installerHolder));
				} catch (Exception e) {
					// TODO log
					processBoundSemaphore.release();
				}
			}

		}

		@Override
		public void close() throws Exception {
			consumer.close();
			session.close();
		}

	}

	/**
	 * Represents the actual execution of a checking task. Implementation makes
	 * sure that resulting message is produced on the main thread, while
	 * executing checking tasks in parallel.
	 * 
	 */
	private class CheckTaskMessageProcessor implements Runnable {

		private CheckTaskMessage taskMessage;
		private PackageInstallerHolder installerHolder;

		public CheckTaskMessageProcessor(CheckTaskMessage taskMessage,
				PackageInstallerHolder installerHolder) {
			this.taskMessage = taskMessage;
			this.installerHolder = installerHolder;
		}

		@Override
		public void run() {
			try {
				ResultMessage checkResult = checkProvider.checkTask(
						taskMessage, installerHolder);
				checkResult.setContextIdRef(taskMessage.getContextIdRef());// To
																			// correlate
																			// contexts
				resultMessageQueue.put(checkResult);
			} catch (InterruptedException e) {

			} catch (Exception e) {
				log.log(Level.SEVERE, "Error while checking package", e);
			} finally {
				processBoundSemaphore.release();
			}
		}
	}

	/**
	 * Unit to post checking results back to the integrating layer. Messages may
	 * be sent only on a single thread, requiring to introduce a shared result
	 * message queue.
	 * 
	 */
	private class CheckResultMessageSender implements Runnable {

		private Session session;
		private MessageProducer producer;

		private void setupProducer() throws JMSException {
			this.session = jmsSessionInstance.get();
			this.producer = session.createProducer(masterTopic);
		}

		@Override
		public void run() {
			try {
				setupProducer();
			} catch (JMSException e) {
				log.log(Level.SEVERE,
						"Could not set-up message producer for check task results",
						e);
				return;
			}

			while (true) {
				try {
					ResultMessage checkResultMessage = resultMessageQueue
							.take();
					TextMessage message = session.createTextMessage();
					JAXBContext newInstance = JAXBContext
							.newInstance(ResultMessage.class);
					StringWriter stringWriter = new StringWriter();
					newInstance.createMarshaller().marshal(checkResultMessage,
							stringWriter);

					message.setText(stringWriter.toString());

					producer.send(message);
				} catch (InterruptedException e) {
					return;
				} catch (JAXBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}

	/**
	 * Eager loading of a bean requires a scheduled execution which is canceled
	 * at first call. Using @Stratup-mechanisms would hamper deployment phase.
	 * 
	 * @param timer
	 */
	@Schedule(hour = "*", minute = "*", persistent = false)
	private void setupReceivers(Timer timer) {
		timer.cancel();
		log.info("Registering message consumers");
		// TODO

		processMessageExecutor = Executors.newFixedThreadPool(4,
				managedThreadFactory);
		processBoundSemaphore = new Semaphore(4, true);

		for (PackageInstallerHolder packageInstaller : packageInstallerBean
				.getAvailableInstaller()) {
			try {
				managedThreadFactory.newThread(
						new MessageReceiver(packageInstaller)).start();
			} catch (JMSException e) {
				log.log(Level.SEVERE,
						"Could not initialize message consumer for installer "
								+ packageInstaller, e);
			}
		}

		managedThreadFactory.newThread(new CheckResultMessageSender()).start();
	}

}
