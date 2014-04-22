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

import static org.junit.Assert.*;

import org.junit.Test;

import de.hopmann.msc.commons.exception.VersionFormatException;

public class VersionHelperTest {

	@Test
	public void testEquals() throws VersionFormatException {

		String versionA = "1.2.3";
		String versionB = "1-2.3";

		assertEquals(VersionHelper.getVersionNumber(versionA),
				VersionHelper.getVersionNumber(versionB));
	}

	@Test
	public void testNotEquals() throws VersionFormatException {

		String versionA = "1.2.3";
		String versionB = "1-2-3.4";

		assertNotEquals(VersionHelper.getVersionNumber(versionA),
				VersionHelper.getVersionNumber(versionB));
	}

	@Test
	public void testComparison() throws VersionFormatException {

		String versionA = "1.2.4";
		String versionB = "1-2-3.4";
		String versionC = "1-2-3.8";

		assertTrue(VersionHelper.getVersionNumber(versionA).compareTo(
				VersionHelper.getVersionNumber(versionB)) > 0);

		assertTrue(VersionHelper.getVersionNumber(versionB).compareTo(
				VersionHelper.getVersionNumber(versionC)) < 0);
	}

	@Test
	public void testInverse() throws VersionFormatException {

		class CheckInverseMapping {
			void assertInverse(String versionString)
					throws VersionFormatException {

				assertEquals(VersionHelper.getVersionNumber(versionString),
						VersionHelper.getVersionNumber(VersionHelper
								.getVersionString(VersionHelper
										.getVersionNumber(versionString))));
			}
		}

		new CheckInverseMapping().assertInverse("1.2.3.4");
		new CheckInverseMapping().assertInverse("1.2-3.4");
		new CheckInverseMapping().assertInverse("1.2");
		new CheckInverseMapping().assertInverse("1.2-3");
	}

}
