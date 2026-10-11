package com.parking.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

final class ParkflowTheme {

    static final Color NAVY = new Color(17, 31, 43);
    static final Color GREEN = new Color(48, 185, 150);
    static final Color GREEN_HOVER = new Color(37, 157, 126);
    static final Color BACKGROUND = new Color(15, 23, 32);
    static final Color SURFACE = new Color(27, 40, 51);
    static final Color TEXT = new Color(229, 238, 240);
    static final Color MUTED = new Color(164, 181, 188);
    static final Color BORDER = new Color(52, 70, 81);
    static final Color PALE_GREEN = new Color(39, 68, 62);
    static final Color PALE_BLUE = new Color(41, 59, 72);

    private static final Font BODY_FONT =
            new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font BUTTON_FONT =
            new Font("Segoe UI", Font.BOLD, 13);

    private ParkflowTheme() {
    }

    static void install() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", BODY_FONT);
        UIManager.put("Button.font", BUTTON_FONT);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.focus", PALE_GREEN);
        UIManager.put("Button.rollover", Boolean.TRUE);
        UIManager.put("CheckBox.background", BACKGROUND);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("RadioButton.background", BACKGROUND);
        UIManager.put("RadioButton.foreground", TEXT);
        UIManager.put("List.background", SURFACE);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("List.selectionBackground", PALE_GREEN);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("TextField.font", BODY_FONT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", GREEN);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        UIManager.put("ComboBox.font", BODY_FONT);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", PALE_GREEN);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ComboBox.border", BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 7, 4, 7)));
        UIManager.put("Table.font", BODY_FONT);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", PALE_GREEN);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TableHeader.font", BUTTON_FONT);
        UIManager.put("TableHeader.background", BACKGROUND);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("TableHeader.separatorColor", BORDER);
        UIManager.put("TabbedPane.font", BUTTON_FONT);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", SURFACE);
        UIManager.put("TabbedPane.contentAreaColor", SURFACE);
        UIManager.put("TabbedPane.focus", GREEN);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.foreground", TEXT);
        UIManager.put("TitledBorder.titleColor", TEXT);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("ScrollBar.background", SURFACE);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("Viewport.background", SURFACE);
    }

    static void styleButton(JButton button, boolean primary) {
        button.setFont(BUTTON_FONT);
        button.setBackground(primary ? GREEN : SURFACE);
        button.setForeground(primary ? NAVY : TEXT);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(primary ? GREEN : BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setMargin(new Insets(4, 10, 4, 10));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getModel().addChangeListener(event -> {
            if (button.getModel().isRollover() && button.isEnabled()) {
                button.setBackground(primary ? GREEN_HOVER : PALE_BLUE);
            } else {
                button.setBackground(primary ? GREEN : SURFACE);
            }
        });
    }

    static void styleTextField(JTextField field) {
        field.setFont(BODY_FONT);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setBorder(UIManager.getBorder("TextField.border"));
    }

    static void styleTable(JTable table) {
        table.setFont(BODY_FONT);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PALE_GREEN);
        table.setSelectionForeground(TEXT);
        table.setShowHorizontalLines(true);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source,
                    Object value,
                    boolean selected,
                    boolean focused,
                    int row,
                    int column) {
                Component cell = super.getTableCellRendererComponent(
                        source, value, selected, focused, row, column);
                if (!selected) {
                    cell.setBackground(row % 2 == 0
                            ? SURFACE : BACKGROUND);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                return cell;
            }
        });
        table.getTableHeader().setFont(BUTTON_FONT);
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setBackground(BACKGROUND);
        table.getTableHeader().setPreferredSize(
                new java.awt.Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
    }
}
