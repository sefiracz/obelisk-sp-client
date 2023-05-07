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
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
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

  private static final Object lock = new Object();

  protected final long UPDATE_INTERVAL = 500;

  protected volatile boolean update;

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
  public final void asyncTask(Runnable callback, boolean notifyUpdate) {
    new Thread(() -> {
      try {
        callback.run();
        if(notifyUpdate)
          notifyUpdate();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }).start();
  }

  /**
   * Updates the JavaFX UI components whenever {@code notifyUpdate()} is called
   * @param executorService Thread executor service that's going to run this function
   * @param useJfxThread Runs callback in JavaFX thread when set to true, otherwise user needs to wrap code explicitly
   * @param callback Implementation that updates the JavaFX components
   */
  public final void asyncUpdate(ScheduledExecutorService executorService, boolean useJfxThread, Runnable callback) {
    executorService.scheduleAtFixedRate(() -> {
      if (update) {
        synchronized (lock) {
          if (useJfxThread) {
            Platform.runLater(() -> run(callback));
          } else {
            run(callback);
          }
        }
        update = false;
      }
    }, 100, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  /**
   * Updates the JavaFX UI components whenever {@code notifyUpdate()} is called, implicitly runs under JavaFX thread
   * @param executorService Thread executor service that's going to run this function
   * @param callback Implementation that updates the JavaFX components
   */
  public final void asyncUpdate(ScheduledExecutorService executorService, Runnable callback) {
    asyncUpdate(executorService, true, callback);
  }

  private void run(Runnable callback) {
    try {
      callback.run();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  public void windowClose(Stage stage) {
    stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
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
