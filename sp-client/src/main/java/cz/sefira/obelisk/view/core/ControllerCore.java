package cz.sefira.obelisk.view.core;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.core.ControllerCore
 *
 * Created: 02.05.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * FXML controller core features
 */
public abstract class ControllerCore  {

  private static final Logger logger = LoggerFactory.getLogger(ControllerCore.class.getName());

  private volatile boolean update;

  /**
   * Notifies the update thread that updates JavaFX UI components
   */
  public final void notifyUpdate() {
    update = true;
  }


  /**
   * Offload thread to be run apart from JavaFX thread for heavy workload that takes time and therefore needs to
   * run at separate thread to not block UI rendering and user experience.
   *
   * @param callback Heavy workload that might take time to finish
   * @param notifyUpdate (if true) After workload is done notify update thread that updates JavaFX UI components
   */
  public final void asyncTask(TaskCallback callback, boolean notifyUpdate) {
    new Thread(() -> {
      try {
        callback.execute();
        if(notifyUpdate)
          notifyUpdate();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }).start();
  }


  /**
   * Updates the JavaFX UI components whenever {@code notifyUpdate()} is called
   * @param callback Implementation of {@code UpdateCallback()} function that updates the JavaFX components
   */
  public final void asyncUpdate(ScheduledExecutorService executorService, UpdateCallback callback) {
    executorService.scheduleAtFixedRate(() -> {
      if (update) {
        update = false;
        Platform.runLater(() -> {
          try {
            callback.update();
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        });
      }
    }, 100, 500, TimeUnit.MILLISECONDS);
  }

  @FunctionalInterface
  public interface UpdateCallback {

    void update() throws Exception;

  }

  @FunctionalInterface
  public interface TaskCallback {

    void execute() throws Exception;

  }

  public void setLogoBackground(Pane node) {
    setLogoBackground(node, null, null);
  }

  public void setLogoBackground(Pane node, Integer width, Integer height) {
    try {
      node.setStyle("-fx-background-image: url(data:image/png;base64," + AppConfig.get().getBackgroundLogo() + ");" +
          "-fx-background-position: right top; -fx-background-repeat: no-repeat;" +
          ((height != null && width != null) ? ("-fx-background-size: "+width+"px "+height+"px;") : "")
      );
    }
    catch (IOException e) {
      logger.error("Unable to show background image logo: "+e.getMessage(), e);
    }
  }

  /**
   * Handler for repeated periodical activation of pressed buttons
   */
  public static class PressedRepeatEventHandler implements EventHandler<MouseEvent> {

    private final Runnable callable;
    private final int initialDelay;
    private final  int period;
    private final  TimeUnit unit;

    private ScheduledExecutorService executorService;

    public PressedRepeatEventHandler(Runnable callable, int initialDelay, int period, TimeUnit unit) {
      this.callable = callable;
      this.initialDelay = initialDelay;
      this.period = period;
      this.unit = unit;
    }

    @Override
    public void handle(MouseEvent event) {
      if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
        Platform.runLater(() -> {
          if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
          }
          executorService.scheduleAtFixedRate(callable, initialDelay, period, unit);
        });
      }
      else if (event.getEventType().equals(MouseEvent.MOUSE_RELEASED) ||
          event.getEventType().equals(MouseEvent.MOUSE_EXITED) ||
          event.getEventType().equals(MouseEvent.DRAG_DETECTED) ||
          event.getEventType().equals(MouseEvent.MOUSE_ENTERED)) {
        if (executorService != null && !executorService.isShutdown()) {
          executorService.shutdownNow();
        }
      }
    }

  }

}
