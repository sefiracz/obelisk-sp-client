package cz.sefira.obelisk.view;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.StandaloneUIController
 *
 * Created: 15.03.2023
 * Author: hlavnicka
 */

import javafx.stage.Stage;

import java.io.Closeable;

/**
 * API for standalone FXML-backed controller
 */
public interface StandaloneUIController extends Closeable {

 void init(Stage stage, Object... params);

}
