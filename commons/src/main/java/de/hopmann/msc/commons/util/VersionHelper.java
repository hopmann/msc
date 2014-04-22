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

import java.math.BigInteger;
import java.util.regex.Pattern;

import de.hopmann.msc.commons.exception.VersionFormatException;

/**
 * Translates between version number representations. For maximized
 * applicability, it assumes a limited number of version subdivisions
 * 
 */
public class VersionHelper {

	private final static Pattern VERSION_SPLIT_PATTERN = Pattern
			.compile("[.\\-]");
	private static final int MAX_DIVISIONS = 5;

	public final static BigInteger MAX_VERSION = BigInteger.valueOf(
			Integer.MAX_VALUE).multiply(BigInteger.valueOf(MAX_DIVISIONS));
	public final static BigInteger MIN_VERSION = BigInteger.ZERO;

	public static BigInteger getVersionNumber(String versionString)
			throws VersionFormatException {
		if (versionString == null || versionString.isEmpty()) {
			return null;
		}
		String[] split = VERSION_SPLIT_PATTERN.split(versionString);

		if (split.length == 0) {
			throw new VersionFormatException("Version number is empty");
		}

		if (split.length > MAX_DIVISIONS) {
			throw new VersionFormatException("Version number has more than "
					+ MAX_DIVISIONS + " subdivisions: " + versionString);
		}

		int[] divisions = new int[MAX_DIVISIONS]; // most significant first

		try {
			for (int i = 0; i < split.length; i++) {
				divisions[i] = Integer.parseInt(split[i]);
			}
		} catch (NumberFormatException e) {
			// Should not happen because reg exp should only match numbers
			throw new VersionFormatException(e.getMessage());
		}

		BigInteger versionNumber = BigInteger.valueOf(divisions[0]);
		for (int i = 1; i < divisions.length; i++) {
			versionNumber = versionNumber.shiftLeft(Integer.SIZE);
			versionNumber = versionNumber.add(BigInteger.valueOf(divisions[i]));
		} // TODO check sign bit

		// long versionNumber = divisions[0];
		// for (int i = 1; i < divisions.length; i++) {
		// versionNumber <<= Short.SIZE;
		// versionNumber += divisions[i];
		// }

		return versionNumber;
	}

	public static BigInteger getVersionNumber(String versionString,
			BigInteger defaultNumber) {
		BigInteger versionNumber;
		try {
			versionNumber = getVersionNumber(versionString);
		} catch (VersionFormatException e) {
			return defaultNumber;
		}

		if (versionNumber == null) {
			return defaultNumber;
		}

		return versionNumber;
	}

	public static String getVersionString(BigInteger versionNumber) {

		StringBuilder versionStringBuilder = new StringBuilder();
		BigInteger integerMask = BigInteger.valueOf(Integer.MAX_VALUE);

		for (int i = 0; i < MAX_DIVISIONS; i++) {
			int division = (short) versionNumber
					.shiftRight((Integer.SIZE * (MAX_DIVISIONS - 1 - i)))
					.and(integerMask).intValue();

			if (versionStringBuilder.length() != 0) {
				versionStringBuilder.append(".");
			}
			versionStringBuilder.append(division);
		}

		return versionStringBuilder.toString();
	}
}
