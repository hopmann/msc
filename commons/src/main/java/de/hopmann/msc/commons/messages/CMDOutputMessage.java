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
package de.hopmann.msc.commons.messages;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.ToStringBuilder;

import de.hopmann.msc.commons.util.RCMDOutputReader;

@XmlRootElement(name = "OutputMessage")
@XmlAccessorType(XmlAccessType.NONE)
public class CMDOutputMessage {

	public enum CMDOutputStatus {
		WARNING, OK, NOTE, SKIPPED, ERROR;

		/**
		 * Returns the {@link CMDOutputStatus} designated by a stars message of
		 * a {@link RCMDOutputReader}.
		 * 
		 * @param starsMessage
		 * @return
		 */
		public static CMDOutputStatus valueOfStarsMessage(String starsMessage) {
			if (starsMessage == null || starsMessage.isEmpty()) {
				return null;
			}

			for (CMDOutputStatus status : CMDOutputStatus.values()) {
				if (starsMessage.endsWith(status.name())) {
					return status;
				}
			}

			return null;
		}
	}

	@XmlElementWrapper
	@XmlElement
	private String[] starsLevels;

	@XmlElementWrapper
	@XmlElement
	private String[] messages;

	@XmlAttribute
	private CMDOutputStatus status;

	protected CMDOutputMessage() {

	}

	public CMDOutputMessage(List<String> messages, String[] starsLevels,
			CMDOutputStatus status) {
		this.messages = messages.toArray(new String[0]);
		this.starsLevels = starsLevels;
		this.status = status;
	}

	public String[] getMessages() {
		return messages;
	}

	public String[] getStarsLevels() {
		return starsLevels;
	}

	public CMDOutputStatus getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("starsLevels", starsLevels)
				.append("messages", messages).append("status", status)
				.toString();
	}
}