package com.posmix.mixtuvgag.models;

public class ReturnInvoiceItem {
    private int id, returnInvoiceId, productId, originalItemId;
    private String productName;
    private int quantity;
    private double unitPrice, taxPercentage, taxAmount, total;
    private String reason;

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public int getReturnInvoiceId() { return returnInvoiceId; } public void setReturnInvoiceId(int r) { returnInvoiceId = r; }
    public int getProductId() { return productId; } public void setProductId(int p) { productId = p; }
    public int getOriginalItemId() { return originalItemId; } public void setOriginalItemId(int o) { originalItemId = o; }
    public String getProductName() { return productName; } public void setProductName(String n) { productName = n; }
    public int getQuantity() { return quantity; } public void setQuantity(int q) { quantity = q; }
    public double getUnitPrice() { return unitPrice; } public void setUnitPrice(double u) { unitPrice = u; }
    public double getTaxPercentage() { return taxPercentage; } public void setTaxPercentage(double t) { taxPercentage = t; }
    public double getTaxAmount() { return taxAmount; } public void setTaxAmount(double t) { taxAmount = t; }
    public double getTotal() { return total; } public void setTotal(double t) { total = t; }
    public String getReason() { return reason; } public void setReason(String r) { reason = r; }
}
