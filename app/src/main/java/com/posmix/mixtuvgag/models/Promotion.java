package com.posmix.mixtuvgag.models;

public class Promotion {
    public static final int TYPE_PERCENTAGE = 1;
    public static final int TYPE_FIXED = 2;
    public static final int TYPE_BUY_X_GET_Y = 3;

    private int id, type, productId, categoryId;
    private String name, code, productName;
    private double discountValue, minAmount;
    private int buyQty, getQty;
    private long startDate, endDate;
    private boolean isActive = true;

    public int getId() { return id; } public void setId(int id) { this.id = id; }
    public String getName() { return name; } public void setName(String n) { name = n; }
    public String getCode() { return code; } public void setCode(String c) { code = c; }
    public int getType() { return type; } public void setType(int t) { type = t; }
    public int getProductId() { return productId; } public void setProductId(int p) { productId = p; }
    public int getCategoryId() { return categoryId; } public void setCategoryId(int c) { categoryId = c; }
    public String getProductName() { return productName; } public void setProductName(String n) { productName = n; }
    public double getDiscountValue() { return discountValue; } public void setDiscountValue(double d) { discountValue = d; }
    public double getMinAmount() { return minAmount; } public void setMinAmount(double m) { minAmount = m; }
    public int getBuyQty() { return buyQty; } public void setBuyQty(int b) { buyQty = b; }
    public int getGetQty() { return getQty; } public void setGetQty(int g) { getQty = g; }
    public long getStartDate() { return startDate; } public void setStartDate(long s) { startDate = s; }
    public long getEndDate() { return endDate; } public void setEndDate(long e) { endDate = e; }
    public boolean isActive() { return isActive; } public void setActive(boolean a) { isActive = a; }

    public String getTypeName() {
        switch (type) {
            case TYPE_PERCENTAGE: return "خصم نسبة مئوية";
            case TYPE_FIXED: return "خصم مبلغ ثابت";
            case TYPE_BUY_X_GET_Y: return "اشتري X واحصل على Y";
            default: return "غير محدد";
        }
    }
}
