package cz.sefira.obelisk.view.core;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.core.TimerService
 *
 * Created: 02.05.2023
 * Author: hlavnicka
 */

import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 * description
 */
public class TimerService extends Service<Void> {

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