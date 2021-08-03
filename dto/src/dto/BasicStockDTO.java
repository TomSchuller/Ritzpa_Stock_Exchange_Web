package dto;

public interface BasicStockDTO {
    // func 2 - no need of the trading arrays
    String getSymbol();

    String getCompany();

    Integer getValue();

    Integer getTotalCycle();
}
