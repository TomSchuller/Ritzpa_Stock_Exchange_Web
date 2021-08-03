package dto;

public class TransactionDTO {
    private final int value;
    private final int quantity;
    private final int cycle;
    private final String timeStamp;
    private final String type;
    private final String buyerName;
    private final String sellerName;
    private final boolean isBuyerFull;
    private final boolean isSellerFull;

    public Integer getCycle() { return cycle; }
    public Integer getValue() { return value; }
    public Integer getQuantity() { return quantity; }
    public String getTimeStamp() { return timeStamp; }
    public String getType() { return type; }
    public boolean getIsBuyerFull() { return isBuyerFull; }
    public boolean getIsSellerFull() { return isSellerFull; }

    public String getBuyerName() {
        return buyerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public TransactionDTO(int newValue, int newQuantity, int newCycle, String newTimeStamp, int newType, String newBuyerName, String newSellerName, boolean newIsBuyerFull, boolean newIsSellerFull){
        this.value = newValue;
        this.quantity = newQuantity;
        this.timeStamp = newTimeStamp;
        this.cycle = newCycle;
        if (newType == 1) this.type = "LMT";
        else if (newType == 2) this.type = "MKT";
        else if (newType == 3) this.type = "FOK";
        else this.type = "IOC";
        this.buyerName = newBuyerName;
        this.sellerName = newSellerName;
        this.isBuyerFull = newIsBuyerFull;
        this.isSellerFull = newIsSellerFull;
    }
}
