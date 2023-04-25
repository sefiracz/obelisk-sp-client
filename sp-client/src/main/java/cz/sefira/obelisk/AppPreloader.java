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
package cz.sefira.obelisk;

import com.sun.javafx.application.LauncherImpl;
import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.model.OS;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import iaik.pkcs.pkcs11.wrapper.PKCS11Implementation;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class AppPreloader extends Preloader {
	private static final Logger logger = LoggerFactory.getLogger(AppPreloader.class.getName());

	public void launchApp(String[] args) {
		// set jni dispatch (needs to happen before any JNA calls), JavaFX library cache directory,
		// MSCrypto, IAIK PKCS11 wrapper native libraries
		String libPath = setLibraryPath();
		logger.info("Native lib path: " + (libPath != null ? libPath : "unpacked libs"));
		logger.info("Launching app");
		LauncherImpl.launchApplication(getApplicationClass(), AppPreloader.class, args);
	}

	/**
	 * Displays splash screen at startup.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		Parameters params = getParameters();
		if (params.getNamed().get("error") != null) {
			String stacktrace = params.getNamed().get("stacktrace");
			logger.error(stacktrace);
			StandaloneDialog.showErrorDialog(new DialogMessage("preloader.error.fatal",
							DialogMessage.Level.ERROR, 500, 180), null, stacktrace);
			System.exit(1);
		} else if (new UserPreferences(AppConfig.get()).getSplashScreen()) {
			showSplashScreen(primaryStage);
		}
	}

	private void showSplashScreen(Stage primaryStage) {
		logger.info("Showing splashscreen");
		final String appName = AppConfig.get().getApplicationName();
		primaryStage.setTitle(appName);
		logger.info("Load splashscreen resources");
		primaryStage.getIcons().add(new Image(AppPreloader.class.getResourceAsStream("/images/icon.png")));
		final ImageView splash = new ImageView(new Image(AppPreloader.class.getResourceAsStream("/images/splash_min.png")));
		logger.info("Resources loaded");
		double splashWidth = splash.getImage().getWidth();
		double splashHeight = splash.getImage().getHeight();
		final StackPane background = new StackPane(splash);
		final Scene splashScene = new Scene(background, splashWidth, splashHeight);
		splashScene.setFill(Color.TRANSPARENT);
		Rectangle2D screenResolution = Screen.getPrimary().getBounds();
		primaryStage.setX((screenResolution.getWidth() / 2) - (splashWidth / 2));
		primaryStage.setY((screenResolution.getHeight() / 2) - (splashHeight / 2));
		primaryStage.setScene(splashScene);
		primaryStage.setAlwaysOnTop(true);
		primaryStage.initStyle(StageStyle.TRANSPARENT);
		logger.info("Show splashscreen");

		// transition definition
//		javafx.animation.FadeTransition transition = new javafx.animation.FadeTransition(Duration.seconds(2), background);
//		transition.setFromValue(1);
//		transition.setToValue(0);
//		transition.setRate(0.6);
		final PauseTransition transition = new PauseTransition(Duration.seconds(2));

		transition.setOnFinished(event -> {
			logger.info("Hide splashscreen");
			primaryStage.close();
		});
		logger.info("Play transition");
		primaryStage.show();
		transition.play();
	}

	private String setLibraryPath() {
		if (OS.isWindows() && System.getProperty("force.dynamic.libs") == null) {
			String libraryPath = Paths.get(AppConfig.get().getWindowsInstalledPath(), "lib").toFile().getAbsolutePath();
			if (new File(libraryPath).exists()) {
				// set JNA native library
				String jniDispatchName = "jnidispatch";
				if (Paths.get(libraryPath, System.mapLibraryName(jniDispatchName)).toFile().exists()) {
					System.setProperty("jna.boot.library.name", jniDispatchName);
					System.setProperty("jna.boot.library.path", libraryPath); // dir containing jnidispatch.dll
				}
				// set MSCryptoStore native library
				String cryptoStoreDll = "MSCryptoStore";
				if (Paths.get(libraryPath, System.mapLibraryName(cryptoStoreDll)).toFile().exists()) {
					System.setProperty("sefira.mscrypto.library.name", cryptoStoreDll);
					System.setProperty("sefira.mscrypto.library.path", libraryPath); // dir containing MSCryptoStore.dll
				}
				// set java library path
				System.setProperty("javafx.cachedir", libraryPath);
				// set PKCS11 wrapper path
				String pkcs11WrapperName = "pkcs11wrapper";
				Path pkcs11library = Paths.get(libraryPath, System.mapLibraryName(pkcs11WrapperName));
				if (pkcs11library.toFile().exists()) {
					// dir containing pkcs11wrapper.dll
					PKCS11Implementation.ensureLinkedAndInitialized(pkcs11library.toFile().getAbsolutePath());
				}
				return libraryPath;
			}
		}
		return null;
	}

	/**
	 * Returns the JavaFX {@link Application} class to launch.
	 *
	 * @return The JavaFX {@link Application} class to launch.
	 */
	protected Class<? extends Application> getApplicationClass() {
		return App.class;
	}

	@Override
	public void handleApplicationNotification(PreloaderNotification info) {
		if(info instanceof PreloaderMessage) {
			final PreloaderMessage preloaderMessage = (PreloaderMessage) info;
			logger.error("PreLoaderMessage: type = " + preloaderMessage.getMessageType() + ", title = " + preloaderMessage.getTitle()
					+", header = " + preloaderMessage.getHeaderText() + ", content = " + preloaderMessage.getContentText());
		} else if (info instanceof ErrorNotification) {
			ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
			try {
				ErrorNotification error = (ErrorNotification) info;
				Throwable t = error.getCause();
				String title = resources.getString("preloader.error")+" - "+error.getLocation();
				// Display dialog
				DialogMessage errMsg = new DialogMessage(error.getDetails(), DialogMessage.Level.ERROR);
				StandaloneDialog.showErrorDialog(errMsg, title, t);
			} catch (Exception e) {
				final Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle(resources.getString("preloader.error"));
				alert.setHeaderText(
						MessageFormat.format(resources.getString("preloader.error.occurred"), AppConfig.get().getApplicationName()));
				alert.setContentText(resources.getString("contact.application.provider"));
				alert.showAndWait();
			}
			System.exit(1);
		} else {
			logger.error("Unknown preloader notification class: " + info.getClass().getName());
		}
	}

	@Override
	public boolean handleErrorNotification(ErrorNotification info) {
		// Log error messages
		logger.error("An error has occurred during startup", info.getCause());

		// Display dialog
		ResourceBundle resourceBundle = ResourceBundle.getBundle("bundles/nexu");
		final Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(resourceBundle.getString("preloader.error"));
		alert.setHeaderText(MessageFormat.format(resourceBundle.getString("preloader.error.occurred"), AppConfig.get().getApplicationName()));
		alert.setContentText(resourceBundle.getString("contact.application.provider"));
		alert.showAndWait();
		return true;
	}

	/**
	 * POJO that holds information about a message that get returned to preloader.
	 *
	 * @author Jean Lepropre (jean.lepropre@nowina.lu)
	 */
	static class PreloaderMessage implements PreloaderNotification {
		private final Alert.AlertType messageType;
		private final String title;
		private final String headerText;
		private final String contentText;

		public PreloaderMessage(Alert.AlertType messageType, String title, String headerText, String contentText) {
			super();
			this.messageType = messageType;
			this.title = title;
			this.headerText = headerText;
			this.contentText = contentText;
		}

		public Alert.AlertType getMessageType() {
			return messageType;
		}

		public String getTitle() {
			return title;
		}

		public String getHeaderText() {
			return headerText;
		}

		public String getContentText() {
			return contentText;
		}
	}
}
