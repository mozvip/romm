package com.github.mozvip.romm.ui;

import com.github.mozvip.romm.model.ArchiveStatus;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ArchiveStatusRenderer extends JLabel implements TableCellRenderer {

    public ArchiveStatusRenderer() {
        setOpaque(true); //MUST do this for background to show up.
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        ArchiveStatus status = (ArchiveStatus) value;
        switch (status) {
            case MISSING -> setBackground(new Color(213, 0, 0));
            case PARTIAL -> setBackground(new Color(255, 214, 0));
            case COMPLETE -> setBackground(new Color(0, 200, 83));
        }
        setText(status.name());
        return this;
    }
}
