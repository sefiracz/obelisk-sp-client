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

import cz.sefira.obelisk.api.Notification;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.storage.EventsStorage;
import cz.sefira.obelisk.util.TextUtils;
import cz.sefira.obelisk.view.StandaloneUIController;
import cz.sefira.obelisk.view.core.ControllerCore;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyLongWrapper;
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


import java.net.URL;
import java.util.ResourceBundle;

/**
 * Events viewer controller
 */
public class EventsViewerController extends ControllerCore implements StandaloneUIController, Initializable {

  private static final Logger logger = LoggerFactory.getLogger(EventsViewerController.class.getName());

  @FXML
  private RadioButton showLast;

  @FXML
  private RadioButton showToday;

  @FXML
  private RadioButton showAll;

  @FXML
  private Button exitButton;

  @FXML
  private TableView<Notification> eventsTable;

  @FXML
  private TableColumn<Notification, Number> eventIdColumn;

  @FXML
  private TableColumn<Notification, String> eventDateColumn;

  @FXML
  private TableColumn<Notification, String> eventNotificationColumn;

  @FXML
  private Button cancel;

  private Stage primaryStage;

  private final ObservableList<Notification> observableEvents;

  private PlatformAPI api;

  public EventsViewerController() {
    super();
    observableEvents = FXCollections.observableArrayList();
  }

  @Override
  public void init(Stage stage, Object... params) {
    this.primaryStage = stage;
    this.api = (PlatformAPI) params[0];

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

    Region exitIcon = new Region();
    exitIcon.setPrefSize(22,22);
    exitIcon.getStyleClass().add("icon-event-exit");
    exitButton.setGraphic(exitIcon);

    if (showLast.isSelected()) {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.LAST));
    } else if (showToday.isSelected()) {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.DAY));
    } else if (showAll.isSelected()) {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.ALL));
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    exitButton.setOnAction(e -> Platform.exit());
    cancel.setOnAction((e) -> close());
    eventsTable.setPlaceholder(new Label(resourceBundle.getString("table.view.no.content")));
    eventsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    eventsTable.setRowFactory(tv -> new TableRow<>() {

      @Override
      protected void updateItem(Notification item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null) {
          this.setTooltip(new Tooltip(item.getMessageText()));
        }
      }
    });

    eventIdColumn.setCellValueFactory(param -> new ReadOnlyLongWrapper(param.getValue().getSeqId()));
    eventDateColumn.setCellValueFactory(param -> {
      String date = TextUtils.localizedDatetime(param.getValue().getDate(), true);
      return new ReadOnlyStringWrapper(date);
    });
    eventNotificationColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getMessageText()));
    eventNotificationColumn.setCellFactory(new Callback<>() {
      @Override
      public TableCell<Notification, String> call(TableColumn<Notification, String> param) {
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

    showLast.setOnAction(e -> {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.LAST));
    });
    showToday.setOnAction(e -> {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.DAY));
    });
    showAll.setOnAction(e -> {
      observableEvents.setAll(api.getEventsStorage().getNotifications(EventsStorage.SelectorType.ALL));
    });

    eventsTable.setItems(observableEvents);
  }


  @Override
  public void close() {
    primaryStage.close();
  }

}
