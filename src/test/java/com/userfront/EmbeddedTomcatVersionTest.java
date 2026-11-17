package com.userfront;

import static org.junit.Assert.assertTrue;

import org.apache.catalina.util.ServerInfo;
import org.junit.Test;

/**
 * Guards against regressing to an embedded Tomcat affected by CVE-2025-24813
 * (fixed in 9.0.99 / 10.1.35 / 11.0.3).
 */
public class EmbeddedTomcatVersionTest {

	private static final int[] MIN_SAFE_VERSION = { 9, 0, 99 };

	@Test
	public void embeddedTomcatIsNotVulnerableToCve202524813() {
		String version = ServerInfo.getServerNumber();
		assertTrue("Embedded Tomcat " + version + " is older than 9.0.99 (CVE-2025-24813)",
				compareVersions(version, MIN_SAFE_VERSION) >= 0);
	}

	private static int compareVersions(String version, int[] minimum) {
		String[] parts = version.split("\\.");
		for (int i = 0; i < minimum.length; i++) {
			int part = i < parts.length ? Integer.parseInt(parts[i].replaceAll("\\D.*$", "")) : 0;
			if (part != minimum[i]) {
				return Integer.compare(part, minimum[i]);
			}
		}
		return 0;
	}
}
