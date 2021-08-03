package dto;

import java.util.List;

public interface AdvancedStockDTO extends BasicStockDTO {
    List<TransactionDTO> getPurchases();

    List<TransactionDTO> getSells();

    List<TransactionDTO> getTransactions();
}
