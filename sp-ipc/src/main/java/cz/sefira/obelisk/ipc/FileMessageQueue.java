package cz.sefira.obelisk.ipc;

/*
 * Copyright 2023 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.ipc.MessageQueue
 *
 * Created: 23.01.2023
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

/**
 * IPC message queue between N-instances of Launcher and 1 client
 */
public final class FileMessageQueue implements MessageQueue {

  private static final String messageFilesDir = "queue";
  private static final String fileExt = ".obmsg";
  private static final String partExt = ".obmsg.part";

  private final AppConfig appConfig;

  FileMessageQueue(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  public String addMessage(byte[] message) {
    try {
      String messagesDir = getMessagesDirectory();
      String filenameId = getFileMessageId();
      Path filename = Paths.get(messagesDir, filenameId + partExt);
      try (OutputStream out = Files.newOutputStream(filename)) {
        out.write(message); // TODO - encrypt message?
      }
      Files.move(filename, Paths.get(messagesDir, filenameId + fileExt), ATOMIC_MOVE);
      return filenameId + fileExt;
    }
    catch (Exception e) {
      throw new RuntimeException(e); // TODO - handle error
    }
  }

  public String addMessage(Message message) {
    try {
      String messagesDir = getMessagesDirectory();
      String filenameId = message.getId();
      Path filename = Paths.get(messagesDir, filenameId + partExt);
      try (OutputStream out = Files.newOutputStream(filename)) {
        out.write(message.getPayload()); // TODO - encrypt message?
      }
      Files.move(filename, Paths.get(messagesDir, filenameId + fileExt), ATOMIC_MOVE);
      return filenameId + fileExt;
    } catch (Exception e) {
      throw new RuntimeException(e); // TODO - handle error
    }
  }

  public Message getMessage() {
    try {
      String messagesDir = getMessagesDirectory();
      try (Stream<Path> list = Files.list(Paths.get(messagesDir))) {
        List<Path> msgFiles = list.filter(Files::isRegularFile)
            .filter(file -> file.getFileName().toString().endsWith(fileExt))
            .collect(Collectors.toList());
        if (!msgFiles.isEmpty()) {
          msgFiles.sort(Comparator.comparing(file -> file.getFileName().toString()));
          Path msgFile = msgFiles.get(0);
          try {
            String fileMessageName = msgFile.getFileName().toString();
            Long timestamp = getMessageIdTimestamp(fileMessageName);
            return new Message(fileMessageName, timestamp, Files.readAllBytes(msgFile)); // TODO - decrypt message?
          }
          finally {
            Files.delete(msgFile);
          }
        }
      }
      return null;
    }
    catch (Exception e) {
      throw new RuntimeException(e); // TODO - handle error
    }
  }

  private String getFileMessageId() throws NoSuchAlgorithmException {
    long timestamp = System.currentTimeMillis();
    byte[] id = MessageDigest.getInstance("MD5").digest(UUID.randomUUID().toString().getBytes());
    StringBuilder hex = new StringBuilder();
    for (byte b : id) {
      hex.append(String.format("%02x", b));
    }
    String hexId = hex.toString();
    return timestamp + "_" + hexId;
  }

  private Long getMessageIdTimestamp(String fileMessageId) {
    String time = fileMessageId.split("_")[0];
    try {
      return Long.parseLong(time);
    } catch (Exception e) {
      return null;
    }
  }

  private synchronized String getMessagesDirectory() throws IOException {
    final Path processDir = appConfig.getAppProcessDirectory();
    Path messagesDir = processDir.resolve(messageFilesDir);
    if (!messagesDir.toFile().exists()) {
      Files.createDirectories(messagesDir);
    }
    return messagesDir.toFile().getAbsolutePath();
  }
}
