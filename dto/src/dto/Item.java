package dto;

public class Item {
    private final String symbol;
    private Integer quantity;
    private Integer realQuantity;
    private Integer value;

    public Item(String newSymbol, int newQuantity, int newValue){
        symbol = newSymbol;
        quantity = newQuantity;
        realQuantity = newQuantity;
        value = newValue;
    }

    public String getSymbol() { return symbol; }
    public Integer getQuantity() { return quantity; }
    public Integer getRealQuantity() { return realQuantity; }
    public Integer getValue() { return value; }

    public void setQuantity(int newQuantity) {
        quantity = newQuantity;
    }
    public void setRealQuantity(int newQuantity) {
        realQuantity = newQuantity;
    }
    public void setValue(int newValue) {
        value = newValue;
    }
}
