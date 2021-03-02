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
package lu.nowina.nexu.jetty;

import freemarker.template.Configuration;
import lu.nowina.nexu.TechnicalException;
import lu.nowina.nexu.api.EnvironmentInfo;
import lu.nowina.nexu.api.Execution;
import lu.nowina.nexu.api.Feedback;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.plugin.HttpPlugin;
import lu.nowina.nexu.api.plugin.HttpResponse;
import lu.nowina.nexu.api.plugin.HttpStatus;
import lu.nowina.nexu.json.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class RequestProcessor extends AbstractHandler {

	private static final Logger logger = LoggerFactory.getLogger(RequestProcessor.class.getName());

	private static final String UTF8 = "UTF-8";

	private static final String TEXT_PLAIN = "text/plain";
	private static final String APPLICATION_JSON = "application/json";
	private static final String IMAGE_PNG = "image/png";

	private NexuAPI api;

	private String nexuHostname;

	public RequestProcessor() {
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
    cfg.setClassForTemplateLoading(getClass(), "/");
  }

	public void setConfig(NexuAPI api) {
		this.api = api;
	}

	public void setNexuHostname(String nexuHostname) {
		this.nexuHostname = nexuHostname;
	}

	@Override
	public void handle(String target, Request arg1, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if (!"0:0:0:0:0:0:0:1".equals(request.getRemoteHost()) && !"127.0.0.1".equals(request.getRemoteHost())) {
			logger.warn("Cannot accept request from " + request.getRemoteHost());
			response.setStatus(HttpStatus.ERROR.getHttpCode());
			response.setCharacterEncoding(UTF8);
			response.setContentType(TEXT_PLAIN);
			PrintWriter writer = response.getWriter();
			writer.write("Please connect from localhost");
			writer.close();
			return;
		}

		final String errorMessage = returnNullIfValid(request);
		if(errorMessage != null) {
			logger.warn("Invalid request " + errorMessage);
			response.setStatus(HttpStatus.ERROR.getHttpCode());
			response.setCharacterEncoding(UTF8);
			response.setContentType(TEXT_PLAIN);
			PrintWriter writer = response.getWriter();
			writer.write(errorMessage);
			writer.close();
			return;
		}

		if(api.getAppConfig().isCorsAllowAllOrigins()) {
			response.setHeader("Access-Control-Allow-Origin", "*");
		} else {
			if(api.getAppConfig().getCorsAllowedOrigins().contains(request.getHeader("Origin"))) {
				response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
			} else {
				// No match ==> use first value returned by iterator and log a warning
				logger.warn(request.getHeader("Origin") + " does not match any value in corsAllowedOrigins: "
						+ api.getAppConfig().getCorsAllowedOrigins());
				response.setHeader("Access-Control-Allow-Origin", api.getAppConfig().getCorsAllowedOrigins().iterator().next());
			}
		}
		response.setHeader("Vary", "Origin");
		response.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type");

		if ("OPTIONS".equals(request.getMethod())) {
			response.setStatus(HttpStatus.OK.getHttpCode());
			response.getWriter().close();
			return;
		}
		logger.info("Request " + target);

		try {
			if ("/favicon.ico".equals(target)) {
        favIcon(response);
      } else if ("/".equals(target) || "/clientInfo".equals(target)) {
        clientInfo(response);
			} else {
				httpPlugin(target, request, response);
			}
		} catch(Exception e) {
			logger.error("Cannot process request", e);
			try {
				response.setStatus(HttpStatus.ERROR.getHttpCode());
				response.setCharacterEncoding(UTF8);
				response.setContentType(APPLICATION_JSON);

				final Execution<?> execution = new Execution<Object>(BasicOperationStatus.EXCEPTION);
				final Feedback feedback = new Feedback(e);
				feedback.setVersion(api.getAppConfig().getApplicationVersion());
				feedback.setInfo(api.getEnvironmentInfo());
				execution.setFeedback(feedback);

				final PrintWriter writer = response.getWriter();
				writer.write(GsonHelper.toJson(execution));
				writer.close();
			} catch (IOException e2) {
				logger.error("Cannot write error !?", e2);
			}
		}
	}

  /**
	 * This method checks the validity of the given request.
	 * <p>This implementation returns <code>null</code> by contract.
	 * @param request The request to check.
	 * @return An error message if request is invalid or <code>null</code>
	 * if the request is valid.
	 */
	protected String returnNullIfValid(final HttpServletRequest request) {
		return null;
	}

	private void httpPlugin(String target, HttpServletRequest request, HttpServletResponse response) throws Exception {
		int index = target.indexOf("/", 1);
		String pluginId = target.substring(target.charAt(0) == '/' ? 1 : 0, index);

		logger.info("Process request " + target + " pluginId: " + pluginId);
		HttpPlugin httpPlugin = api.getHttpPlugin(pluginId);

		HttpResponse resp = httpPlugin.process(api, new DelegatedHttpServerRequest(request, '/' + pluginId));
		if (resp == null || resp.getContent() == null) {
			throw new TechnicalException("Plugin responded null");
		} else {
			response.setStatus(resp.getHttpStatus().getHttpCode());
			response.setContentType(resp.getContentType());
			PrintWriter writer = response.getWriter();
			writer.write(resp.getContent());
			writer.close();
		}
	}

	private void clientInfo(HttpServletResponse response) throws IOException {
		response.setCharacterEncoding(UTF8);
		response.setContentType(APPLICATION_JSON);
		response.setHeader("pragma", "no-cache");
		response.setIntHeader("expires", -1);
		PrintWriter writer = response.getWriter();
		EnvironmentInfo info = api.getEnvironmentInfo();
		String infoJson = " \"info\" : { " +
				"\"jreVendor\": \""+info.getJreVendor()+"\"," +
				"\"osName\": \""+info.getOsName()+"\"," +
				"\"osArch\": \""+info.getOsArch()+"\"," +
				"\"osVersion\": \""+info.getOsVersion()+"\"," +
				"\"arch\": \""+info.getArch()+"\"," +
				"\"os\": \""+info.getOs()+"\"" +
				"}";
		writer.write("{ "+infoJson+" , \"version\": \"" + api.getAppConfig().getApplicationVersion() + "\"}");
		writer.close();
	}

	private void favIcon(HttpServletResponse response) throws IOException {
		response.setContentType(IMAGE_PNG);
		InputStream in = this.getClass().getResourceAsStream("/tray-icon.png");
		ServletOutputStream out = response.getOutputStream();
		IOUtils.copy(in, out);
		in.close();
		out.close();
	}

}
