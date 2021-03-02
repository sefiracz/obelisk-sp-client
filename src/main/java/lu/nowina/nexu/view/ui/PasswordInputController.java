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
package lu.nowina.nexu.view.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import lu.nowina.nexu.api.AbstractProduct;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.core.AbstractUIOperationController;
import org.apache.commons.lang.StringUtils;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class PasswordInputController extends AbstractUIOperationController<char[]> implements Initializable {

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
    StageHelper.getInstance().setTitle((String) params[1], "password.title");
		if(params.length > 2) {
			product = (AbstractProduct) params[2];
		}
		if (product != null) {
			this.passwordPrompt.setText(MessageFormat.format(resources.getString("password.default.prompt"),
              product.getSimpleLabel()));
		} else if (passwordPrompt != null) {
      this.passwordPrompt.setText(passwordPrompt);
    } else {
			this.passwordPrompt.setText(resources.getString("password.title"));
		}

	}
}
