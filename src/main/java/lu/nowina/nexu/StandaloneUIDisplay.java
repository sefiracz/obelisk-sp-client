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
package lu.nowina.nexu;

import eu.europa.esig.dss.token.PasswordInputCallback;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lu.nowina.nexu.api.NexuPasswordInputCallback;
import lu.nowina.nexu.api.Product;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.core.ExtensionFilter;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;
import lu.nowina.nexu.view.core.UIDisplay;
import lu.nowina.nexu.view.core.UIOperation;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneUIDisplay.class.getName());

	private Stage blockingStage;
	private Stage nonBlockingStage;
	private UIOperation<?> currentBlockingOperation;
	private OperationFactory operationFactory;

	public StandaloneUIDisplay() {
		this.blockingStage = createStage(true, null);
		this.nonBlockingStage = createStage(false, null);
	}

	private void display(Parent panel, boolean blockingOperation) {
		LOGGER.info("Display " + panel + " in display " + this + " from Thread " + Thread.currentThread().getName());
		Platform.runLater(() -> {
			Stage stage = (blockingOperation) ? blockingStage : nonBlockingStage;
			LOGGER.info("Display " + panel + " in display " + this + " from Thread " + Thread.currentThread().getName());
			if (!stage.isShowing()) {
				if(blockingOperation) {
					stage = blockingStage = createStage(true, null);
				} else {
					stage = nonBlockingStage = createStage(false, null);
				}
				LOGGER.info("Loading ui " + panel + " is a new Stage " + stage);
			} else {
				LOGGER.info("Stage still showing, display " + panel);
			}
			final Scene scene = new Scene(panel);
			scene.getStylesheets().add(this.getClass().getResource("/styles/nexu.css").toString());
			stage.setScene(scene);
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

	private Stage createStage(final boolean blockingStage, String title) {
		final Stage newStage = new Stage();
		newStage.getIcons().add(new Image(StandaloneUIDisplay.class.getResourceAsStream("/tray-icon.png")));
		newStage.setTitle(title);
		newStage.setAlwaysOnTop(true);
		newStage.setOnCloseRequest((e) -> {
			LOGGER.info("Closing stage " + newStage + " from " + Thread.currentThread().getName());
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
			LOGGER.info("Hide stage " + oldStage + " and create new stage");
			if(blockingOperation) {
				blockingStage = createStage(true, null);
			} else {
				nonBlockingStage = createStage(false, null);
			}
			oldStage.hide();
		});
	}

	public <T> void displayAndWaitUIOperation(final UIOperation<T> operation) {
		display(operation.getRoot(), true);
		waitForUser(operation);
	}

	private <T> void waitForUser(UIOperation<T> operation) {
		try {
			LOGGER.info("Wait on Thread " + Thread.currentThread().getName());
			currentBlockingOperation = operation;
			operation.waitEnd();
			currentBlockingOperation = null;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private final class FlowPasswordCallback implements NexuPasswordInputCallback {

    private final Product product;
		private String passwordPrompt;

		public FlowPasswordCallback(Product product) {
		  this.product = product;
      this.passwordPrompt = null;
		}

		@Override
    @SuppressWarnings("unchecked")
		public char[] getPassword() {
			LOGGER.info("Request password");
      final OperationResult<Object> passwordResult = StandaloneUIDisplay.this.operationFactory.getOperation(
              UIOperation.class, "/fxml/password-input.fxml", passwordPrompt,
              AppPreloader.getConfig().getApplicationName(), product).perform();
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
          throw new NexuException(e);
        }
      } else {
        throw new IllegalArgumentException("Not managed operation status: " + passwordResult.getStatus().getCode());
      }
		}

		@Override
		public void setPasswordPrompt(String passwordPrompt) {
			this.passwordPrompt = passwordPrompt;
		}

	}

	@Override
	public PasswordInputCallback getPasswordInputCallback(Product product) {
		return new FlowPasswordCallback(product);
	}

	@Override
	public File displayFileChooser(ExtensionFilter... extensionFilters) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(ResourceBundle.getBundle("bundles/nexu").getString("fileChooser.title.openResourceFile"));
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

	@Override
	public void display(NonBlockingUIOperation operation) {
		display(operation.getRoot(), false);
	}
}
