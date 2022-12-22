package cz.sefira.obelisk.view.x509;

/*
 * Copyright 2013 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.x509.CertificateInfo
 *
 * Created: 18.12.13
 * Author: hlavnicka
 */

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * description
 */
public class CertificateInfoDialog extends JDialog {

  private static final Logger logger = LoggerFactory.getLogger(CertificateInfoDialog.class.getName());

  private static final List<CertificateInfoDialog> dialogs = new ArrayList<>();
  private static int dialogCount = 0;

  public CertificateInfoDialog(Certificate certificate) {
    super(!dialogs.isEmpty() ? dialogs.get(dialogs.size() - 1) : null);
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    if (certificate != null) {
      try {
        try (InputStream in = CertificateInfoDialog.class.getResourceAsStream("/tray-icon.png")) {
          if (in != null) {
            setIconImage(new ImageIcon(IOUtils.toByteArray(in)).getImage());
          }
        }
        catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
        CertificateInfoPanel info = new CertificateInfoPanel();
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        new CertificateInfoLogic(certificate, this, info);
        setTitle(ResourceBundle.getBundle("bundles/nexu").getString("certificate.viewer.title"));
        /* Set up dialog */
        setContentPane(info);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setResizable(false);
        Point location = centerWindow(560, 480);
        if (location != null) {
          setLocation(location);
        }
        pack();
        dialogs.add(this);
        dialogCount = dialogs.size();
        setVisible(true);
      } finally {
        dialogs.remove(this);
        dialogCount = dialogs.size();
      }
    }
  }

  private Point centerWindow(double width, double height){
    final Rectangle2D screenResolution = Screen.getPrimary().getBounds();
    if(width > 0) {
      double x = (screenResolution.getWidth() / 4) - (width / 2) + (dialogCount % 5 * 25);
      if (x >= 0) {
        if (height > 0) {
          double y = (screenResolution.getHeight() / 4) - (height / 2) + (dialogCount % 4 * 25);
          if (y >= 0) {
            Point p = new Point();
            p.setLocation(x, y);
            return p;
          }
        }
      }
    }
    return null;
  }
}
