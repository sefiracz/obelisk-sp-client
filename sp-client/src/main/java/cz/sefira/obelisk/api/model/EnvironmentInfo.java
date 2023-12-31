/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk.api.model;

import java.util.Properties;

public class EnvironmentInfo {

	private static final String OS_VERSION = "os.version";

	private static final String OS_NAME = "os.name";

	private static final String JAVA_VENDOR = "java.vendor";

	private static final String OS_ARCH = "os.arch";

	private JREVendor jreVendor;

	private String osName;

	private String osArch;

	private String osVersion;

	private Arch arch;

	private OS os;

	public EnvironmentInfo() {
	}

	public static EnvironmentInfo buildFromSystemProperties(Properties systemProperties) {
		EnvironmentInfo info = new EnvironmentInfo();

		String osArch = systemProperties.getProperty(OS_ARCH);
		if ("x86_64".equals(osArch)) {
			osArch = "amd64";
		}
		info.setOsArch(osArch);
		info.setArch(Arch.forOSArch(osArch)); // architecture

		info.setJreVendor(JREVendor.forJREVendor(System.getProperty(JAVA_VENDOR)));

		String osName = systemProperties.getProperty(OS_NAME);
		info.setOsName(osName);
		info.setOs(OS.forOSName(osName)); // operating system

		String osVersion = systemProperties.getProperty(OS_VERSION);
		info.setOsVersion(osVersion);

		return info;
	}

  public static String buildDiagnosticEnvInfo() {
    Properties systemProperties = System.getProperties();
    return "OS name: " + systemProperties.getProperty("os.name") + "\n" +
            "OS version: " + systemProperties.getProperty("os.version") + "\n" +
            "JRE vendor: " + systemProperties.getProperty("java.vendor") + "\n" +
            "JRE name: " + systemProperties.getProperty("java.runtime.name") + "\n" +
            "JRE version: " + systemProperties.getProperty("java.runtime.version") + "\n" +
            "Architecture: " + systemProperties.getProperty("os.arch") + "\n";
  }

	/**
	 * Compare the filter value with the EnvironmentInfo to see if there is a match.
	 *
	 * @param env
	 * @return true if the EnvironmentInfo matches the filter
	 */
	public boolean matches(EnvironmentInfo env) {
		if (os != null && os != env.getOs()) {
			return false;
		}
		if (arch != null && arch != env.getArch()) {
			return false;
		}
		return true;
	}

	public JREVendor getJreVendor() {
		return jreVendor;
	}

	public void setJreVendor(JREVendor jreVendor) {
		this.jreVendor = jreVendor;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public Arch getArch() {
		return arch;
	}

	public void setArch(Arch arch) {
		this.arch = arch;
	}

	public OS getOs() {
		return os;
	}

	public void setOs(OS os) {
		this.os = os;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}

	public String getOsArch() {
		return osArch;
	}

	public void setOsArch(String osArch) {
		this.osArch = osArch;
	}

}
