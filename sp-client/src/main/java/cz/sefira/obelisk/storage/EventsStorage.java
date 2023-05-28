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

import cz.sefira.obelisk.api.Notification;
import cz.sefira.obelisk.storage.model.EventsRoot;
import one.microstream.X;
import one.microstream.collections.EqHashTable;
import one.microstream.collections.HashTable;
import one.microstream.collections.types.XGettingTable;
import one.microstream.persistence.internal.InquiringLegacyTypeMappingResultor;
import one.microstream.persistence.internal.LoggingLegacyTypeMappingResultor;
import one.microstream.persistence.types.PersistenceLegacyTypeMappingResultor;
import one.microstream.persistence.types.PersistenceRefactoringMappingProvider;
import one.microstream.persistence.types.Storer;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;
import one.microstream.typing.KeyValue;
import org.apache.commons.lang.time.DateUtils;

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
    removeOldEvents();
  }

  private void removeOldEvents() {
    List<Notification> old = new ArrayList<>();
    for (Notification n : eventsRoot.getNotifications()) {
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.YEAR, -1);
      if (n.getDate().before(calendar.getTime())) {
        old.add(n);
      }
    }
    eventsRoot.getNotifications().removeAll(old);
    commitChange(eventsRoot);
  }

  public final synchronized void addNotification(Notification notification) {
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

  public List<Notification> getNotifications(SelectorType selectorType) {
    List<Notification> allNotifications = new ArrayList<>(eventsRoot.getNotifications());
    List<Notification> selected = new ArrayList<>();
    switch (selectorType) {
      case LAST:
        int size = allNotifications.size();
        long seqId = size > 0 ? allNotifications.get(size-1).getSeqId() : -1;
        for (int i = allNotifications.size() - 1; i >= 0; i--) {
          Notification n = allNotifications.get(i);
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
          Notification n = allNotifications.get(i);
          if (DateUtils.isSameDay(today, n.getDate())) {
            selected.add(n);
          } else {
            break;
          }
        }
        break;
      case ALL:
      default:
        for (Notification n : allNotifications) {
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
