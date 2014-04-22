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

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import de.hopmann.msc.commons.model.PackageRepository;

public class RepositoryTest {

	static String CRANXML = "<Repository repositoryType=\"CRAN\"/>";

	@XmlRootElement
	private static class TestClass {
		@XmlElement
		PackageRepository repository;
	}


	@Test
	public void testUnmarshall() throws JAXBException {
		

		JAXBContext repoContext = JAXBContext.newInstance(TestClass.class);

		Object unmarshal = repoContext.createUnmarshaller().unmarshal(new StringReader("<testClass><repository repositoryType=\"CRAN\"/></testClass>"));
		System.out.println(unmarshal.toString());
	}
}
