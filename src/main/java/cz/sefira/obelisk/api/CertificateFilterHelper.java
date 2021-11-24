package cz.sefira.obelisk.api;

import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.tsl.KeyUsageBit;

import java.util.*;

/**
 * Provides filtering capabilities for product adapters.
 *
 * @author Landry Soules
 *
 */
public class CertificateFilterHelper {

	public List<DSSPrivateKeyEntry> filterKeys(SignatureTokenConnection token, CertificateFilter filter) {
		List<DSSPrivateKeyEntry> fullList = token.getKeys();
		List<DSSPrivateKeyEntry> filteredList = new ArrayList<>();
		if(filter == null)
			return fullList;
		for (DSSPrivateKeyEntry entry : fullList) {
			// expired
			if (!filter.getAllowExpired() && entry.getCertificate().isExpiredOn(new Date())) {
				if(System.getProperty("allowExpired") == null) {
					filteredList.add(entry);
				}
			}
			// absolute certificate filter via SHA1 digest
			if (filter.getCertificateSHA1() != null &&
					!Arrays.equals(filter.getCertificateSHA1(), entry.getCertificate().getDigest(DigestAlgorithm.SHA1))) {
				filteredList.add(entry);
			}
			// key usage nonRepudiation filter
			if (filter.getNonRepudiationBit() && !entry.getCertificate().checkKeyUsage(KeyUsageBit.nonRepudiation)) {
				filteredList.add(entry);
			}
			// key usage digitalSignature filter
			if (filter.getDigitalSignatureBit() && !entry.getCertificate().checkKeyUsage(KeyUsageBit.digitalSignature)) {
				filteredList.add(entry);
			}
		}
 		fullList.removeAll(filteredList);
		return fullList;
	}
}
