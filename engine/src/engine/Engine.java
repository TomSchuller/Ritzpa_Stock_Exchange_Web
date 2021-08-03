package engine;


import dto.Action;
import dto.StockDTO;
import dto.TransactionDTO;
import dto.UserDTO;

import java.io.InputStream;
import java.util.List;

public interface Engine  {
    /**
     *
     * @param inputStream
     */
    void load(InputStream inputStream, String username) throws Exception;

    /**
     *List<StockDTO>
     */
    List<String> getStocksNames();

    /**
     *
     * @param SYMBOL
     */
    StockDTO getStock(String SYMBOL);

    List<StockDTO> getAllStocks();

    List<UserDTO> getAllUsers();

    UserDTO getUser(String name);

    List<String> getUsersNames();

    List<String> getUsersTypes();

    List<TransactionDTO> order(int lmtMkr, int type, String stockSymbol, int amount, int price, String userName) throws IllegalArgumentException;

/*
    void startThread(LoadFileTask task);
*/
    String isCompanyExists(String company);

    boolean isUserExists(String name);

    void addUser(String name, String type);

    void addChatString(String chatString, String username);

    int getVersion();

    List<SingleChatEntry> getChatEntries(int fromIndex);

    void addMoney(int val, String username) throws Exception;

    int getTotalMoneyPerUser(String username) throws Exception;

    void addStock(String company, String symbol, int quantity, int value, String username) throws Exception;

    void addAction(Action newAction, String username) throws Exception;

    void addAlert(String username, String alert);

    List<String> getUserAlerts(String username);
}
