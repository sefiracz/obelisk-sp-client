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
package cz.sefira.obelisk.view.ui;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.model.EnvironmentInfo;
import cz.sefira.obelisk.api.model.Feedback;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.util.TextUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class ProvideFeedbackController extends AbstractFeedbackUIOperationController implements Initializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProvideFeedbackController.class);

	@FXML
  private HBox btnContainer;

	@FXML
	private Button report;

	@FXML
	private Button cancel;

	@FXML
	private Label message;

	private ResourceBundle resources;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
    this.resources = ResourceUtils.getBundle();
		report.setOnAction(e -> {
			new Thread(() -> {
				try {
					// subject
					String subject = MessageFormat.format(resources.getString("feedback.mail.subject"), getApplicationName());
					// body
          StringBuilder body = new StringBuilder();
          body.append(resources.getString("feedback.mail.body.diagnostic.data")).append("\n\n");

          // environment info
          body.append(resources.getString("feedback.mail.body.envInfo")).append("\n");
          body.append("App version: ").append(AppConfig.get().getApplicationVersion()).append("\n");
          body.append(EnvironmentInfo.buildDiagnosticEnvInfo()).append("\n");

          // initialized pkcs11 modules
          body.append(resources.getString("feedback.mail.body.pkcs11.modules")).append("\n");
          List<String> initModules = getApi().getPKCS11Manager().getInitializedModules();
          for(String module : initModules) {
            body.append(module).append("\n");
          }
          body.append("\n");

          // error stackstrace
          String stackTrace = TextUtils.printException(getException());
          body.append(resources.getString("feedback.mail.body.stacktrace")).append("\n");
					body.append(stackTrace).append("\n");

					// mailto
					String uriStr = String.format("mailto:%s?subject=%s&body=%s",
							getAppConfig().getTicketUrl(),
							urlEncode(subject),
							urlEncode(body.toString()));
					Desktop.getDesktop().browse(new URI(uriStr));
				} catch (IOException | URISyntaxException ioe) {
					LOGGER.error(ioe.getMessage());
				}
			}).start();
			signalEnd(null);
		});
		cancel.setOnAction(e -> signalUserCancel());
	}

	@Override
	protected void doInit(Object... params) {
    StageHelper.getInstance().setTitle(getApplicationName(), "feedback.title");
    Platform.runLater(() -> {
      message.setText(MessageFormat.format(resources.getString("feedback.message"),
              resources.getString("button.report.incident"), getApplicationName()));
      btnContainer.getChildren().removeIf(b -> !getAppConfig().isEnableIncidentReport() && b.getId().equals("report"));
    });
  }

	private String urlEncode(String str) {
		return URLEncoder.encode(str, StandardCharsets.UTF_8).replace("+", "%20");
	}

}
