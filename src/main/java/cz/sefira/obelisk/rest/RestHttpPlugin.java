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
package cz.sefira.obelisk.rest;

import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.plugin.*;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.DigestAlgorithm;
import cz.sefira.obelisk.api.*;
import cz.sefira.obelisk.api.plugin.*;
import cz.sefira.obelisk.json.GsonHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Default implementation of HttpPlugin for NexU.
 *
 * @author David Naramski
 */
public class RestHttpPlugin implements HttpPlugin {

	private static final Logger logger = LoggerFactory.getLogger(RestHttpPlugin.class.getName());

	@Override
	public List<InitializationMessage> init(String pluginId, NexuAPI api) {
		return Collections.emptyList();
	}

	@Override
	public HttpResponse process(NexuAPI api, HttpRequest req) throws Exception {

		final String target = req.getTarget();
		logger.info("PathInfo " + target);

		final String payload = IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
		logger.info("Payload '" + payload + "'");

		switch(target) {
		case "/sign":
			return signRequest(api, req, payload);
		case "/getToken":
		  return getToken(api, req, payload);
		case "/getCertificate":
			return getCertificate(api, req, payload);
		case "/smartcardList":
			return smartcardList(api, req, payload);
		default:
			throw new RuntimeException("Target not recognized " + target);
		}
	}


	private HttpResponse signRequest(NexuAPI api, HttpRequest req, String payload) {
		logger.info("Signature");
		final SignatureRequest r = GsonHelper.fromJson(payload, SignatureRequest.class);
    r.setSessionValue(getSessionValue(req));
		final HttpResponse invalidRequestHttpResponse = checkRequestValidity(api, r);
		if(invalidRequestHttpResponse != null) {
			return invalidRequestHttpResponse;
		} else {
			logger.info("Call API");
			final Execution<?> respObj = api.sign(r);
			return toHttpResponse(respObj);
		}
	}

	private synchronized HttpResponse getCertificate(NexuAPI api, HttpRequest req, String payload) {
		logger.info("API call certificates");
		final GetCertificateRequest r;
		if (StringUtils.isEmpty(payload)) {
			r = new GetCertificateRequest();

			final String certificatePurpose = req.getParameter("certificatePurpose");
			if (certificatePurpose != null) {
				logger.info("Certificate purpose " + certificatePurpose);
				final Purpose purpose = Enum.valueOf(Purpose.class, certificatePurpose);
				final CertificateFilter certificateFilter = new CertificateFilter();
				certificateFilter.setPurpose(purpose);
				r.setCertificateFilter(certificateFilter);
			}else {
				final String nonRepudiation = req.getParameter("nonRepudiation");
				if(isNotBlank(nonRepudiation)) {
					final CertificateFilter certificateFilter = new CertificateFilter();
					certificateFilter.setNonRepudiationBit(Boolean.parseBoolean(nonRepudiation));
					r.setCertificateFilter(certificateFilter);
				}
				final String digitalSignature = req.getParameter("digitalSignature");
				if(isNotBlank(digitalSignature)) {
					final CertificateFilter certificateFilter = new CertificateFilter();
					certificateFilter.setDigitalSignatureBit(Boolean.parseBoolean(digitalSignature));
					r.setCertificateFilter(certificateFilter);
				}
			}
		} else {
			r = GsonHelper.fromJson(payload, GetCertificateRequest.class);
		}
    r.setSessionValue(getSessionValue(req));
		final HttpResponse invalidRequestHttpResponse = checkRequestValidity(api, r);
		if(invalidRequestHttpResponse != null) {
			return invalidRequestHttpResponse;
		} else {
			logger.info("Call API");
			final Execution<?> respObj = api.getCertificate(r);
			return toHttpResponse(respObj); // GetCertificateResponse
		}
	}

	private HttpResponse getToken(NexuAPI api, HttpRequest req, String payload) {
		final GetTokenRequest r;
		if (StringUtils.isEmpty(payload)) {
			r = new GetTokenRequest();
			final String certificate = req.getParameter("certificate");
			if (certificate != null) {
				logger.info("Certificate: " + certificate);
				r.setCertificate(DSSUtils.loadCertificate(Base64.decodeBase64(certificate)));
			}
		} else {
			r = GsonHelper.fromJson(payload, GetTokenRequest.class);
		}
    r.setSessionValue(getSessionValue(req));
		final HttpResponse invalidRequestHttpResponse = checkRequestValidity(api, r);
		if(invalidRequestHttpResponse != null) {
			return invalidRequestHttpResponse;
		} else {
			logger.info("Call API");
			final Execution<?> respObj = api.getToken(r);
			return toHttpResponse(respObj); // GetTokenResponse
		}
	}

	private HttpResponse smartcardList(NexuAPI api, HttpRequest req, String payload) {
		byte[] digest = DSSUtils.digest(DigestAlgorithm.SHA256, payload.getBytes(StandardCharsets.UTF_8));
		final SmartcardListRequest r = GsonHelper.fromJson(payload, SmartcardListRequest.class);
		r.setDigest(digest);
		logger.info(Base64.encodeBase64String(digest));
		final HttpResponse invalidRequestHttpResponse = checkRequestValidity(api, r);
    r.setSessionValue(getSessionValue(req));
    if(invalidRequestHttpResponse != null) {
			return invalidRequestHttpResponse;
		} else {
			logger.info("Call API");
      final Execution<?> respObj = api.smartcardList(r);
      return toHttpResponse(respObj);
		}
	}

  protected <T> Execution<T> returnNullIfValid(NexuRequest request) {
    return null; // to be implemented
  }

	private HttpResponse checkRequestValidity(final NexuAPI api, final NexuRequest request) {
		final Execution<Object> verification = returnNullIfValid(request);
		if(verification != null) {
			final Feedback feedback;
			if(verification.getFeedback() == null) {
				feedback = new Feedback();
				feedback.setFeedbackStatus(FeedbackStatus.SIGNATURE_VERIFICATION_FAILED);
				verification.setFeedback(feedback);
			} else {
				feedback = verification.getFeedback();
			}
			feedback.setInfo(api.getEnvironmentInfo());
			feedback.setVersion(api.getAppConfig().getApplicationVersion());
			return toHttpResponse(verification);
		} else {
			return null;
		}
	}

	private HttpResponse toHttpResponse(final Execution<?> respObj) {
		if (respObj.isSuccess()) {
			return new HttpResponse(GsonHelper.toJson(respObj), "application/json;charset=UTF-8", HttpStatus.OK);
		} else {
			return new HttpResponse(GsonHelper.toJson(respObj), "application/json;charset=UTF-8", HttpStatus.ERROR);
		}
	}

	private SessionValue getSessionValue(HttpRequest req) {
    return new SessionValue(req.getHeader("OBSP-Session-ID"), req.getHeader("OBSP-Session-Signature"));
  }
}
