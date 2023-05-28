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

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.api.plugin.InitErrorMessage;
import cz.sefira.obelisk.flow.BasicFlowRegistry;
import cz.sefira.obelisk.flow.Flow;
import cz.sefira.obelisk.flow.FlowRegistry;
import cz.sefira.obelisk.flow.operation.BasicOperationFactory;
import cz.sefira.obelisk.generic.SessionManager;
import cz.sefira.obelisk.storage.EventsStorage;
import cz.sefira.obelisk.storage.ProductStorage;
import cz.sefira.obelisk.storage.SmartcardStorage;
import cz.sefira.obelisk.storage.StorageHandler;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.StandaloneDialog;
import cz.sefira.obelisk.view.core.UIDisplay;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {

	private static final Logger logger = LoggerFactory.getLogger(App.class.getName());

	private static final ExecutorService initThread = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "InitThread");
		t.setDaemon(false);
		return t;
	});

	private StorageHandler storageHandler;

	@Override
	public void start(Stage primaryStage) throws Exception {
		Platform.setImplicitExit(false);

		// starting params
		Parameters params = getParameters(); // currently not used

		// init UI
		final StandaloneUIDisplay uiDisplay = new StandaloneUIDisplay();
		final OperationFactory operationFactory = new BasicOperationFactory();
		((BasicOperationFactory)operationFactory).setDisplay(uiDisplay);
		uiDisplay.setOperationFactory(operationFactory);

		// initialize API thread
		initThread.submit(() -> {
			try {
				logger.info("Initializing platform API");
				final PlatformAPI api = buildAPI(uiDisplay, operationFactory);
				AppConfigurer.applyLocale(api, null);
				logger.info("Detect all available products");
				api.detectAll();
				logger.info("Initialization finished");
			} catch (Exception e) {
				logger.error("Initialization failed: "+e.getMessage(), e);
				StandaloneDialog.runLater(() -> {
						StandaloneDialog.showErrorDialog(new DialogMessage("preloader.error.fatal",
								DialogMessage.Level.ERROR, 500, 180), null, e);
					System.exit(1);
				});
			}
		});
		initThread.shutdown(); // initialization thread no longer needed
		logger.info("Start finished");
	}

	private PlatformAPI buildAPI(final UIDisplay uiDisplay, final OperationFactory operationFactory) {
		try {
			storageHandler = new StorageHandler();
		} catch (IOException e) {
			StandaloneDialog.showDialog(null, new DialogMessage("preloader.error.occurred",
					DialogMessage.Level.ERROR, new String[] {e.getMessage()}), true);
			System.exit(1);
		}

		AppConfigurer.applyUserPreferences(new UserPreferences(AppConfig.get()));
		final APIBuilder builder = new APIBuilder();
		final PlatformAPI api = builder.build(uiDisplay, getFlowRegistry(), storageHandler, operationFactory);
		notifyPreloader(builder.initPlugins(api, AppConfig.get().getProperties()));
		return api;
	}

	/**
	 * Returns the {@link FlowRegistry} to use to resolve {@link Flow}s.
	 * @return The {@link FlowRegistry} to use to resolve {@link Flow}s.
	 */
	protected FlowRegistry getFlowRegistry() {
		return new BasicFlowRegistry();
	}

	@Override
	public void stop() {
		logger.info("Stopping application...");
		SessionManager.getManager().destroy();
		if (storageHandler != null) {
			storageHandler.close();
		}
		System.exit(0);
	}

	private void notifyPreloader(final List<InitErrorMessage> messages) {
		for(final InitErrorMessage message : messages) {
			Preloader.ErrorNotification errorNotification = new Preloader.ErrorNotification(message.getPluginName(),
					message.getMessageProperty(), message.getException());
			notifyPreloader(errorNotification);
		}
	}

}
