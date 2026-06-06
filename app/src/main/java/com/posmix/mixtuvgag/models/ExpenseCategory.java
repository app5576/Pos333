package com.posmix.mixtuvgag.models;

public class ExpenseCategory {
    private int id;
    private String name;
    private boolean isDefault;

    public ExpenseCategory() {}
    public ExpenseCategory(String name, boolean isDefault) { this.name = name; this.isDefault = isDefault; }

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public String getName() { return name; } public void setName(String n) { name = n; }
    public boolean isDefault() { return isDefault; } public void setDefault(boolean d) { isDefault = d; }
}
