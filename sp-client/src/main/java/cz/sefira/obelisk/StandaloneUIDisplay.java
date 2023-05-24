/**
 * © Nowina Solutions, 2015-2016
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
package cz.sefira.obelisk;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.flow.BasicOperationStatus;
import cz.sefira.obelisk.flow.StageHelper;
import cz.sefira.obelisk.util.ResourceUtils;
import cz.sefira.obelisk.view.core.NonBlockingUIOperation;
import cz.sefira.obelisk.dss.token.PasswordInputCallback;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import cz.sefira.obelisk.api.Product;
import cz.sefira.obelisk.api.ReauthCallback;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.api.flow.OperationResult;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.view.core.ExtensionFilter;
import cz.sefira.obelisk.view.core.UIDisplay;
import cz.sefira.obelisk.view.core.UIOperation;
import org.identityconnectors.common.security.GuardedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ResourceBundle;

/**
 * Implementation of {@link UIDisplay} used for standalone mode.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class StandaloneUIDisplay implements UIDisplay {

	private static final Logger logger = LoggerFactory.getLogger(StandaloneUIDisplay.class.getName());

	private Stage blockingStage;
	private Stage nonBlockingStage;
	private UIOperation<?> currentBlockingOperation;
	private OperationFactory operationFactory;
	private String currentOperationName;

	public StandaloneUIDisplay() {
		this.blockingStage = createStage(true);
		this.nonBlockingStage = createStage(false);
	}

	private void display(final UIOperation<?> operation, boolean blockingOperation) {
		Parent panel = operation.getRoot();
		currentOperationName = operation.getOperationName();
		Platform.runLater(() -> {
			Stage stage = (blockingOperation) ? blockingStage : nonBlockingStage;
			logger.info("Display " + currentOperationName);
			if (!stage.isShowing()) {
				if(blockingOperation) {
					stage = blockingStage = createStage(true);
				} else {
					stage = nonBlockingStage = createStage(false);
				}
				logger.info("Loading "+(blockingOperation?"":"non-")+"blocking UI "
						+ currentOperationName + " using new Stage " + stage);
			} else {
				logger.info("Stage still showing, displaying " + currentOperationName);
			}
			final Scene scene = new Scene(panel);
			scene.getStylesheets().add(this.getClass().getResource("/styles/nexu.css").toString());
			stage.setScene(scene);
			StageHelper.getInstance().setMinSize(((Region)panel), stage);
			// center stage on primary screen
			centerStage(stage, ((Region)panel));
			stage.setTitle(StageHelper.getInstance().getTitle());
			stage.show();
			StageHelper.getInstance().setTitle("", null);
		});
	}

	private void centerStage(Stage stage, Region r){
		final Rectangle2D screenResolution = Screen.getPrimary().getBounds();
		double width = r.getPrefWidth() <= 0 ?
				(r.getMinWidth() <= 0 ?
						(r.getMaxWidth() <= 0 ?
								(r.getWidth() <= 0 ? 400 : r.getWidth())
								: r.getMaxWidth()) : r.getMinWidth()) : r.getPrefWidth();
		double height = r.getPrefHeight() <= 0 ?
				(r.getMinHeight() <= 0 ?
						(r.getMaxHeight() <= 0 ?
								(r.getHeight() <= 0 ? 400 : r.getHeight())
								: r.getMaxHeight()) : r.getMinHeight()) : r.getPrefHeight();
		if(width > 0) {
			double x = (screenResolution.getWidth() / 2) - (width / 2);
			if (x >= 0) {
				stage.setX(x);
			}
		}
		if (height > 0) {
			double y = (screenResolution.getHeight() / 2) - (height / 2);
			if (y >= 0) {
				stage.setY(y);
			}
		}
	}

	private Stage createStage(final boolean blockingStage) {
		final Stage newStage = new Stage();
		newStage.getIcons().add(new Image(AppConfig.get().getIconLogoStream()));
		newStage.setAlwaysOnTop(true);
		newStage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
			if (event.getCode() == KeyCode.ESCAPE) {
				logger.info("Closing window '"+newStage.getTitle()+"'");
				newStage.close();
				if (blockingStage && (currentBlockingOperation != null)) {
					currentBlockingOperation.signalUserCancel();
				}
			}
		});
		newStage.setOnCloseRequest((e) -> {
			logger.info("Closing window '"+newStage.getTitle()+"'");
			newStage.hide();
			e.consume();

			if (blockingStage && (currentBlockingOperation != null)) {
				currentBlockingOperation.signalUserCancel();
			}
		});
		return newStage;
	}

	@Override
	public void close(final boolean blockingOperation) {
		Platform.runLater(() -> {
			Stage oldStage = (blockingOperation) ? blockingStage : nonBlockingStage;
			if (currentOperationName != null) {
				logger.info("Hide " + currentOperationName + " using " + oldStage + " and create new stage");
			}
			if (blockingOperation) {
				blockingStage = createStage(true);
			} else {
				nonBlockingStage = createStage(false);
			}
			oldStage.close();
			currentOperationName = null;
		});

	}

	@Override
	public void display(NonBlockingUIOperation operation) {
		display(operation, false);
	}

	public <T> void displayAndWaitUIOperation(final UIOperation<T> operation) {
		display(operation, true);
		waitForUser(operation);
	}

	private <T> void waitForUser(UIOperation<T> operation) {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Wait on Thread " + Thread.currentThread().getName());
			currentBlockingOperation = operation;
			operation.waitEnd();
			currentBlockingOperation = null;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public Stage getStage(boolean blockingOperation) {
		return blockingOperation ? blockingStage : nonBlockingStage;
	}

	private final class FlowPasswordCallback implements PasswordInputCallback {

    private final Product product;
		private final String passwordPrompt;

		public FlowPasswordCallback(Product product) {
		  this.product = product;
      this.passwordPrompt = null;
		}

		@Override
    @SuppressWarnings("unchecked")
		public char[] getPassword() {
			logger.info("Request password");
      final OperationResult<Object> passwordResult = StandaloneUIDisplay.this.operationFactory.getOperation(
              UIOperation.class, "/fxml/password-input.fxml", passwordPrompt, product).perform();
      if(passwordResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
        return (char[]) passwordResult.getResult(); // get password
      } else if(passwordResult.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
        throw new CancelledOperationException();
      } else if(passwordResult.getStatus().equals(BasicOperationStatus.EXCEPTION)) {
        final Exception e = passwordResult.getException();
        if(e instanceof RuntimeException) {
          // Throw exception as is
          throw (RuntimeException) e;
        } else {
          // Wrap in a runtime exception
          throw new AppException(e);
        }
      } else {
        throw new IllegalArgumentException("Not managed operation status: " + passwordResult.getStatus().getCode());
      }
		}

	}

	private final class Pkcs11ReauthCallback implements ReauthCallback {

		@Override
		@SuppressWarnings("unchecked")
		public GuardedString getReauth() {
			logger.info("Request PKCS11 re-auth");
			// check if cached
			try {
				GuardedString reauth = SessionManager.getManager().getSecret();
				if (reauth != null)
					return reauth.copy();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
			// ask user for re-auth
			final OperationResult<Object> reauthOperationResult = StandaloneUIDisplay.this.operationFactory.getOperation(
					UIOperation.class, "/fxml/reauth-input.fxml", AppConfig.get()).perform();
			if(reauthOperationResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
				return (GuardedString) reauthOperationResult.getResult();
			} else if(reauthOperationResult.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
				throw new CancelledOperationException();
			} else if(reauthOperationResult.getStatus().equals(BasicOperationStatus.EXCEPTION)) {
				final Exception e = reauthOperationResult.getException();
				if(e instanceof RuntimeException) {
					// Throw exception as is
					throw (RuntimeException) e;
				} else {
					// Wrap in a runtime exception
					throw new AppException(e);
				}
			} else {
				throw new IllegalArgumentException("Not managed operation status: " + reauthOperationResult.getStatus().getCode());
			}
		}
	}

	@Override
	public PasswordInputCallback getPasswordInputCallback(Product product) {
		return new FlowPasswordCallback(product);
	}

	@Override
	public ReauthCallback getReauthCallback() {
		return new Pkcs11ReauthCallback();
	}


	@Override
	public File displayFileChooser(ExtensionFilter... extensionFilters) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(ResourceUtils.getBundle().getString("fileChooser.title.openResourceFile"));
		fileChooser.getExtensionFilters().addAll(toJavaFXExtensionFilters(extensionFilters));
		return fileChooser.showOpenDialog(blockingStage);
	}

	private FileChooser.ExtensionFilter[] toJavaFXExtensionFilters(ExtensionFilter... extensionFilters) {
		final FileChooser.ExtensionFilter[] result = new FileChooser.ExtensionFilter[extensionFilters.length];
		int i = 0;
		for(final ExtensionFilter extensionFilter : extensionFilters) {
			result[i++] = new FileChooser.ExtensionFilter(extensionFilter.getDescription(), extensionFilter.getExtensions());
		}
		return result;
	}

	public void setOperationFactory(final OperationFactory operationFactory) {
		this.operationFactory = operationFactory;
	}

}
