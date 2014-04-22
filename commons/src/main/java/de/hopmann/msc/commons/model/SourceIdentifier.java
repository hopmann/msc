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
package de.hopmann.msc.commons.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import de.hopmann.msc.commons.model.SourceIdentifier.SourceRepositoryIdentifier;

/**
 * Identifier for a remote package location.
 * 
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({ SourceRepositoryIdentifier.class })
@XmlRootElement(name = "SourceLocation")
@MappedSuperclass
public class SourceIdentifier {

	@GeneratedValue
	@Id
	private Long id;

	protected SourceIdentifier() {

	}

	public SourceIdentifier(String sourceType) {
		this.sourceType = sourceType;
	}

	@XmlAttribute
	private String sourceType;

	@XmlElement
	private Version sourceVersion;

	@XmlAttribute
	private String sourceLocation;

	public Long getId() {
		return id;
	}

	public String getSourceType() {
		return sourceType;
	}

	public Version getSourceVersion() {
		return sourceVersion;
	}

	public String getSourceLocation() {
		return sourceLocation;
	}

	public void setSourceLocation(String sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	/**
	 * Identifier for a remote package location which allows for package name
	 * lookup.
	 * 
	 */
	@XmlRootElement(name = "SourceRepository")
	@XmlAccessorType(XmlAccessType.NONE)
	@Entity
	public static class SourceRepositoryIdentifier extends SourceIdentifier {

		protected SourceRepositoryIdentifier() {

		}

		public SourceRepositoryIdentifier(String repositoryType) {
			super(repositoryType);
		}

	}
//
//	/**
//	 * Identifier for a remote package location which allows for package name
//	 * lookup.
//	 * 
//	 */
//	@XmlRootElement(name = "SourceLocation")
//	@XmlAccessorType(XmlAccessType.NONE)
//	@Entity
//	public static class SourceLocationIdentifier extends SourceIdentifier {
//
//		protected SourceLocationIdentifier() {
//
//		}
//
//		public SourceLocationIdentifier(String repositoryType) {
//			super(repositoryType);
//		}
//
//	}

}
