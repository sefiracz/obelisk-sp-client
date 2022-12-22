package cz.sefira.obelisk.view.x509;

/*
 * Copyright 2013 by SEFIRA, spol. s r. o.
 * http://www.sefira.cz
 *
 * cz.sefira.obelisk.view.x509.CertInfoLogic
 *
 * Created: 18.12.13
 * Author: hlavnicka
 */


import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Business logic implementation of CertInfo dialog
 */
public class CertificateInfoLogic {

  private final CertificateInfoPanel info;
  private final Certificate certificate;
  private int row = 0;

  public CertificateInfoLogic(Certificate certificate, final JDialog frame, final CertificateInfoPanel info) {
    this.info = info;
    this.certificate = certificate;
    fillTable();
    info.getCloseButton().addActionListener(e -> frame.dispose());
    info.getCertFields().addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        JTable table = (JTable) e.getSource();
        row = table.getSelectedRow();
        info.setTextAreaContent((String) info.getCertFields().getModel().getValueAt(row, 2));
      }
    });
    info.getCertFields().addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        JTable table = (JTable) e.getSource();
        Point p = e.getPoint();
        row = table.rowAtPoint(p);
        if (e.getClickCount() == 1) {
          info.setTextAreaContent((String) info.getCertFields().getModel().getValueAt(row, 2));
        }
      }
    });
    info.getCertFields().addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == 9) {
          int selectedRow = info.getCertFields().getSelectedRow();
          int col = info.getCertFields().getSelectedColumn();
          int currentRow = selectedRow + col;
          if (selectedRow + col >= info.getCertFields().getRowCount()) {
            currentRow = 0;
          }
          row = selectedRow != -1 ? currentRow : 0;
        }
        if ((e.getKeyCode() == 40 || e.getKeyCode() == 10) && (row + 1 < info.getCertFields().getModel().getRowCount())) {
          row++;
        }
        else if (e.getKeyCode() == 38 && (row - 1 >= 0)) {
          row--;
        }
        info.setTextAreaContent((String) info.getCertFields().getModel().getValueAt(row, 2));
      }
    });
  }

  private void fillTable() {
    CertificateInfoData fs = new CertificateInfoData(certificate);
    AbstractTableModel tableModel = new AbstractTableModel() {

      private final String[] certColumnHeader = {
          ResourceBundle.getBundle("bundles/nexu").getString("certificate.viewer.col.field"),
          ResourceBundle.getBundle("bundles/nexu").getString("certificate.viewer.col.value"),
          ""
      };

      private List<String[]> data = new ArrayList<>();

      public AbstractTableModel getInstance(List<String[]> data) {
        if (data != null) {
          this.data = data;
        }
        this.fireTableDataChanged();
        return this;
      }

      public String getColumnName(int column) {
        return certColumnHeader[column];
      }

      public int getRowCount() {
        return data.size();
      }

      public int getColumnCount() {
        return certColumnHeader.length;
      }

      public Object getValueAt(int rowIndex, int columnIndex) {
        String[] field = data.get(rowIndex);
        if (columnIndex >= field.length) {
          columnIndex = 1;
        }
        return data.get(rowIndex)[columnIndex];
      }
    }.getInstance(fs.getFieldData());

    info.getCertFields().setSelectionModel(new DefaultListSelectionModel() {

      public DefaultListSelectionModel getInstance() {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return this;
      }

      public void clearSelection() {}

      public void removeSelectionInterval(int index0, int index1) {}

    }.getInstance());
    info.getCertFields().setModel(tableModel);
    TableColumnModel model = info.getCertFields().getColumnModel();
    model.getColumn(0).setPreferredWidth(3000); // 30% width
    model.getColumn(1).setPreferredWidth(7000); // 70% width
    model.removeColumn(model.getColumn(2)); // hide 3rd data column
    info.getCertFields().setRowSelectionInterval(3, 3); // pre-select Subject row
  }

}
