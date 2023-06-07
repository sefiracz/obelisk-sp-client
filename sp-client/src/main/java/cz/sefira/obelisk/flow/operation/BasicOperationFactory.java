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
package cz.sefira.obelisk.flow.operation;

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.flow.Operation;
import cz.sefira.obelisk.api.flow.OperationFactory;
import cz.sefira.obelisk.prefs.PreferencesFactory;
import cz.sefira.obelisk.prefs.UserPreferences;
import cz.sefira.obelisk.view.DialogMessage;
import cz.sefira.obelisk.view.core.NonBlockingUIOperation;
import cz.sefira.obelisk.view.core.UIDisplay;
import cz.sefira.obelisk.view.core.UIOperation;
import cz.sefira.obelisk.api.PlatformAPI;

/**
 * Basic implementation of {@link OperationFactory} that uses reflection.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class BasicOperationFactory implements OperationFactory {

    private UIDisplay display;

    public BasicOperationFactory() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends Operation<R>> Operation<R> getOperation(final Class<T> clazz, final Object... params) {
        try {
            final T operation = clazz.getDeclaredConstructor().newInstance();
            if (operation instanceof CompositeOperation) {
                final CompositeOperation<R> compositeOperation = (CompositeOperation<R>) operation;
                compositeOperation.setOperationFactory(this);
                compositeOperation.setDisplay(this.display);
            } else if (operation instanceof UIDisplayAwareOperation) {
                final UIDisplayAwareOperation<R> uiDisplayAwareOperation = (UIDisplayAwareOperation<R>) operation;
                uiDisplayAwareOperation.setDisplay(this.display);
            }
            operation.setParams(params);
            return operation;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

  @Override
  @SuppressWarnings("unchecked")
  public void getMessageDialog(PlatformAPI api, DialogMessage message, boolean blockingUI) {
    UserPreferences prefs = PreferencesFactory.getInstance(AppConfig.get());
    String dialogId = message.getDialogId();
    if(dialogId != null && prefs.getHiddenDialogIds().contains(dialogId)) {
      return; // do not display message dialog
    }
    if (blockingUI) {
      getOperation(UIOperation.class, "/fxml/message.fxml", api, message).perform();
    } else {
      getOperation(NonBlockingUIOperation.class, "/fxml/message.fxml", api, message).perform();
    }
  }

  public void setDisplay(final UIDisplay display) {
        this.display = display;
    }
}
