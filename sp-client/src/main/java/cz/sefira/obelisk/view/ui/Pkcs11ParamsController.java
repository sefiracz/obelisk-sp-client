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
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.model.Pkcs11Params;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.view.core.ExtensionFilter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import cz.sefira.obelisk.api.model.OS;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Pkcs11ParamsController extends AbstractUIOperationController<Pkcs11Params> implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(Pkcs11ParamsController.class.getName());

    private static final OS OS = EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs();

    @FXML
    private Button ok;

    @FXML
    private VBox librarySelection;

    @FXML
    private Button cancel;

    @FXML
    private Button selectFile;

    private File pkcs11File;
    private final BooleanProperty pkcs11FileSpecified;

    public Pkcs11ParamsController() {
        this.pkcs11FileSpecified = new SimpleBooleanProperty(false);
    }

    @Override
    public void init(final Object... params) {
        StageHelper.getInstance().setTitle(AppConfig.get().getApplicationName(), "pkcs11.params.title");
        setLogoBackground(librarySelection, 250, 250);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.ok.setOnAction(event -> {
            final Pkcs11Params result = new Pkcs11Params();
            result.setPkcs11Lib(this.pkcs11File);
            this.signalEnd(result);
        });
        this.ok.disableProperty().bind(Bindings.not(this.pkcs11FileSpecified));
        this.cancel.setOnAction(e -> this.signalUserCancel());
        this.selectFile.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles() && db.getFiles() != null && db.getFiles().size() == 1) {
                e.acceptTransferModes(TransferMode.ANY);
            } else {
                e.consume();
            }
        });
        this.selectFile.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles() && db.getFiles() != null && db.getFiles().size() == 1) {
                processPkcs11File(db.getFiles().get(0));
            }
            e.setDropCompleted(success);
            e.consume();
        });
        this.selectFile.setOnAction(e -> {
            File selectedPkcs11File = this.getDisplay().displayFileChooser(
                new ExtensionFilter(OS.getNativeLibraryFileExtensionDescription(), OS.getNativeLibraryFileExtension()),
                new ExtensionFilter("All files", "*")
            );
            processPkcs11File(selectedPkcs11File);
        });
    }

    private void processPkcs11File(File selectedPkcs11File) {
        if (selectedPkcs11File != null) {
            logger.info("Selected PKCS11 driver file: "+selectedPkcs11File.getAbsolutePath());
            pkcs11File = selectedPkcs11File;
            pkcs11FileSpecified.set(true);
            selectFile.setText(pkcs11File.getName());
        }
    }

}
