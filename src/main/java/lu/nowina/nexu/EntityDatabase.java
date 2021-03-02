/**
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Licensed under EUPL Version 1.1 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
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
