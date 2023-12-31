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

import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.flow.operation.CoreOperationStatus;
import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.apache.commons.lang.StringEscapeUtils;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Return true if the user want to try "Advance mode"
 *
 * @author David Naramski
 *
 */
public class UnsupportedProductController extends AbstractUIOperationController<Void> implements Initializable {

    @FXML
    private Label message;

    @FXML
    private VBox messageBox;

    @FXML
    private Button back;

    @FXML
    private Button hicSuntDracones;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.hicSuntDracones.setOnAction(ev -> this.signalEnd(null));
        this.back.setOnAction(e -> this.signalEndWithStatus(CoreOperationStatus.BACK));
    }

    @Override
    public final void init(final Object... params) {
        StageHelper.getInstance().setTitle((String) params[0], "unsupported.product.title");

        Platform.runLater(() -> this.message.setText(StringEscapeUtils.unescapeJava(MessageFormat
                .format(ResourceUtils.getBundle().getString("unsupported.product.header"), params[0]))));

        setLogoBackground(messageBox);
    }
}
