package com.posmix.mixtuvgag.models;
import java.util.List;

public class ReturnInvoice {
    public static final int TYPE_SALE_RETURN = 1;
    public static final int TYPE_PURCHASE_RETURN = 2;

    private int id, type, originalInvoiceId;
    private String returnNumber, notes;
    private Integer customerId, supplierId;
    private String customerName, supplierName;
    private long date;
    private double subtotal, taxAmount, discount, total;
    private String originalInvoiceNumber;
    private List<ReturnInvoiceItem> items;

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public int getType() { return type; } public void setType(int t) { type = t; }
    public String getReturnNumber() { return returnNumber; } public void setReturnNumber(String n) { returnNumber = n; }
    public int getOriginalInvoiceId() { return originalInvoiceId; } public void setOriginalInvoiceId(int i) { originalInvoiceId = i; }
    public String getOriginalInvoiceNumber() { return originalInvoiceNumber; } public void setOriginalInvoiceNumber(String n) { originalInvoiceNumber = n; }
    public Integer getCustomerId() { return customerId; } public void setCustomerId(Integer c) { customerId = c; }
    public Integer getSupplierId() { return supplierId; } public void setSupplierId(Integer s) { supplierId = s; }
    public String getCustomerName() { return customerName; } public void setCustomerName(String n) { customerName = n; }
    public String getSupplierName() { return supplierName; } public void setSupplierName(String n) { supplierName = n; }
    public long getDate() { return date; } public void setDate(long d) { date = d; }
    public double getSubtotal() { return subtotal; } public void setSubtotal(double s) { subtotal = s; }
    public double getTaxAmount() { return taxAmount; } public void setTaxAmount(double t) { taxAmount = t; }
    public double getDiscount() { return discount; } public void setDiscount(double d) { discount = d; }
    public double getTotal() { return total; } public void setTotal(double t) { total = t; }
    public String getNotes() { return notes; } public void setNotes(String n) { notes = n; }
    public List<ReturnInvoiceItem> getItems() { return items; } public void setItems(List<ReturnInvoiceItem> items) { this.items = items; }
}
