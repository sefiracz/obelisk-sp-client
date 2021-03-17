/**
 * © Nowina Solutions, 2015-2015
 * © SEFIRA spol. s r.o., 2020-2021
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.view.core;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lu.nowina.nexu.api.flow.OperationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Convenient base class for {@link UIOperationController}.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public abstract class AbstractUIOperationController<R> implements UIOperationController<R> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUIOperationController.class.getName());

	private UIOperation<R> uiOperation;
	private UIDisplay display;

	private volatile boolean update;

	public AbstractUIOperationController() {
		super();
	}

	@Override
	public final void setUIOperation(final UIOperation<R> uiOperation) {
		this.uiOperation = uiOperation;
	}

	@Override
	public final void setDisplay(UIDisplay display) {
		this.display = display;
	}

	protected final void signalEnd(R result) {
		uiOperation.signalEnd(result);
	}

	/**
	 * Provides the flow alternative actions (other than next or cancel).
	 * @param operationStatus
	 * Status the flow will check before dispatching to an action.
	 */
	protected final void signalEndWithStatus(final OperationStatus operationStatus) {
		uiOperation.signalEnd(operationStatus);
	}

	protected final void signalUserCancel() {
		uiOperation.signalUserCancel();
	}

	/**
	 * This implementation does nothing.
	 */
	public void init(Object... params) {
		// Do nothing by contract
	}

	protected final UIDisplay getDisplay() {
		return display;
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
        LOGGER.error(e.getMessage(), e);
      }
    }).start();
  }

  /**
   * Notifies the update thread that updates JavaFX UI components
   */
  public final void notifyUpdate() {
    update = true;
  }

  /**
   * Updates the JavaFX UI components whenever {@code notifyUpdate()} is called
   * @param callback Implementation of {@code UpdateCallback()} function that updates the JavaFX components
   */
  public final void asyncUpdate(UpdateCallback callback) {
    if(uiOperation == null)
      return;
    uiOperation.getUpdateExecutorService().scheduleAtFixedRate(() -> {
      if (update) {
        Platform.runLater(() -> {
          try {
            callback.update();
          } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
          } finally {
            update = false;
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

  public static class TimerService extends Service<Void> {

    private final long seconds;

    public TimerService(long seconds) {
      if (seconds <= 0)
        throw new IllegalArgumentException("Invalid value. Positive value only.");
      this.seconds = seconds;
    }

    @Override
    protected Task<Void> createTask() {
      return new Task<Void>() {

        @Override
        protected Void call() throws Exception {
          Thread.sleep(seconds * 10L);
          for (int p = 99; p > 0; p--) {
            Thread.sleep(seconds * 10L);
            updateProgress(p, 100);
          }
          return null;
        }
      };
    }
  }


}
