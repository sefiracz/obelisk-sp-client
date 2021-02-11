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

import java.io.File;

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
    return null; // override this
  }

}
