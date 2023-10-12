/**
 * © SEFIRA spol. s r.o., 2020-2023
 * <p>
 * Licensed under EUPL Version 1.2 or - upon approval by the European Commission - later versions of the EUPL (the "License").
 * You may use this work only in accordance with the License.
 * You can obtain a copy of the License at the following address:
 * <p>
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * <p>
 * Unless there is a legal or contractual obligation in writing, the software distributed under the License is distributed "as is",
 * WITHOUT WARRANTIES OR CONDITIONS WHATSOEVER, express or implied.
 * See the License for specific permissions and language restrictions under the License.
 */
package cz.sefira.obelisk.storage;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.storage.EventsStorage
 *
 * Created: 05.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.notification.EventNotification;
import cz.sefira.obelisk.storage.model.EventsRoot;
import one.microstream.persistence.internal.LoggingLegacyTypeMappingResultor;
import one.microstream.persistence.types.PersistenceLegacyTypeMappingResultor;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * description
 */
public class EventsStorage extends AbstractStorage {

  private static final Logger logger = LoggerFactory.getLogger(EventsStorage.class.getName());

  private final EventsRoot eventsRoot = new EventsRoot();

  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public EventsStorage(Path store) {
    EmbeddedStorageFoundation<?> foundation = EmbeddedStorage.Foundation(store);
    foundation.getConnectionFoundation().setLegacyTypeMappingResultor(
        LoggingLegacyTypeMappingResultor.New(
            PersistenceLegacyTypeMappingResultor.New()
        )
    );
    this.storage = foundation.createEmbeddedStorageManager(eventsRoot).start();
    if (!eventsRoot.isCloseFlag()) {
      eventsRoot.incrementSequence(); // increment at startup if sequence was not properly closed
    }
    logger.info("Events size: "+eventsRoot.getNotifications().size());
    removeOldEvents();
  }

  private void removeOldEvents() {
    List<EventNotification> old = new ArrayList<>();
    for (EventNotification n : eventsRoot.getNotifications()) {
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.YEAR, -1);
      if (n.getDate().before(calendar.getTime())) {
        old.add(n);
      }
    }
    if (!old.isEmpty()) {
      logger.info("Removing old events: "+old.size());
      eventsRoot.getNotifications().removeAll(old);
      commitChange(eventsRoot);
    }
  }

  public final synchronized void addNotification(EventNotification notification) {
    if (notification != null) {
      notification.setSeqId(eventsRoot.getSequence().get());
      eventsRoot.getNotifications().add(notification);
      if (notification.isClose()) {
        eventsRoot.incrementSequence();
        eventsRoot.setCloseFlag(true); // sequence is closed, new sequence
      } else {
        eventsRoot.setCloseFlag(false);
      }
    }
    propertyChangeSupport.firePropertyChange("event", "", "refresh");
    commitChange(eventsRoot);
  }

  public List<EventNotification> getNotifications(SelectorType selectorType) {
    List<EventNotification> allNotifications = new ArrayList<>(eventsRoot.getNotifications());
    List<EventNotification> selected = new ArrayList<>();
    switch (selectorType) {
      case LAST:
        int size = allNotifications.size();
        long seqId = size > 0 ? allNotifications.get(size-1).getSeqId() : -1;
        for (int i = allNotifications.size() - 1; i >= 0; i--) {
          EventNotification n = allNotifications.get(i);
          if (seqId == n.getSeqId()) {
            selected.add(n);
          } else {
            break;
          }
        }
        break;
      case DAY:
        Date today = new Date();
        for (int i = allNotifications.size() - 1; i >= 0; i--) {
          EventNotification n = allNotifications.get(i);
          if (DateUtils.isSameDay(today, n.getDate())) {
            selected.add(n);
          } else {
            break;
          }
        }
        break;
      case ALL:
      default:
        for (EventNotification n : allNotifications) {
          selected.add(0, n);
        }
        break;
    }
    return selected;
  }

  public void addListener(PropertyChangeListener listener) {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  public void removeListener(PropertyChangeListener listener) {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }

  public enum SelectorType {

    ALL, DAY, LAST

  }

}
