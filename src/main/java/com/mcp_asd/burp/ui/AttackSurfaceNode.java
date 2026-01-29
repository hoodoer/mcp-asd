package com.mcp_asd.burp.ui;

import org.json.JSONObject;

public class AttackSurfaceNode {
    private final String name;
    private final JSONObject data;

    public AttackSurfaceNode(String name, JSONObject data) {
        this.name = name;
        this.data = data;
    }

    public JSONObject getData() {
        return data;
    }

    @Override
    public String toString() {
        return name; // This is what is displayed in the JTree
    }
}
