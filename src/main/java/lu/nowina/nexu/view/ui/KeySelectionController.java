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

import eu.europa.esig.dss.DSSASN1Utils;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.QCStatementOids;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.x509.CertificateToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lu.nowina.nexu.Utils;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.flow.operation.CoreOperationStatus;
import lu.nowina.nexu.view.core.AbstractUIOperationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class KeySelectionController extends AbstractUIOperationController<DSSPrivateKeyEntry> implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(KeySelectionController.class.getName());

    private static final String ICON_UNLOCKED = "/images/key.png";
    private static final String ICON_QC = "/images/qc.png";
    private static final String ICON_QCSD = "/images/qscd.png";

    @FXML
    private Button select;

    @FXML
    private Button cancel;

    @FXML
    private Button back;

    @FXML
    private ListView<DSSPrivateKeyEntry> listView;

    private ResourceBundle resources;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;
        this.select.disableProperty().bind(this.listView.getSelectionModel().selectedItemProperty().isNull());
        this.select.setOnAction((event) -> {
            final DSSPrivateKeyEntry selectedItem = this.listView.getSelectionModel().getSelectedItem();
            logger.info("Selected item " + selectedItem);
            this.signalEnd(selectedItem);
        });
        this.cancel.setOnAction(e -> this.signalUserCancel());
        this.back.setOnAction(e -> this.signalEndWithStatus(CoreOperationStatus.BACK));

        this.listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.listView.setCellFactory(param -> {
            return new ListCell<DSSPrivateKeyEntry>() {

                @Override
                protected void updateItem(final DSSPrivateKeyEntry k, final boolean bln) {
                    super.updateItem(k, bln);
                    if (k != null) {
                        final CertificateToken certificateToken = k.getCertificate();

                        final Label lSubject = new Label();
                        lSubject.setText(DSSASN1Utils.getSubjectCommonName(certificateToken));
                        lSubject.setStyle("-fx-font-weight: bold;");

                        final Label lIssuer = new Label();
                        lIssuer.setText(MessageFormat.format(resources.getString("key.selection.issuer"),
                                DSSASN1Utils.get(certificateToken.getIssuerX500Principal()).get("2.5.4.3")));

                        final Label lUsage = new Label();
                        lUsage.setText(MessageFormat.format(resources.getString("key.selection.keyusage"),
                                KeySelectionController.this.createKeyUsageString(certificateToken, resources)));

                        final Label lValidity = new Label();
                        final SimpleDateFormat format = new SimpleDateFormat("dd MMMMMM yyyy");
                        final String startDate = format.format(certificateToken.getNotBefore());
                        final String endDate = format.format(certificateToken.getNotAfter());
                        lValidity.setText(
                                MessageFormat.format(resources.getString("key.selection.validity"), startDate, endDate));

                        final Hyperlink link = new Hyperlink(resources.getString("key.selection.certificate.open"));

                        link.setOnAction(actionEvent -> Utils.openCertificate(DSSUtils.convertToPEM(certificateToken)));

                        final VBox vBox = new VBox(lSubject, lIssuer, lUsage, lValidity, link);

                        VBox vBoxLeft;
                        try {
                            vBoxLeft = new VBox(KeySelectionController.this.getQCIcons(certificateToken).stream().toArray(ImageView[]::new));
                        } catch (final IOException e) {
                            logger.error(e.getMessage(), e);
                            vBoxLeft = new VBox();
                        }
                        vBoxLeft.setPadding(new Insets(0, 10, 0, 0));
                        vBoxLeft.setAlignment(Pos.CENTER);

                        final HBox hBox = new HBox(vBoxLeft, vBox);
                        this.setGraphic(hBox);

                        Tooltip tooltip = new Tooltip(getQCInfo(certificateToken));
                        hackTooltipStartTiming(tooltip, 100);
                        this.setTooltip(tooltip);
                    }
                }

            };
        });
    }

    private List<ImageView> getQCIcons(final CertificateToken certificateToken) throws IOException {
        final List<ImageView> qcIconsImages = new ArrayList<>();
        final List<String> qcStatements = DSSASN1Utils.getQCStatementsIdList(certificateToken);
        if (qcStatements.contains(QCStatementOids.QC_COMPLIANCE.getOid())) {
            qcIconsImages.add(this.fetchImage(ICON_QC));
        }
        if (qcStatements.contains(QCStatementOids.QC_SSCD.getOid())) {
            qcIconsImages.add(this.fetchImage(ICON_QCSD));
        }
        if (qcIconsImages.isEmpty()) {
            qcIconsImages.add(this.fetchImage(ICON_UNLOCKED));
        }
        return qcIconsImages;
    }

    private String getQCInfo(final CertificateToken certificateToken) {
        String tooltip = MessageFormat.format(resources.getString("certificates.status.noqc"), new Object[]{});
        final List<String> qcStatements = DSSASN1Utils.getQCStatementsIdList(certificateToken);
        if (qcStatements.contains(QCStatementOids.QC_COMPLIANCE.getOid())) {
            tooltip = MessageFormat.format(resources.getString("certificates.status.qc"), new Object[]{});
        }
        if (qcStatements.contains(QCStatementOids.QC_SSCD.getOid())) {
            tooltip = MessageFormat.format(resources.getString("certificates.status.qscd"), new Object[]{});
        }
        return tooltip;
    }

    /* javafx 8 tooltip delay workaround */
    private void hackTooltipStartTiming(Tooltip tooltip, double delayMillis) {
        try {
            Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
            fieldBehavior.setAccessible(true);
            Object objBehavior = fieldBehavior.get(tooltip);

            Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
            fieldTimer.setAccessible(true);
            Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

            objTimer.getKeyFrames().clear();
            objTimer.getKeyFrames().add(new KeyFrame(Duration.millis(delayMillis)));
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }


    private ImageView fetchImage(final String imagePath) throws IOException {
        return new ImageView(new Image(this.getClass().getResource(imagePath).openStream()));
    }

    private String createKeyUsageString(final CertificateToken token, final ResourceBundle resources) {
        final boolean[] keyUsages = token.getCertificate().getKeyUsage();
        if (keyUsages == null) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        final List<String> keyUsageList = new ArrayList<>();
        if (keyUsages[0]) {
          keyUsageList.add(resources.getString("keyUsage.digitalSignature"));
        }
        if (keyUsages[1]) {
          keyUsageList.add(resources.getString("keyUsage.nonRepudiation"));
        }
        if (keyUsages[2]) {
          keyUsageList.add(resources.getString("keyUsage.keyEncipherment"));
        }
        if (keyUsages[3]) {
          keyUsageList.add(resources.getString("keyUsage.dataEncipherment"));
        }
        if (keyUsages[4]) {
          keyUsageList.add(resources.getString("keyUsage.keyAgreement"));
        }
        if (keyUsages[5]) {
          keyUsageList.add(resources.getString("keyUsage.keyCertSign"));
        }
        if (keyUsages[6]) {
          keyUsageList.add(resources.getString("keyUsage.crlSign"));
        }
        if (keyUsages[7]) {
          keyUsageList.add(resources.getString("keyUsage.encipherOnly"));
        }
        if (keyUsages[8]) {
          keyUsageList.add(resources.getString("keyUsage.decipherOnly"));
        }
        // comma separated list
        for(int i=0; i<keyUsageList.size(); i++) {
          builder.append(keyUsageList.get(i));
          if(i + 1 < keyUsageList.size()) {
            builder.append(", ");
          }
        }

        return builder.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(final Object... params) {
        StageHelper.getInstance().setTitle((String) params[1], "key.selection.title");
        final Boolean displayBackButton = (Boolean) params[2];
        this.back.setManaged(displayBackButton);
        this.back.setVisible(displayBackButton);
        final List<DSSPrivateKeyEntry> keys = (List<DSSPrivateKeyEntry>) params[0];
        final ObservableList<DSSPrivateKeyEntry> items = FXCollections.observableArrayList(keys);
        this.listView.setPlaceholder(new Label(MessageFormat.format(resources.getString("key.selection.empty"),
            new Object[]{})));
        this.listView.setItems(items);
        if(items.isEmpty()) {
          this.listView.setPrefHeight(50);
        } else if(items.size()<=5) {
        	this.listView.setPrefHeight(100.0*items.size());
        } else {
        	this.listView.setPrefHeight(500);
        }

    }

}
