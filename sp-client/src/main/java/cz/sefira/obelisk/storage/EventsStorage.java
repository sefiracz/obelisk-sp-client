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
import one.microstream.persistence.internal.LoggingLegacyTypeMappingResultor;
import one.microstream.persistence.types.PersistenceLegacyTypeMappingResultor;
import one.microstream.persistence.types.Storer;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;
import org.apache.commons.lang.time.DateUtils;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * description
 */
public class EventsStorage implements AutoCloseable {

  private final EventsRoot eventsRoot = new EventsRoot();

  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  private final EmbeddedStorageManager storage;

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
    commitChange();
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
    // create empty placeholders to pad buggy javafx tableview
    for (int i=0; i<13; i++) {
      selected.add(new Notification());
    }
    return selected;
  }

  public void addListener(PropertyChangeListener listener) {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  public void removeListener(PropertyChangeListener listener) {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }

  private void commitChange() {
    Storer storer = storage.createEagerStorer();
    storer.store(eventsRoot);
    storer.commit();
  }

  @Override
  public void close() {
    try {
      storage.close();
      storage.shutdown();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public enum SelectorType {

    ALL, DAY, LAST

  }

}
