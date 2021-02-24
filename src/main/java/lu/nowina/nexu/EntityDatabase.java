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
 * Entity database
 */
public interface EntityDatabase {

  /**
   * This method is run everytime a database is loaded.
   * Can be implemented to initialize data just as they are loaded from database.
   */
  void initialize();

  /**
   * Sets the event handler that must be triggered when an item is added or removed
   * to this database.
   * @param eventHandler The event handler to set.
   */
  void setOnAddRemoveAction(DatabaseEventHandler eventHandler);

}
