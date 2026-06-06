package com.posmix.mixtuvgag.models;

import java.util.Objects;

public class Product {
    private int id;
    private String name, barcode;
    private double buyPrice, sellPrice, wholesalePrice, taxPercentage;
    private int stockQuantity, minStockAlert;
    private boolean isActive = true;
    private String notes, imagePath;
    private int categoryId;
    private String categoryName;
    private int baseUnitId;
    private String baseUnitName;

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getBarcode() { return barcode; } public void setBarcode(String barcode) { this.barcode = barcode; }
    public double getBuyPrice() { return buyPrice; } public void setBuyPrice(double b) { buyPrice = b; }
    public double getSellPrice() { return sellPrice; } public void setSellPrice(double s) { sellPrice = s; }
    public double getWholesalePrice() { return wholesalePrice; } public void setWholesalePrice(double w) { wholesalePrice = w; }
    public int getStockQuantity() { return stockQuantity; } public void setStockQuantity(int q) { stockQuantity = q; }
    public int getMinStockAlert() { return minStockAlert; } public void setMinStockAlert(int m) { minStockAlert = m; }
    public double getTaxPercentage() { return taxPercentage; } public void setTaxPercentage(double t) { taxPercentage = t; }
    public boolean isActive() { return isActive; } public void setActive(boolean a) { this.isActive = a; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
    public String getImagePath() { return imagePath; } public void setImagePath(String i) { imagePath = i; }
    public int getCategoryId() { return categoryId; } public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; } public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public int getBaseUnitId() { return baseUnitId; } public void setBaseUnitId(int baseUnitId) { this.baseUnitId = baseUnitId; }
    public String getBaseUnitName() { return baseUnitName; } public void setBaseUnitName(String baseUnitName) { this.baseUnitName = baseUnitName; }

    public boolean isLowStock() { return stockQuantity <= minStockAlert && stockQuantity >= 0; }
    public double getProfit() { return sellPrice - buyPrice; }
    public double getProfitMargin() { return buyPrice > 0 ? ((sellPrice - buyPrice) / sellPrice) * 100 : 0; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product p = (Product) o;
        return id == p.id &&
               Double.compare(p.sellPrice, sellPrice) == 0 &&
               stockQuantity == p.stockQuantity &&
               isActive == p.isActive &&
               categoryId == p.categoryId &&
               Objects.equals(name, p.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, sellPrice, stockQuantity, isActive, categoryId);
    }
}
