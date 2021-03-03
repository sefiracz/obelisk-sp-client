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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.DialogMessage;
import lu.nowina.nexu.view.core.AbstractUIOperationController;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class MessageController extends AbstractUIOperationController<Void> implements Initializable {

	@FXML
	private BorderPane messagePane;

	@FXML
  private VBox iconBox;

	@FXML
  private Region icon;

	@FXML
	private Label message;

	@FXML
  private VBox bottomContainer;

	@FXML
	private HBox btnContainer;

  @FXML
  private VBox doNotShowContainer;

	@FXML
	private Button ok;

	private ResourceBundle resources;
	private boolean hideCheckbox;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (ok != null) {
			ok.setOnAction(e -> {
        if(hideCheckbox) {
          // TODO - nastavit zobrazovani dialogu do konfigurace

        }
			  signalEnd(null);
			});
		}
		this.resources = resources;
	}

	@Override
	public void init(Object... params) {
    NexuAPI api = (NexuAPI) params[0];
    DialogMessage dialogMessage = (DialogMessage) params[1];
    // set title
    StageHelper.getInstance().setTitle(api.getAppConfig().getApplicationName(), dialogMessage.getLevel().getTitleCode());

    Platform.runLater(() -> {
      // set message
      if (dialogMessage.getMessageProperty() != null) {
        // message from property
        String messageText = MessageFormat.format(resources.getString(dialogMessage.getMessageProperty()),
                dialogMessage.getMessageParameters());
        message.setText(messageText);
      } else if (dialogMessage.getMessage() != null) {
        // pre-set message
        message.setText(dialogMessage.getMessage());
      } else {
        // default value
        message.setText(resources.getString("error"));
      }

      // set size
      this.messagePane.setPrefSize(dialogMessage.getWidth(), dialogMessage.getHeight());

      // set dialog icon
      switch (dialogMessage.getLevel()) {
        case INFORMATION:
          icon.getStyleClass().add("icon-information");
          icon.setPrefSize(50, 50);
          break;
        case WARNING:
          icon.getStyleClass().add("icon-warning");
          icon.setPrefSize(54, 50);
          break;
        case ERROR:
          icon.getStyleClass().add("icon-error");
          icon.setPrefSize(50, 50);
          break;
        default:
          iconBox.getChildren().removeAll(); // no icon
      }

      // add additional buttons
      int position = 0;
      for(Button b : dialogMessage.getButtons()) {
        btnContainer.getChildren().add(position, b);
        position++;
      }

      // remove ok button if not needed
      if(!dialogMessage.isShowOkButton()) {
        btnContainer.getChildren().remove(ok);
      }

      // hide do not show checkbox
      if(!dialogMessage.isShowDoNotShowCheckbox()) {
        hideCheckbox = bottomContainer.getChildren().remove(doNotShowContainer);
      }
    });

	}

}
