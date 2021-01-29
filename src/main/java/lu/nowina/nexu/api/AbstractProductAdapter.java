package lu.nowina.nexu.api;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.api.AbstractProductAdapter
 *
 * Created: 05.01.2021
 * Author: hlavnicka
 */

import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;
import lu.nowina.nexu.view.core.UIOperation;

import java.io.File;
import java.util.ResourceBundle;

/**
 * description
 */
public abstract class AbstractProductAdapter implements ProductAdapter {

  protected final File nexuHome;

  public AbstractProductAdapter(final File nexuHome) {
    this.nexuHome = nexuHome;
  }

  @Override
  public SystrayMenuItem getExtensionSystrayMenuItem(final NexuAPI api) {
    return new SystrayMenuItem() {

      @Override
      public String getName() {
        return "systray.menu.manage.keystores";
      }

      @Override
      public String getLabel() {
        return ResourceBundle.getBundle("bundles/nexu").getString(getName());
      }

      @Override
      public FutureOperationInvocation<Void> getFutureOperationInvocation() {
        return UIOperation.getFutureOperationInvocation(NonBlockingUIOperation.class,
            "/fxml/manage-keystores.fxml", api);
      }
    };
  }

}
