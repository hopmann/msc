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
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import de.hopmann.msc.commons.util.ControlFile.ControlField;
import de.hopmann.msc.commons.util.ControlFile.ControlFields;

public class ControlFileTest {

	private static class PackagesRecord {
		@ControlField("Package")
		public String packageField;

		@ControlField
		private String Depends;

		@ControlFields({ @ControlField("License"), @ControlField("Licence") })
		String license;

		@ControlField("NeedsCompilation")
		Test needsCompilation;

		@ControlField("Version")
		Test2 version;

		@ControlField("Test")
		String[] arrayTest;

		static class Test {
			String value;

			@SuppressWarnings("unused")
			public Test(String value) {
				this.value = value;
			}
		}

		static class Test2 {
			String value;

			@SuppressWarnings("unused")
			Test2 valueOf(String value) {
				return new Test2(value);
			}

			public Test2(String value) {
				this.value = value;
			}
		}

		public PackagesRecord() {

		}
	}

	private static class PackagesRecordSub extends PackagesRecord {

		@SuppressWarnings("unused")
		public PackagesRecordSub() {
		}
	}

	@Test
	public void test() throws IOException {

		InputStream packagesResource = getClass().getResourceAsStream(
				"/PACKAGES");

		ControlFile<PackagesRecord> controlFile = new ControlFile<PackagesRecord>(
				PackagesRecord.class, new InputStreamReader(packagesResource));

		PackagesRecord readRecord = controlFile.readRecord();
		assertNotNull(readRecord);
		assertEquals("Aspell", readRecord.packageField);
		assertEquals("methods", readRecord.Depends);
		assertEquals("GPL", readRecord.license);
		assertEquals("yes", readRecord.needsCompilation.value);
		assertEquals("0.2-0", readRecord.version.value);
		assertEquals("2", readRecord.arrayTest[1]);
		assertEquals(3, readRecord.arrayTest.length);

		controlFile.close();
	}

	@Test
	public void testSub() throws IOException {

		InputStream packagesResource = getClass().getResourceAsStream(
				"/PACKAGES");

		ControlFile<PackagesRecordSub> controlFile = new ControlFile<PackagesRecordSub>(
				PackagesRecordSub.class,
				new InputStreamReader(packagesResource));

		PackagesRecord readRecord = controlFile.readRecord();
		assertNotNull(readRecord);
		assertEquals("Aspell", readRecord.packageField);
		assertEquals("methods", readRecord.Depends);
		assertEquals("GPL", readRecord.license);
		assertEquals("yes", readRecord.needsCompilation.value);
		assertEquals("0.2-0", readRecord.version.value);
		assertEquals("2", readRecord.arrayTest[1]);
		assertEquals(3, readRecord.arrayTest.length);

		controlFile.close();
	}
}
