package com.parking.ui;

import com.parking.services.AdminService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

public final class LoginWindow extends JFrame {

    private static final String[] SECURITY_QUESTIONS = {
        "What was the name of your first school?",
        "What city were you born in?",
        "What was the name of your first pet?"
    };

    private final AdminService adminService = new AdminService();
    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JPasswordField confirmPasswordField =
            new JPasswordField(22);
    private final JComboBox<String> securityQuestionBox =
            new JComboBox<>(SECURITY_QUESTIONS);
    private final JPasswordField securityAnswerField =
            new JPasswordField(22);
    private final JPanel fieldsPanel = new JPanel(new GridBagLayout());
    private final JLabel headingLabel = new JLabel();
    private final JButton submitButton = new JButton();
    private boolean creatingAccount;

    public LoginWindow() throws SQLException {
        ParkflowTheme.install();
        creatingAccount = !adminService.hasAdminAccount();

        setTitle("Parkflow - Admin Login");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(500, 500));
        setSize(560, 620);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(ParkflowTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ParkflowTheme.NAVY);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 5, 0, 0, ParkflowTheme.GREEN),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        JLabel brand = new JLabel("Parkflow");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 27));
        brand.setForeground(Color.WHITE);
        header.add(brand, BorderLayout.WEST);

        JLabel badge = new JLabel("ADMIN ACCESS");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(new Color(176, 205, 210));
        header.add(badge, BorderLayout.EAST);

        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(ParkflowTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ParkflowTheme.BORDER),
                BorderFactory.createEmptyBorder(22, 24, 22, 24)));

        headingLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        fieldsPanel.setBackground(ParkflowTheme.SURFACE);
        JScrollPane fieldsScrollPane = new JScrollPane(fieldsPanel);
        fieldsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        fieldsScrollPane.getViewport().setBackground(ParkflowTheme.SURFACE);
        fieldsScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setBackground(ParkflowTheme.SURFACE);
        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.gridx = 0;
        actionConstraints.gridy = 0;
        actionConstraints.weightx = 1;
        actionConstraints.fill = GridBagConstraints.HORIZONTAL;
        actionConstraints.insets = new Insets(5, 0, 5, 0);

        ParkflowTheme.styleButton(submitButton, true);
        submitButton.addActionListener(event -> submitCredentials());
        actions.add(submitButton, actionConstraints);

        JButton forgotPasswordButton = new JButton("Forgot password?");
        ParkflowTheme.styleButton(forgotPasswordButton, false);
        forgotPasswordButton.addActionListener(
                event -> recoverPassword());
        actionConstraints.gridy++;
        actions.add(forgotPasswordButton, actionConstraints);

        JButton driverButton = new JButton("Open Driver Window");
        ParkflowTheme.styleButton(driverButton, false);
        driverButton.addActionListener(event -> {
            DriverWindow driverWindow = new DriverWindow();
            driverWindow.setVisible(true);
        });
        actionConstraints.gridy++;
        actions.add(driverButton, actionConstraints);

        JLabel footer = new JLabel(
                "Driver access does not require an admin login.",
                SwingConstants.CENTER);
        footer.setForeground(ParkflowTheme.MUTED);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        card.add(headingLabel, BorderLayout.NORTH);
        card.add(fieldsScrollPane, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);
        root.add(card, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);

        refreshForm();
        getRootPane().setDefaultButton(submitButton);
    }

    private void refreshForm() {
        fieldsPanel.removeAll();
        headingLabel.setText(creatingAccount
                ? "Create the administrator account"
                : "Sign in to the dashboard");
        submitButton.setText(creatingAccount
                ? "Create Admin Account"
                : "Sign In");

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 0, 5, 0);

        addField("Username", usernameField, constraints);
        addField("Password", passwordField, constraints);
        if (creatingAccount) {
            addField("Confirm password", confirmPasswordField, constraints);
            addField("Security question", securityQuestionBox, constraints);
            addField("Security answer", securityAnswerField, constraints);
            JLabel hint = new JLabel(
                    "Use at least 8 characters. Your recovery answer is "
                            + "stored as a one-way hash.");
            hint.setForeground(ParkflowTheme.MUTED);
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            constraints.gridy++;
            fieldsPanel.add(hint, constraints);
        }

        fieldsPanel.revalidate();
        fieldsPanel.repaint();
        packHeight();
    }

    private void addField(
            String label, java.awt.Component field,
            GridBagConstraints constraints) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        constraints.gridy++;
        fieldsPanel.add(fieldLabel, constraints);
        constraints.gridy++;
        fieldsPanel.add(field, constraints);
    }

    private void packHeight() {
        java.awt.Rectangle screen =
                java.awt.GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .getMaximumWindowBounds();
        int height = Math.min(
                creatingAccount ? 660 : 560,
                Math.max(500, screen.height - 32));
        setSize(560, height);
        setLocationRelativeTo(null);
    }

    private void submitCredentials() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            if (creatingAccount) {
                String confirmation =
                        new String(confirmPasswordField.getPassword());
                if (!password.equals(confirmation)) {
                    showError("The passwords do not match.");
                    return;
                }

                adminService.createInitialAdmin(
                        username,
                        password,
                        (String) securityQuestionBox.getSelectedItem(),
                        new String(securityAnswerField.getPassword()));
                creatingAccount = false;
                passwordField.setText("");
                confirmPasswordField.setText("");
                securityAnswerField.setText("");
                refreshForm();
                JOptionPane.showMessageDialog(
                        this,
                        "Admin account created. Sign in to continue.",
                        "Account Created",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if (!adminService.authenticate(username, password)) {
                showError("The username or password is incorrect.");
                return;
            }

            if (adminService.getSecurityQuestion(username).isEmpty()
                    && !configureLegacyRecovery(username, password)) {
                return;
            }

            ParkingDashboard dashboard = new ParkingDashboard();
            dashboard.setVisible(true);
            dispose();
        } catch (SQLException | IllegalArgumentException
                 | IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    private boolean configureLegacyRecovery(
            String username, String password) throws SQLException {
        JComboBox<String> questionBox =
                new JComboBox<>(SECURITY_QUESTIONS);
        JPasswordField answerField = new JPasswordField(18);
        int result = JOptionPane.showConfirmDialog(
                this,
                new Object[]{
                    "Set a recovery question for your account:",
                    questionBox,
                    "Answer (at least 3 characters):",
                    answerField
                },
                "Set Up Password Recovery",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        return adminService.configureRecovery(
                username, password,
                (String) questionBox.getSelectedItem(),
                new String(answerField.getPassword()));
    }

    private void recoverPassword() {
        String username = JOptionPane.showInputDialog(
                this, "Enter the admin username:",
                "Password Recovery", JOptionPane.QUESTION_MESSAGE);
        if (username == null || username.isBlank()) {
            return;
        }

        try {
            var question = adminService.getSecurityQuestion(username);
            if (question.isEmpty()) {
                showError("No recovery question is set for that account. "
                        + "Sign in and configure recovery first.");
                return;
            }

            JPasswordField answerField = new JPasswordField(18);
            int answerResult = JOptionPane.showConfirmDialog(
                    this,
                    new Object[]{question.get(), answerField},
                    "Password Recovery",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answerResult != JOptionPane.OK_OPTION) {
                return;
            }
            String answer = new String(answerField.getPassword());

            JPasswordField newPassword = new JPasswordField(18);
            JPasswordField confirmPassword = new JPasswordField(18);
            int result = JOptionPane.showConfirmDialog(
                    this,
                    new Object[]{
                        "New password (at least 8 characters):",
                        newPassword,
                        "Confirm new password:",
                        confirmPassword
                    },
                    "Reset Password",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String newValue = new String(newPassword.getPassword());
            if (!newValue.equals(
                    new String(confirmPassword.getPassword()))) {
                showError("The passwords do not match.");
                return;
            }
            if (!adminService.resetPassword(username, answer, newValue)) {
                showError("The recovery answer is incorrect.");
                return;
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Password reset. You can now sign in.",
                    "Password Reset",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException | IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Parkflow",
                JOptionPane.ERROR_MESSAGE);
    }
}
