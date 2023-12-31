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
package cz.sefira.obelisk.view.ui;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.ui.EventViewerController
 *
 * Created: 04.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.notification.EventNotification;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.storage.EventsStorage;
import cz.sefira.obelisk.util.TextUtils;
import cz.sefira.obelisk.view.StandaloneUIController;
import cz.sefira.obelisk.view.core.ControllerCore;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Events viewer controller
 */
public class EventsViewerController extends ControllerCore implements StandaloneUIController, PropertyChangeListener, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(EventsViewerController.class.getName());

  @FXML
  private RadioButton showLast;

  @FXML
  private RadioButton showToday;

  @FXML
  private RadioButton showAll;

  @FXML
  private TableView<EventNotification> eventsTable;

  @FXML
  private TableColumn<EventNotification, String> eventIdColumn;

  @FXML
  private TableColumn<EventNotification, String> eventDateColumn;

  @FXML
  private TableColumn<EventNotification, String> eventNotificationColumn;

  @FXML
  private Button cancel;

  private Stage primaryStage;

  private final ObservableList<EventNotification> observableEvents;

  private ScheduledExecutorService executorService;

  private PlatformAPI api;

  public EventsViewerController() {
    super();
    observableEvents = FXCollections.observableArrayList();
  }

  @Override
  public void init(Stage stage, Object... params) {
    this.primaryStage = stage;
    this.api = (PlatformAPI) params[0];
    executorService = Executors.newSingleThreadScheduledExecutor();
    api.getEventsStorage().addListener(this);

    Region lastIcon = new Region();
    lastIcon.setPrefSize(22,22);
    lastIcon.getStyleClass().add("icon-event-last");
    showLast.setGraphic(lastIcon);
    showLast.getStyleClass().remove("radio-button");

    Region dayIcon = new Region();
    dayIcon.setPrefSize(22,22);
    dayIcon.getStyleClass().add("icon-event-day");
    showToday.setGraphic(dayIcon);
    showToday.getStyleClass().remove("radio-button");

    Region allIcon = new Region();
    allIcon.setPrefSize(22,22);
    allIcon.getStyleClass().add("icon-event-all");
    showAll.setGraphic(allIcon);
    showAll.getStyleClass().remove("radio-button");

    asyncTask(() -> {}, true);

    // asynchronous window content update
    asyncUpdate(executorService, () -> {
      List<EventNotification> list = new ArrayList<>();
      if (showLast.isSelected()) {
        list = api.getEventsStorage().getNotifications(EventsStorage.SelectorType.LAST);
      } else if (showToday.isSelected()) {
        list = api.getEventsStorage().getNotifications(EventsStorage.SelectorType.DAY);
      } else if (showAll.isSelected()) {
        list = api.getEventsStorage().getNotifications(EventsStorage.SelectorType.ALL);
      }
      observableEvents.setAll(list);
    });

  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    cancel.setOnAction((e) -> windowClose(primaryStage));
    eventsTable.setPlaceholder(new Label(resourceBundle.getString("table.view.no.content")));
    eventsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    eventsTable.setRowFactory(tv -> new TableRow<>() {

      @Override
      protected void updateItem(EventNotification item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null && item.getMessageText() != null) {
          this.setTooltip(new Tooltip(item.getMessageText()));
        }
      }
    });

    eventIdColumn.setCellValueFactory(param -> {
      Long seqId = param.getValue().getSeqId();
      String seqIdValue = seqId >= 0 ? String.valueOf(seqId) : null;
      return new ReadOnlyStringWrapper(seqIdValue);
    });
    eventDateColumn.setCellValueFactory(param -> {
      String date = TextUtils.localizedDatetime(param.getValue().getDate(), true);
      return new ReadOnlyStringWrapper(date);
    });
    eventNotificationColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getMessageText()));
    eventNotificationColumn.setCellFactory(new Callback<>() {
      @Override
      public TableCell<EventNotification, String> call(TableColumn<EventNotification, String> param) {
        return new TableCell<>() {
          @Override
          public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (!isEmpty()) {
              Text text = new Text(item);
              text.wrappingWidthProperty().bind(getTableColumn().widthProperty());
              setGraphic(text);
            } else {
              setGraphic(null);
            }
          }
        };
      }
    });

    eventsTable.setItems(observableEvents);

    showLast.setOnAction(e -> {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.LAST));
    });
    showToday.setOnAction(e -> {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.DAY));
    });
    showAll.setOnAction(e -> {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.ALL));
    });

  }


  @Override
  public void close() {
    if(executorService != null)
      executorService.shutdown();
    api.getEventsStorage().removeListener(this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    asyncTask(() -> {}, true);
  }
}
