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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hopmann.msc.commons.messages.CMDOutputMessage;
import de.hopmann.msc.commons.messages.CMDOutputMessage.CMDOutputStatus;

/**
 * Facility to read R CMD output. Associates single {@link CMDOutputMessage} and
 * its status.
 * 
 */
public class RCMDOutputReader implements AutoCloseable {

	private static final String DONE = "DONE";

	private BufferedReader processOutReader;
	private String[] starsLevels = new String[10];
	private List<String> messages = new ArrayList<>(1);
	private int maxLevel = 0;
	private boolean isDone = false;
	private CMDOutputStatus starsMessageStatus;
	private PrintStream logPrintStream;
	private int[] statusCount = new int[CMDOutputStatus.values().length];

	private static Pattern starsMessagePattern = Pattern
			.compile("^\\s*(\\*++)\\s*(.+)");

	public RCMDOutputReader(InputStream inputStream) {
		this.processOutReader = new BufferedReader(new InputStreamReader(
				inputStream));
	}

	public RCMDOutputReader(InputStream inputStream, PrintStream logPrintStream) {
		this(inputStream);

		this.logPrintStream = logPrintStream;
	}

	public RCMDOutputReader(Process process) {
		this(process.getInputStream());
	}

	public RCMDOutputReader(Process process, PrintStream logPrintStream) {
		this(process);

		this.logPrintStream = logPrintStream;
	}

	private void addMessage(String line) {
		messages.add(line);
	}

	private CMDOutputMessage buildMessage() {
		if (messages.isEmpty()
				&& (starsMessageStatus == null || starsMessageStatus == CMDOutputStatus.OK)) {
			return null;
		}

		CMDOutputMessage message = new CMDOutputMessage(messages,
				Arrays.copyOfRange(starsLevels, 0, maxLevel + 1),
				starsMessageStatus);
		messages.clear();
		starsMessageStatus = null;
		return message;
	}

	@Override
	public void close() throws IOException {
		if (processOutReader != null) {
			processOutReader.close();
		}
		if (logPrintStream != null) {
			logPrintStream.close();
		}
	}

	public boolean isDone() {
		return isDone;
	}

	public CMDOutputMessage readMessage() throws IOException {
		String line;
		while ((line = processOutReader.readLine()) != null) {
			if (logPrintStream != null) {
				logPrintStream.println(line);
			}

			Matcher matcher = starsMessagePattern.matcher(line);

			if (matcher.find()) {
				// Starsmessage

				// Message to return before setting new stars level
				CMDOutputMessage output = buildMessage();

				// New message level
				int level = matcher.group(1).length() - 1; // lower number,
															// higher level
				setStarsMessage(level, matcher.group(2));

				if (output != null) {
					// returns message if there were lines set
					return output;
				}
			} else {
				// other message
				addMessage(line);
			}
		}

		if (!messages.isEmpty()) {
			// last message
			return buildMessage();
		}

		// return null if called after EOF
		return null;
	}

	private void setStarsMessage(int level, String message) {
		if (level < 0) {
			// No starsmessage
			return;
		}

		if (message.startsWith(DONE)) {
			isDone = true;
		}

		starsMessageStatus = CMDOutputStatus.valueOfStarsMessage(message);
		if (starsMessageStatus != null) {
			statusCount[starsMessageStatus.ordinal()]++;
		}

		if (level < maxLevel) {
			// higher stars level -> clear all sub-levels
			Arrays.fill(starsLevels, level,
					Math.min(maxLevel + 1, starsLevels.length), null);
		}
		if (level < starsLevels.length) {
			starsLevels[level] = message;
		}
		maxLevel = level;
	}

	public int getStatusCount(CMDOutputStatus status) {
		return statusCount[status.ordinal()];
	}

}
