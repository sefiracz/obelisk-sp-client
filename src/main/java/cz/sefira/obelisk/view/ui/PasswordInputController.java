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

import cz.sefira.obelisk.api.AbstractProduct;
import cz.sefira.obelisk.api.ConfiguredKeystore;
import cz.sefira.obelisk.api.DetectedCard;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class PasswordInputController extends AbstractUIOperationController<char[]> implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(PasswordInputController.class.getName());

	@FXML
	private Region icon;

	@FXML
	private Button ok;

	@FXML
	private Button cancel;

	@FXML
	private Label passwordPrompt;

	@FXML
	private PasswordField password;

	private AbstractProduct product;

	private ResourceBundle resources;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		EventHandler<ActionEvent> handler = event -> signalEnd(password.getText().toCharArray());
		ok.setOnAction(handler);
		password.setOnAction(handler);
		cancel.setOnAction(e -> signalUserCancel());
		this.resources = resources;
	}

	@Override
	public void init(Object... params) {
    final String passwordPrompt = (String) params[0];
		if(params.length > 2) {
			product = (AbstractProduct) params[2];
		}
		String titleKey = "password.title";
    String promptKey = "password.default.prompt";
    if (passwordPrompt != null) {
      this.passwordPrompt.setText(passwordPrompt);
    } else if (product != null) {
    	String label = product.getSimpleLabel();
      if(product instanceof DetectedCard) {
        titleKey = "password.title.pin";
        promptKey = "password.smartcard.prompt";
      }
      if (product instanceof ConfiguredKeystore) {
				try {
					label = Paths.get(new URI(((ConfiguredKeystore) product).getUrl())).getFileName().toString();
					label += " ("+product.getType().getLabel()+")";
				}
				catch (URISyntaxException e) {
					logger.error(e.getMessage(), e);
				}
			}
			icon.getStyleClass().add("icon-lock");
			icon.setPrefSize(40, 50);

      this.passwordPrompt.setText(MessageFormat.format(resources.getString(promptKey), label));
    } else {
			this.passwordPrompt.setText(resources.getString(titleKey));
		}
    StageHelper.getInstance().setTitle((String) params[1], titleKey);
	}
}
