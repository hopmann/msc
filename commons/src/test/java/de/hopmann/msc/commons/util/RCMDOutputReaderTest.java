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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.hopmann.msc.commons.messages.CMDOutputMessage;
import de.hopmann.msc.commons.messages.CMDOutputMessage.CMDOutputStatus;

public class RCMDOutputReaderTest {

	@Test
	public void testInstall() throws Exception {

		InputStream cmdInstallLog = getClass().getResourceAsStream(
				"/cmdInstallSpacetime.log");

		RCMDOutputReader rcmdOutputReader = new RCMDOutputReader(cmdInstallLog);

		List<CMDOutputMessage> messages = new ArrayList<>();
		CMDOutputMessage message;
		while ((message = rcmdOutputReader.readMessage()) != null) {
			messages.add(message);
		}

		rcmdOutputReader.close();

		System.out.println(messages);
		assertEquals(1, messages.size());

		CMDOutputMessage firstMsg = messages.get(0);
		assertEquals(2, firstMsg.getMessages().length);
		assertTrue(firstMsg.getStarsLevels()[1]
				.equals("preparing package for lazy loading"));
		assertTrue(rcmdOutputReader.isDone());
	}

	@Test
	public void testCheck() throws Exception {

		InputStream cmdInstallLog = getClass().getResourceAsStream(
				"/cmdCheckSpam.log");

		RCMDOutputReader rcmdOutputReader = new RCMDOutputReader(cmdInstallLog);

		List<CMDOutputMessage> messages = new ArrayList<>();
		CMDOutputMessage message;
		while ((message = rcmdOutputReader.readMessage()) != null) {
			messages.add(message);
		}

		rcmdOutputReader.close();

		System.out.println(messages);
		assertEquals(3, messages.size());

		CMDOutputMessage firstMsg = messages.get(0);
		assertEquals(1, firstMsg.getMessages().length);
		assertTrue(firstMsg.getStarsLevels()[0]
				.startsWith("checking Rd cross-references"));
		assertEquals(CMDOutputStatus.NOTE, firstMsg.getStatus());
		assertEquals(2, rcmdOutputReader.getStatusCount(CMDOutputStatus.NOTE));
	}
}
