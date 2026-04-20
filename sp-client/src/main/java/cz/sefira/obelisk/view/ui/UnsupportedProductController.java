/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 * <p>
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 * <p>
 * http://ec.europa.eu/idabc/eupl5
 * <p>
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package cz.sefira.obelisk.view.ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.flow.operation.CoreOperationStatus;
import cz.sefira.obelisk.token.pkcs11.DetectedCard;
import cz.sefira.obelisk.util.DesktopUtils;
import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.util.TextUtils;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Return true if the user want to try "Advance mode"
 *
 * @author David Naramski
 *
 */
public class UnsupportedProductController extends AbstractUIOperationController<Void> implements Initializable {

  private static final Logger logger = LoggerFactory.getLogger(UnsupportedProductController.class.getName());
  private static final int QR_CODE_WIDTH = 250;
  private static final int QR_CODE_HEIGHT = 250;

  @FXML
  private VBox messageBox;

  @FXML
  private TextFlow message;

  @FXML
  private TextArea details;

  @FXML
  private ImageView qrcode;

  @FXML
  private Button advancedSetup;

  @FXML
  private Button back;

  @FXML
  private Button cancel;

  @FXML
  private Button support;

  private PlatformAPI api;
  private DetectedCard card;
  private QRCodeWriter qrCodeWriter;

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.advancedSetup.setOnAction(ev -> this.signalEnd(null));
    this.back.setOnAction(e -> this.signalEndWithStatus(CoreOperationStatus.BACK));
    this.cancel.setOnAction(e -> this.signalUserCancel());
    this.support.disableProperty().bind(Bindings.isEmpty(details.textProperty()));
    this.support.setOnAction(e -> {
      // mailto
      try {
        logger.info("User clicked to send an unknown device report");
        String subject = MessageFormat.format(resources.getString("unsupported.mail.subject"),
            AppConfig.get().getApplicationName(), card.getAtr());
        String body = details.getText();
        String uriStr = String.format("mailto:%s?subject=%s&body=%s",
            AppConfig.get().getTicketUrl(),
            TextUtils.urlEncode(subject),
            TextUtils.urlEncode(body));
        DesktopUtils.browse(uriStr);
      } catch (Exception ex) {
        DialogMessage errMsg = new DialogMessage("feedback.message", DialogMessage.Level.ERROR, 380, 140);
        errMsg.setOwner(getDisplay().getStage(true));
        StandaloneDialog.showErrorDialog(errMsg, null, ex);
        logger.error("Send device report failed: "+ex.getMessage(), ex);
      }
    });
    qrCodeWriter = new QRCodeWriter();
    qrcode.setImage(placeholder());
  }

  @Override
  public final void init(final Object... params) {
    StageHelper.getInstance().setTitle(AppConfig.get().getApplicationName(), "unsupported.product.title");

    api = (PlatformAPI) params[0];
    card = (DetectedCard) params[1];

    Platform.runLater(() -> {
      String copy = ResourceUtils.getBundle().getString("clipboard.copy");
      String copied = ResourceUtils.getBundle().getString("clipboard.copied");
      Text mail = new Text(" " + AppConfig.get().getTicketUrl() + " ");
      mail.setStyle("-fx-font-weight: bold; -fx-font-size: 14px");
      Tooltip tooltip = new Tooltip();
      tooltip.setShowDelay(new Duration(50));
      tooltip.setText(copy);
      Tooltip.install(mail, tooltip);

      mail.setOnMouseExited(event -> {
        tooltip.hide();
        tooltip.setText(copy);
      });

      mail.setOnMouseClicked(e -> {
        StringSelection selection = new StringSelection(AppConfig.get().getTicketUrl());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        tooltip.setText(copied);
        tooltip.show(mail,e.getScreenX() + 10, e.getScreenY() + 10);
      });

      this.message.getChildren().add(new Text(StringEscapeUtils.unescapeJava(MessageFormat
          .format(ResourceUtils.getBundle().getString("unsupported.product.message.1"),
              AppConfig.get().getApplicationName()))));
      this.message.getChildren().add(mail);
      this.message.getChildren().add(new Text(ResourceUtils.getBundle().getString("unsupported.product.message.2")));

      String atr = card.getAtr(); // unknown device's ATR

      // list known installed drivers on user's computer
      Set<String> installed = api.getPKCS11Manager().getInstalledDrivers();
      String installedDrivers = "";
      if (!installed.isEmpty()) {
        installedDrivers = ResourceUtils.getBundle().getString("unsupported.known.drivers") + ": \n";
        StringBuilder installedList = new StringBuilder(installedDrivers);
        for (String driver : installed) {
          installedList.append(driver).append("\n");
        }
        installedList.append("\n");
        installedDrivers = installedList.toString();
      }

      String message = MessageFormat.format(ResourceUtils.getBundle().getString("unsupported.mail.body.template"),
          atr, installedDrivers);

      this.details.textProperty().addListener((observable, oldValue, newValue) -> {
        Image image = generateQRCode(newValue);
        if (image != null) {
          qrcode.setImage(image);
        }
      });
      this.details.setText(message);
    });

    setLogoBackground(messageBox);
  }

  private Image generateQRCode(String text) {
    try {
      BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
        return new Image(new ByteArrayInputStream(out.toByteArray()));
      }
    } catch (Exception e) {
      logger.error("Error while generating QR code: "+e.getMessage());
      return null;
    }
  }

  private Image placeholder() {
    WritableImage placeholder = new WritableImage(QR_CODE_WIDTH, QR_CODE_HEIGHT);
    PixelWriter pw = placeholder.getPixelWriter();
    for (int x = 0; x < QR_CODE_WIDTH; x++) {
      for (int y = 0; y < QR_CODE_HEIGHT; y++) {
        Color c = ((x / (25)) + (y / 25)) % 2 == 0 ? Color.MAGENTA : Color.BLACK;
        pw.setColor(x, y, c);
      }
    }
    return placeholder;
  }
}
