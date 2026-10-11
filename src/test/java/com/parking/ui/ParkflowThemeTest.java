package com.parking.ui;

import org.junit.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

import static org.junit.Assert.assertNotNull;

public class ParkflowThemeTest {

    @Test
    public void installingThemeAllowsDashboardSwingComponentsToInitialize()
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ParkflowTheme.install();

            assertNotNull(UIManager.get("Button.rollover"));
            org.junit.Assert.assertEquals(
                    Boolean.TRUE, UIManager.get("Button.rollover"));
            org.junit.Assert.assertEquals(
                    ParkflowTheme.BACKGROUND,
                    UIManager.getColor("Panel.background"));
            org.junit.Assert.assertEquals(
                    ParkflowTheme.SURFACE,
                    UIManager.getColor("TextField.background"));
            org.junit.Assert.assertTrue(
                    ParkflowTheme.BACKGROUND.getRed() < Color.GRAY.getRed());
            assertNotNull(new JButton("Refresh Data"));
            assertNotNull(new JComboBox<>(new String[]{"OPEN", "CLOSED"}));
            assertNotNull(new JScrollPane(new JTable()));
            assertNotNull(new JTabbedPane());
        });
    }
}
