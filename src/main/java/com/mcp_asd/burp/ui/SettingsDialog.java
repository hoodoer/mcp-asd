package com.mcp_asd.burp.ui;

import com.mcp_asd.burp.GlobalSettings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class SettingsDialog extends JDialog {
    private final GlobalSettings settings;
    private boolean saved = false;

    private JCheckBox proxyTrafficCheckbox;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JCheckBox passiveChecksCheckbox;
    private JCheckBox activeDetectionCheckbox;
    private JCheckBox scopeOnlyCheckbox;

    public SettingsDialog(Window owner, GlobalSettings settings) {
        super(owner, "MCP-ASD Settings", ModalityType.APPLICATION_MODAL);
        this.settings = settings;
        initComponents();
        loadSettings();
        
        // Ensure a reasonable default size
        setPreferredSize(new Dimension(500, 550));
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Group 1: Internal Traffic Proxying
        JPanel proxyPanel = new JPanel(new BorderLayout(5, 5));
        proxyPanel.setBorder(BorderFactory.createTitledBorder("Internal Traffic Visibility"));
        
        JPanel proxyTop = new JPanel(new BorderLayout());
        proxyTrafficCheckbox = new JCheckBox("Proxy Handshake/Enumeration Traffic");
        proxyTop.add(proxyTrafficCheckbox, BorderLayout.NORTH);
        
        JTextArea proxyDesc = new JTextArea("Route internal MCP traffic through Burp's Proxy listener.\nThis allows you to see the 'invisible' handshake and enumeration requests in Burp's Logger and Proxy History.");
        proxyDesc.setWrapStyleWord(true);
        proxyDesc.setLineWrap(true);
        proxyDesc.setEditable(false);
        proxyDesc.setOpaque(false);
        proxyDesc.setFont(proxyDesc.getFont().deriveFont(11f));
        proxyDesc.setBorder(BorderFactory.createEmptyBorder(0, 25, 5, 5));
        proxyTop.add(proxyDesc, BorderLayout.CENTER);
        
        JPanel proxyConfig = new JPanel(new FlowLayout(FlowLayout.LEFT));
        proxyConfig.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        proxyConfig.add(new JLabel("Proxy Host:"));
        proxyHostField = new JTextField(10);
        proxyConfig.add(proxyHostField);
        proxyConfig.add(new JLabel("Port:"));
        proxyPortField = new JTextField(5);
        proxyConfig.add(proxyPortField);
        
        proxyPanel.add(proxyTop, BorderLayout.NORTH);
        proxyPanel.add(proxyConfig, BorderLayout.CENTER);
        
        proxyTrafficCheckbox.addActionListener(e -> {
            boolean selected = proxyTrafficCheckbox.isSelected();
            proxyHostField.setEnabled(selected);
            proxyPortField.setEnabled(selected);
        });

        // Group 2: Detection
        JPanel detectionPanel = new JPanel(new BorderLayout(5, 5));
        detectionPanel.setBorder(BorderFactory.createTitledBorder("MCP Server Detection"));
        
        JPanel detectionInner = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 2, 2, 2);
        
        // Passive
        passiveChecksCheckbox = new JCheckBox("Passive Checks for MCP Server");
        detectionInner.add(passiveChecksCheckbox, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(0, 25, 10, 5); // Indent description
        JTextArea passiveDesc = new JTextArea("Monitor traffic headers and bodies for MCP indicators (e.g., 'MCP-Protocol-Version', 'mcp-session-id'). Raises an Issue if detected.");
        passiveDesc.setWrapStyleWord(true);
        passiveDesc.setLineWrap(true);
        passiveDesc.setEditable(false);
        passiveDesc.setOpaque(false);
        passiveDesc.setFont(passiveDesc.getFont().deriveFont(11f));
        detectionInner.add(passiveDesc, gbc);
        
        // Active
        gbc.gridy++;
        gbc.insets = new Insets(5, 2, 2, 2); // Reset indent
        activeDetectionCheckbox = new JCheckBox("Active Detection");
        detectionInner.add(activeDetectionCheckbox, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(0, 25, 5, 5); // Indent description
        JTextArea activeDesc = new JTextArea("Actively probe new domains for common MCP endpoints (e.g., /mcp, /sse, /ws). Triggers when a domain is first seen.");
        activeDesc.setWrapStyleWord(true);
        activeDesc.setLineWrap(true);
        activeDesc.setEditable(false);
        activeDesc.setOpaque(false);
        activeDesc.setFont(activeDesc.getFont().deriveFont(11f));
        detectionInner.add(activeDesc, gbc);
        
        // Scope Only (The 4th Option)
        gbc.gridy++;
        gbc.insets = new Insets(0, 25, 5, 5); // Indent to match description
        scopeOnlyCheckbox = new JCheckBox("Limit Active Detection to In-Scope items only");
        detectionInner.add(scopeOnlyCheckbox, gbc);
        
        // Spacer at bottom to push everything up if resized
        gbc.gridy++;
        gbc.weighty = 1.0;
        detectionInner.add(Box.createVerticalGlue(), gbc);
        
        activeDetectionCheckbox.addActionListener(e -> scopeOnlyCheckbox.setEnabled(activeDetectionCheckbox.isSelected()));
        
        detectionPanel.add(detectionInner, BorderLayout.CENTER);

        mainPanel.add(proxyPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(detectionPanel);
        
        contentPane.add(mainPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            saveSettings();
            saved = true;
            dispose();
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    private void loadSettings() {
        proxyTrafficCheckbox.setSelected(settings.isProxyTrafficEnabled());
        proxyHostField.setText(settings.getProxyHost());
        proxyPortField.setText(String.valueOf(settings.getProxyPort()));
        proxyHostField.setEnabled(settings.isProxyTrafficEnabled());
        proxyPortField.setEnabled(settings.isProxyTrafficEnabled());

        passiveChecksCheckbox.setSelected(settings.isPassiveChecksEnabled());
        activeDetectionCheckbox.setSelected(settings.isActiveDetectionEnabled());
        scopeOnlyCheckbox.setSelected(settings.isScopeOnlyEnabled());
        scopeOnlyCheckbox.setEnabled(settings.isActiveDetectionEnabled());
    }

    private void saveSettings() {
        settings.setProxyTrafficEnabled(proxyTrafficCheckbox.isSelected());
        settings.setProxyHost(proxyHostField.getText().trim());
        try {
            settings.setProxyPort(Integer.parseInt(proxyPortField.getText().trim()));
        } catch (NumberFormatException e) {
            // Ignore or set default
        }
        
        settings.setPassiveChecksEnabled(passiveChecksCheckbox.isSelected());
        settings.setActiveDetectionEnabled(activeDetectionCheckbox.isSelected());
        settings.setScopeOnlyEnabled(scopeOnlyCheckbox.isSelected());
    }
    
    public boolean isSaved() {
        return saved;
    }
}
