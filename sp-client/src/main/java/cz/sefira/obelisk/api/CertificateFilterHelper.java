/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.api;

import cz.sefira.obelisk.api.ws.model.CertificateFilter;
import cz.sefira.obelisk.dss.DigestAlgorithm;
import cz.sefira.obelisk.dss.token.DSSPrivateKeyEntry;
import cz.sefira.obelisk.dss.token.SignatureTokenConnection;
import cz.sefira.obelisk.dss.KeyUsageBit;
import cz.sefira.obelisk.dss.x509.CertificateToken;
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
				filteredList.add(entry);
				continue;
			}
			// filter certificates issued by CA
			if (filter.getIssuer() != null) {
				CertificateToken issuer = filter.getIssuer();
				try {
					X509Certificate certificate = entry.getCertificateToken().getCertificate();
					certificate.verify(issuer.getPublicKey());
				} catch (Exception e) {
					//  certificate not issued by given CA or unable to verify
					if (!(e instanceof SignatureException)) {
						logger.error("Unexpected error verifying issued certificate: "+e.getMessage());
					}
					filteredList.add(entry); // filter out
					continue;
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
