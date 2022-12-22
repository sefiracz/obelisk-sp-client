package cz.sefira.obelisk.view.x509;

/*
 * Copyright 2014 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 * 
 * cz.sefira.obelisk.view.x509.CertInfoPanel
 *
 * Created: 3.1.14 
 * Author: hlavnicka
 */

import cz.sefira.obelisk.api.EnvironmentInfo;
import cz.sefira.obelisk.api.OS;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class CertificateInfoPanel extends JPanel {

  private JButton closeButton;
  private JScrollPane textPane;
  private JTable certFields;
  private JTextArea certText;

  public CertificateInfoPanel() {
    initComponents();
  }

  @SuppressWarnings("unchecked")
  private void initComponents() {

    closeButton = new JButton();
    closeButton.setText(ResourceBundle.getBundle("bundles/nexu").getString("button.cancel"));

    JScrollPane fieldsPane = new JScrollPane();
    certFields = new JTable();
    // workaround to disable MacOS dark mode bad behavior
    if (EnvironmentInfo.buildFromSystemProperties(System.getProperties()).getOs().equals(OS.MACOSX)) {
      certFields.getTableHeader().setOpaque(false);
      certFields.getTableHeader().setBackground(Color.WHITE);
    }
    textPane = new JScrollPane();
    certText = new JTextArea();

    fieldsPane.setViewportView(certFields);
    textPane.setViewportView(certText);
    certText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    certText.setEditable(false);

    GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(closeButton)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(textPane)
                    .addComponent(fieldsPane, GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)))
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fieldsPane, GroupLayout.PREFERRED_SIZE, 235, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(textPane, GroupLayout.PREFERRED_SIZE, 245, GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(closeButton)
                .addContainerGap())
    );
  }

  public JTable getCertFields() {
    return certFields;
  }

  public JButton getCloseButton() {
    return closeButton;
  }

  public void setTextAreaContent(String text){
    final Point p = (Point) textPane.getViewport().getViewPosition().clone();
    certText.setText(text);
    certText.setEditable(false);
    SwingUtilities.invokeLater(() -> textPane.getViewport().setViewPosition(p));
  }

}