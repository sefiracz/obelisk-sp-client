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
package lu.nowina.nexu;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import lu.nowina.nexu.AppPreloader.PreloaderMessage;
import lu.nowina.nexu.api.AppConfig;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.plugin.InitializationMessage;
import lu.nowina.nexu.flow.BasicFlowRegistry;
import lu.nowina.nexu.flow.Flow;
import lu.nowina.nexu.flow.FlowRegistry;
import lu.nowina.nexu.flow.operation.BasicOperationFactory;
import lu.nowina.nexu.generic.SCDatabase;
import lu.nowina.nexu.generic.SessionManager;
import lu.nowina.nexu.generic.SmartcardInfoDatabase;
import lu.nowina.nexu.view.DialogMessage;
import lu.nowina.nexu.view.StandaloneDialog;
import lu.nowina.nexu.view.core.UIDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class App extends Application {

	private static final Logger logger = LoggerFactory.getLogger(App.class.getName());

	private static SystrayMenu systrayMenu;

	private HttpServer server;

	private AppConfig getConfig() {
		return AppPreloader.getConfig();
	}

	private Properties getProperties() {
		return AppPreloader.getProperties();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Platform.setImplicitExit(false);

		// show error if application is already running
		Parameters params = getParameters();
		if(params.getRaw().contains("alreadyRunning")) {
			StandaloneDialog.showDialog(null, new DialogMessage("preloader.error.already.running",
					DialogMessage.Level.ERROR, new String[] {getConfig().getApplicationName()}), true);
			System.exit(1);
		}

		// start application
		final StandaloneUIDisplay uiDisplay = new StandaloneUIDisplay();
		final OperationFactory operationFactory = new BasicOperationFactory();
		((BasicOperationFactory)operationFactory).setDisplay(uiDisplay);
		uiDisplay.setOperationFactory(operationFactory);

		final NexuAPI api = buildAPI(uiDisplay, operationFactory);

    logger.info("Detect all available products");
    api.detectAll();

    logger.info("Start Jetty");

		server = startHttpServer(api);

		if(api.getAppConfig().isEnableSystrayMenu()) {
			systrayMenu = new SystrayMenu(operationFactory, api, new UserPreferences(getConfig()));
		} else {
			logger.info("Systray menu is disabled.");
		}

		logger.info("Start finished");
	}

	private NexuAPI buildAPI(final UIDisplay uiDisplay, final OperationFactory operationFactory) {
		File nexuHome = getConfig().getNexuHome();
		SCDatabase smartcardDB;
		SmartcardInfoDatabase scInfoDB;
		if (nexuHome != null) {
			File cards = new File(nexuHome, "database-smartcard.xml");
			logger.info("Load smartcard database from " + cards.getAbsolutePath());
			smartcardDB = EntityDatabaseLoader.load(SCDatabase.class, cards);
			File infos = new File(nexuHome, "database-smartcard-info.xml");
			logger.info("Load smartcard connections database from " + infos.getAbsolutePath());
			scInfoDB = EntityDatabaseLoader.load(SmartcardInfoDatabase.class, infos);
		} else {
			smartcardDB = new SCDatabase();
			scInfoDB = new SmartcardInfoDatabase();
		}
		AppConfigurer.setLocalePreferences(new UserPreferences(getConfig()));
		AppConfigurer.applyUserPreferences(new UserPreferences(getConfig()));
		final APIBuilder builder = new APIBuilder();
		final NexuAPI api = builder.build(uiDisplay, getConfig(), getFlowRegistry(), smartcardDB, scInfoDB, operationFactory);
		notifyPreloader(builder.initPlugins(api, getProperties()));
		return api;
	}

	/**
	 * Returns the {@link FlowRegistry} to use to resolve {@link Flow}s.
	 * @return The {@link FlowRegistry} to use to resolve {@link Flow}s.
	 */
	protected FlowRegistry getFlowRegistry() {
		return new BasicFlowRegistry();
	}

	private HttpServer startHttpServer(NexuAPI api) throws Exception {
		final HttpServer server = buildHttpServer();
		server.setConfig(api);
		try {
			server.start();
		} catch(Exception e) {
			try {
				server.stop();
			} catch(Exception e1) {}
			throw e;
		}
		return server;
	}

	/**
	 * Build the HTTP Server for the platform
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private HttpServer buildHttpServer() {
		try {
			Class<HttpServer> cla = (Class<HttpServer>) Class.forName(getConfig().getHttpServerClass());
			logger.info("HttpServer is " + getConfig().getHttpServerClass());
			HttpServer server = cla.getDeclaredConstructor().newInstance();
			return server;
		} catch (Exception e) {
			logger.error("Cannot instanciate Http Server " + getConfig().getHttpServerClass(), e);
			throw new RuntimeException("Cannot instanciate Http Server");
		}
	}

	@Override
	public void stop() throws Exception {
		logger.info("Stopping application...");
		try {
      SessionManager.getManager().destroy();
			if(server != null) {
				server.stop();
				server = null;
			}
		} catch (final Exception e) {
			logger.error("Cannot stop server", e);
		}
	}

	private void notifyPreloader(final List<InitializationMessage> messages) {
		for(final InitializationMessage message : messages) {
			final AlertType alertType;
			switch(message.getMessageType()) {
			case WARNING:
				alertType = AlertType.WARNING;
				break;
			default:
				throw new IllegalArgumentException("Unknown message type: " + message.getMessageType());
			}
			final PreloaderMessage preloaderMessage = new PreloaderMessage(alertType, message.getTitle(),
					message.getHeaderText(), message.getContentText());
			notifyPreloader(preloaderMessage);
		}
	}

	public static void refreshSystrayMenu() {
		if(systrayMenu != null && systrayMenu.getSystrayMenuInitializer() != null)
			systrayMenu.getSystrayMenuInitializer().refreshLabels();
	}
}
