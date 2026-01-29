package com.mcp_asd.burp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ConnectionDialog extends JDialog {
    private JTextField hostField;
    private JTextField portField;
    private JComboBox<String> transportCombo;
    private JTextField pathField;
    private boolean confirmed = false;

    public ConnectionDialog(Frame owner, String defaultHost, int defaultPort) {
        super(owner, "Connect to MCP Server", true);
        setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Row 0: Host
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Host:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        hostField = new JTextField(defaultHost);
        formPanel.add(hostField, gbc);

        // Row 1: Port
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Port:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        portField = new JTextField(String.valueOf(defaultPort));
        formPanel.add(portField, gbc);

        // Row 2: Auto-Detect Button (Spanning)
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton detectButton = new JButton("Auto-Detect Endpoints");
        formPanel.add(detectButton, gbc);
        
        detectButton.addActionListener(e -> {
            detectButton.setEnabled(false);
            detectButton.setText("Scanning...");
            AutoDetector.detect(getHost(), getPort()).thenAccept(results -> {
                SwingUtilities.invokeLater(() -> {
                    detectButton.setEnabled(true);
                    detectButton.setText("Auto-Detect Endpoints");
                    
                                        if (!results.isEmpty()) {
                                            if (results.size() == 1) {
                                                AutoDetector.DetectionResult res = results.get(0);
                                                transportCombo.setSelectedItem(res.transport);
                                                pathField.setText(res.path);
                                                JOptionPane.showMessageDialog(this, "Found " + res.transport + " endpoint at " + res.path, "Detection Successful", JOptionPane.INFORMATION_MESSAGE);
                                                                    } else {
                                                                        // Multiple found, let user choose with a wider message
                                                                        AutoDetector.DetectionResult selected = (AutoDetector.DetectionResult) JOptionPane.showInputDialog(
                                                                            this, 
                                                                            "Multiple MCP endpoints were detected on this server.\nPlease select the one you want to use:",
                                                                            "Select Endpoint",
                                                                            JOptionPane.QUESTION_MESSAGE,
                                                                            null,
                                                                            results.toArray(),
                                                                            results.get(0)
                                                                        );
                                                                        
                                                                        if (selected != null) {                                                    transportCombo.setSelectedItem(selected.transport);
                                                    pathField.setText(selected.path);
                                                }
                                            }
                                        } else {
                                            JOptionPane.showMessageDialog(this, "No common MCP endpoint found.", "Detection Failed", JOptionPane.WARNING_MESSAGE);
                                        }
                                    });
                                });
                            });
                    
                            // Row 3: Transport
                            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.0;
                            formPanel.add(new JLabel("Transport:"), gbc);
                            
                    gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
                            transportCombo = new JComboBox<>(new String[]{"SSE", "WebSocket"});
                            transportCombo.addActionListener(e -> {
                                if ("WebSocket".equals(transportCombo.getSelectedItem())) {
                                    pathField.setText("/ws");
                                }
                                else {
                                    pathField.setText("/mcp");
                                }
                            });
                            formPanel.add(transportCombo, gbc);
                    
                            // Row 4: Path
                            gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
                            formPanel.add(new JLabel("Path:"), gbc);
                            
                    gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
                            pathField = new JTextField("/mcp");
                            formPanel.add(pathField, gbc);
                    
                            add(formPanel, BorderLayout.CENTER);
                    
                            JPanel buttonPanel = new JPanel();
                            JButton connectButton = new JButton("Connect");
                            connectButton.setPreferredSize(new Dimension(100, 30));
                            connectButton.addActionListener(e -> {
                                confirmed = true;
                                dispose();
                            });
                            JButton cancelButton = new JButton("Cancel");
                            cancelButton.setPreferredSize(new Dimension(100, 30));
                            cancelButton.addActionListener(e -> dispose());
                            
                            buttonPanel.add(connectButton);
                            buttonPanel.add(cancelButton);
                            add(buttonPanel, BorderLayout.SOUTH);
                    
                            setMinimumSize(new Dimension(450, 300));
                            pack();
                            setLocationRelativeTo(owner);
                        }
    public boolean isConfirmed() {
        return confirmed;
    }

    public String getHost() {
        return hostField.getText();
    }

    public int getPort() {
        try {
            return Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            return 80;
        }
    }

    public String getTransport() {
        return (String) transportCombo.getSelectedItem();
    }

    public String getPath() {
        return pathField.getText();
    }
}
