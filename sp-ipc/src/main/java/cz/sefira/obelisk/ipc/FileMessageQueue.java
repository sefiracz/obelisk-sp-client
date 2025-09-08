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

import cz.sefira.obelisk.api.config.AppDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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

  private static final Logger logger = LoggerFactory.getLogger(FileMessageQueue.class.getName());
  private static final String fileExt = ".obmsg";
  private static final String partExt = ".obmsg.part";

  FileMessageQueue(){}

  public String addMessage(byte[] message) {
    try {
      Path messagesDir = AppDataProvider.get().getQueueDirPath();
      String filenameId = getFileMessageId();
      Path filename = messagesDir.resolve(filenameId + partExt);
      try (OutputStream out = Files.newOutputStream(filename)) {
        out.write(message); // TODO - encrypt message?
      }
      Files.move(filename, messagesDir.resolve(filenameId + fileExt), ATOMIC_MOVE);
      return filenameId + fileExt;
    }
    catch (Exception e) {
      logger.error("Failed to push byte[] message to queue: "+e.getMessage(), e);
    }
    return null;
  }

  public String addMessage(Message message) {
    try {
      Path messagesDir = AppDataProvider.get().getQueueDirPath();
      String filenameId = message.getId();
      Path filename = messagesDir.resolve(filenameId + partExt);
      try (OutputStream out = Files.newOutputStream(filename)) {
        out.write(message.getPayload()); // TODO - encrypt message?
      }
      Files.move(filename, messagesDir.resolve(filenameId + fileExt), ATOMIC_MOVE);
      return filenameId + fileExt;
    } catch (Exception e) {
      logger.error("Failed to push message object to queue: "+e.getMessage(), e);
    }
    return null;
  }

  public Message getMessage() throws Exception {
    try {
      Path messagesDir = AppDataProvider.get().getQueueDirPath();
      try (Stream<Path> list = Files.list(messagesDir)) {
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
      logger.error("Failed to read message from queue: "+e.getMessage(), e);
    }
    return null;
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

}
