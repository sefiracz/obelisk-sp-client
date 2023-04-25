package cz.sefira.obelisk.process;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.process.WindowsProcessChecker
 *
 * Created: 06.04.2023
 * Author: hlavnicka
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *  Process handler For Windows operating system
 */
public class WindowsProcessHandler implements ProcessHandler {

  @Override
  public boolean isProccessRunning(long pid) {
    try {
      Process tasklistProcess = Runtime.getRuntime().exec(
          new String[]{"cmd", "/c", "tasklist /FI \"PID eq " + pid + "\""});
      BufferedReader tasklistOutputReader = new BufferedReader(new InputStreamReader(tasklistProcess.getInputStream()));
      String line = null;
      boolean processRunning = false;
      while ((line = tasklistOutputReader.readLine()) != null) {
        if (line.contains(" " + pid + " ")) {
          processRunning = true;
          break;
        }
      }
      return processRunning;
    } catch (Exception ex) {
      return false;
    }
  }

  @Override
  public boolean killProcess(long pid) {
    try {
      Runtime.getRuntime().exec(new String[]{"cmd", "/c", "taskkill /PID " + pid + " /F"});
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

}
