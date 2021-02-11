package lu.nowina.nexu;

/*
 * Copyright 2021 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * lu.nowina.nexu.EntityDatabase
 *
 * Created: 05.02.2021
 * Author: hlavnicka
 */

/**
 * description
 */
public interface EntityDatabase {

  /**
   * Method that can be used to initialize loaded database data
   */
  void initialize();

  /**
   * Sets the event handler that must be triggered when an item is added or removed
   * to this database.
   * @param eventHandler The event handler to set.
   */
  void setOnAddRemoveAction(DatabaseEventHandler eventHandler);

}
