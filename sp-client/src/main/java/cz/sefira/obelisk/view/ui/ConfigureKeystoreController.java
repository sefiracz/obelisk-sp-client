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

import cz.sefira.obelisk.AppException;
import cz.sefira.obelisk.token.keystore.ConfiguredKeystore;
import cz.sefira.obelisk.api.model.KeystoreType;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.view.core.AbstractUIOperationController;
import cz.sefira.obelisk.view.core.ExtensionFilter;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ConfigureKeystoreController extends AbstractUIOperationController<ConfiguredKeystore> implements Initializable {

	@FXML
	private VBox keystoreSelection;

	@FXML
	private Button ok;

	@FXML
	private Button cancel;

	@FXML
	private Button selectFile;

	private KeystoreType keystoreType;
	private File keystoreFile;
	private final BooleanProperty keystoreFileSpecified;

	public ConfigureKeystoreController() {
		keystoreFileSpecified = new SimpleBooleanProperty(false);
	}

	@Override
	public void init(Object... params) {
		StageHelper.getInstance().setTitle((String)params[0], "save.keystore.title");
		setLogoBackground(keystoreSelection, 250, 250);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		ok.setOnAction((event) -> {
			final ConfiguredKeystore result = new ConfiguredKeystore();
			try {
				result.setUrl(keystoreFile.toURI().toURL().toString());
			} catch (Exception e1) {
				throw new AppException(e1);
			}
			result.setType(keystoreType);
			signalEnd(result);
		});
		ok.disableProperty().bind(Bindings.not(keystoreFileSpecified));
		cancel.setOnAction(e -> signalUserCancel());
		selectFile.setOnAction(e -> {
			keystoreFile = getDisplay().displayFileChooser(
					new ExtensionFilter("KeyStore files", "*.p12", "*.pfx", "*.P12", "*.PFX", "*.jks", "*.JKS", "*.jceks" ,"*.JCEKS"),
					new ExtensionFilter("All files", "*")
			);
			if (keystoreFile != null) {
        keystoreType = whichKeystoreType(keystoreFile);
        selectFile.setText(keystoreFile.getName());
        keystoreFileSpecified.set(!keystoreType.equals(KeystoreType.UNKNOWN));
      }
		});
	}

	private KeystoreType whichKeystoreType(File keystoreFile){
		try {
			byte[] bytes = FileUtils.readFileToByteArray(keystoreFile);
			if(bytes.length < 4) {
				return KeystoreType.UNKNOWN;
			} else if (bytes[0] == (byte)0xFE && bytes[1] == (byte)0xED &&
					bytes[2] == (byte)0xFE && bytes[3] == (byte)0xED) {
				// JKS - 0xFEEDFEED
				return KeystoreType.JKS;
			} else if(bytes[0] == (byte)0xCE && bytes[1] == (byte)0xCE &&
					bytes[2] == (byte)0xCE && bytes[3] == (byte)0xCE) {
				// JCEKS - 0xCECECECE
				return KeystoreType.JCEKS;
			} else {
				// PKCS12 - ASN1 structure check
				ASN1Primitive pfx = ASN1Primitive.fromByteArray(bytes);
				if (pfx instanceof ASN1Sequence) {
					ASN1Sequence sequence = (ASN1Sequence) pfx;
					if ((sequence.size() == 2) || (sequence.size() == 3)) {
						ASN1Encodable firstComponent = sequence.getObjectAt(0);
						if (firstComponent instanceof ASN1Integer) {
							ASN1Integer version = (ASN1Integer) firstComponent;
							if (version.getValue().intValue() == 3) {
								return KeystoreType.PKCS12;
							}
						}
					}
				}
			}
		} catch (IOException e) {
			return KeystoreType.UNKNOWN;
		}
		return KeystoreType.UNKNOWN;
	}

}
