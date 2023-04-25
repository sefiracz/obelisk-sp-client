package cz.sefira.obelisk.api;

import cz.sefira.obelisk.api.ws.model.CertificateFilter;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.dss.KeyUsageBit;
import cz.sefira.obelisk.dss.x509.CertificateToken;
import cz.sefira.obelisk.token.keystore.EmptyKeyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Provides filtering capabilities for product adapters.
 *
 * @author Landry Soules
 *
 */
public class CertificateFilterHelper {

	private static final Logger logger = LoggerFactory.getLogger(CertificateFilterHelper.class.getName());

	public List<DSSPrivateKeyEntry> filterKeys(SignatureTokenConnection token, CertificateFilter filter) {
		List<DSSPrivateKeyEntry> fullList = token.getKeys();
		List<DSSPrivateKeyEntry> filteredList = new ArrayList<>();
		if(filter == null)
			return fullList;
		for (DSSPrivateKeyEntry entry : fullList) {
			// expired
			if (!filter.getAllowExpired() && entry.getCertificateToken().isExpiredOn(new Date())) {
				if(System.getProperty("allowExpired") == null) {
					filteredList.add(entry);
					continue;
				}
			}
			// filter certificates issued by CA
			if (filter.getIssuer() != null) {
				CertificateToken issuer = filter.getIssuer();
				try {
					if (!(entry instanceof EmptyKeyEntry)) {
						X509Certificate certificate = entry.getCertificateToken().getCertificate();
						certificate.verify(issuer.getPublicKey());
					}
				} catch (SignatureException e) {
					//  certificate not issued by given CA
					filteredList.add(entry); // filter out
					continue;
				} catch (Exception e) {
					logger.error("Unexpected error verifying issued certificates: "+e.getMessage());
				}
			}
			// absolute certificate filter via SHA256 digest
			if (filter.getCertificateId() != null &&
					!Arrays.equals(filter.getCertificateId(), entry.getCertificateToken().getDigest(DigestAlgorithm.SHA256))) {
				filteredList.add(entry);
				continue;
			}
			// key usage nonRepudiation filter
			if (filter.getNonRepudiationBit() && !entry.getCertificateToken().checkKeyUsage(KeyUsageBit.nonRepudiation)) {
				filteredList.add(entry);
				continue;
			}
			// key usage digitalSignature filter
			if (filter.getDigitalSignatureBit() && !entry.getCertificateToken().checkKeyUsage(KeyUsageBit.digitalSignature)) {
				filteredList.add(entry);
				continue;
			}
		}
 		fullList.removeAll(filteredList);
		return fullList;
	}
}
