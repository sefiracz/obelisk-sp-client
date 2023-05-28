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
import cz.sefira.obelisk.flow.FlowRegistry;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.api.plugin.InitErrorMessage;
import cz.sefira.obelisk.api.plugin.AppPlugin;
import cz.sefira.obelisk.storage.EventsStorage;
import cz.sefira.obelisk.storage.ProductStorage;
import cz.sefira.obelisk.storage.SmartcardStorage;
import cz.sefira.obelisk.storage.StorageHandler;
import cz.sefira.obelisk.view.core.UIDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds an instance of {@link PlatformAPI}.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class APIBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(APIBuilder.class.getName());

	public APIBuilder() {
		super();
	}

	/**
	 * Builds and returns an instance of {@link PlatformAPI}.
	 * @param display The implementation of {@link UIDisplay} used to display UI elements.
	 * @param flowRegistry The implementation of {@link FlowRegistry} to use.
	 * @param productStorage The local product database.
	 * @param smartcardStorage The local database of supported smartcard information.
	 * @param eventsStorage The local events database
	 * @param operationFactory The implementation of {@link OperationFactory} to use.
	 * @return The built instance of {@link PlatformAPI}.
	 */
	public PlatformAPI build(final UIDisplay display, final FlowRegistry flowRegistry,
													 final StorageHandler storageHandler, final OperationFactory operationFactory) {
		return new InternalAPI(display, storageHandler, flowRegistry, operationFactory);
	}

	/**
	 * Init plugins on the given {@link PlatformAPI} instance.
	 * @param api The {@link PlatformAPI} instance on which plugins must be initialized. It <strong>MUST</strong> be
	 * an instance previously returned by {@link APIBuilder#build(UIDisplay, FlowRegistry, StorageHandler, OperationFactory)}.
	 * @param properties Configuration properties of the plugin to initialize.
	 * @return Messages about events that occurred during plugins initialization.
	 */
	public List<InitErrorMessage> initPlugins(final PlatformAPI api, final Properties properties) {
		final List<InitErrorMessage> messages = new ArrayList<>();
		final List<PluginInit> plugins = new ArrayList<>();
		Pattern pluginRegex = Pattern.compile("plugin_(\\d+)_(\\w+)");
		for (final String key : properties.stringPropertyNames()) {
			if (key.startsWith("plugin_")) {
				Matcher pluginMatch = pluginRegex.matcher(key);
				if(pluginMatch.find()) {
					// get plugin order
					String pluginOrder = pluginMatch.group(1);
					// get plugin ID name
					String pluginId = pluginMatch.group(2);
					// get plugin class name
					final String pluginClassName = properties.getProperty(key);
					plugins.add(new PluginInit(Integer.parseInt(pluginOrder), pluginId, pluginClassName));
				}
			}
		}
		// sort plugins by given order
		plugins.sort(Comparator.comparing(PluginInit::getPluginOrder));
		// initialize plugins in order
		for(PluginInit plugin : plugins) {
			LOGGER.info(" + Plugin " + plugin.getPluginClassName());
			messages.addAll(buildAndRegisterPlugin((InternalAPI) api, plugin.getPluginClassName(), plugin.getPluginId()));
		}
		return messages;
	}

	private List<InitErrorMessage> buildAndRegisterPlugin(InternalAPI api, String pluginClassName, String pluginId) {
		try {
			final Class<? extends AppPlugin> clazz = Class.forName(pluginClassName).asSubclass(AppPlugin.class);
			final AppPlugin plugin = clazz.getDeclaredConstructor().newInstance();
			return plugin.init(pluginId, api);
		} catch (final Exception e) {
			LOGGER.error(MessageFormat.format("Cannot register plugin {0} (id: {1})", pluginClassName, pluginId), e);
			throw new AppException(e);
		}
	}

	private static class PluginInit {

		private final int pluginOrder;
		private final String pluginClassName;
		private final String pluginId;

		public PluginInit(int pluginOrder, String pluginId, String pluginClassName) {
			this.pluginOrder = pluginOrder;
			this.pluginClassName = pluginClassName;
			this.pluginId = pluginId;
		}

		public int getPluginOrder() {
			return pluginOrder;
		}

		public String getPluginClassName() {
			return pluginClassName;
		}

		public String getPluginId() {
			return pluginId;
		}
	}
}
