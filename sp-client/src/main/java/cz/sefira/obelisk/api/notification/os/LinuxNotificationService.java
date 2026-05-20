package cz.sefira.obelisk.api.notification.os;

/*
 * Copyright 2026 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.api.notification.os.LinuxNotificationService
 *
 * Created: 24/04/2026
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.AppConfig;
import cz.sefira.obelisk.api.PlatformAPI;
import cz.sefira.obelisk.api.notification.Notification;
import cz.sefira.obelisk.api.notification.NotificationService;
import es.blackleg.jlibnotify.JLibnotify;
import es.blackleg.jlibnotify.JLibnotifyNotification;
import es.blackleg.jlibnotify.core.DefaultJLibnotifyLoader;
import es.blackleg.jlibnotify.exception.JLibnotifyLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

/**
 * Linux notification service
 */
public class LinuxNotificationService extends NotificationService {

  private static final Logger logger = LoggerFactory.getLogger(LinuxNotificationService.class);

  private final JLibnotify jLibnotify;

  public LinuxNotificationService(PlatformAPI api) {
    super(api);
    try {
      jLibnotify = DefaultJLibnotifyLoader.init().load();
    } catch (JLibnotifyLoadException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void pushNativeNotification(Notification notification) {
    JLibnotifyNotification jlibNotification;
    try {
      jLibnotify.init(AppConfig.get().getApplicationName());
      String icon = switch (notification.getType()) {
        case WARNING -> "dialog-warning";
        case ERROR -> "dialog-error";
        case INFO -> "dialog-information";
        default -> Paths.get(System.getProperty("user.home"), ".local/share/obelisk-signing-portal-client/lib/obelisk-signing-portal-client.png").toAbsolutePath().toString();
      };
      jlibNotification = jLibnotify.createNotification(AppConfig.get().getApplicationName(), notification.getMessageText(), icon);
      if (notification.getDelay() > 0) {
        jlibNotification.setTimeOut(notification.getDelay() * 1000);
      }
      jlibNotification.show();
    } catch (Exception e) {
      logger.error("Cannot push notification: "+e.getMessage(), e);
      pushIntegratedNotification(notification);
    }
  }

}
