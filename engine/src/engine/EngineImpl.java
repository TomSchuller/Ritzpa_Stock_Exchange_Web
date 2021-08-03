package engine;

import dto.*;
import engine.exception.ContainsCompanyException;
import engine.exception.ContainsSymbolException;
import engine.exception.MissingStockException;
import engine.jaxb.schema.generated.RizpaStockExchangeDescriptor;
import engine.jaxb.schema.generated.RseItem;
import engine.jaxb.schema.generated.RseStock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class EngineImpl implements Engine, Serializable {
    private Map<String, Stock> stocks;
    private List<String> stocksNames;

    private Map<String, User> users;
    private List<String> userNames;

    private final List<SingleChatEntry> chatDataList;
    private final Map<String, List<String>> usersAlerts;

    public EngineImpl() {
        this.stocks = new HashMap<>();
        this.stocksNames = new ArrayList<>();

        this.users = new HashMap<>();
        this.userNames = new ArrayList<>();

        chatDataList = new ArrayList<>();

        usersAlerts = new HashMap<>();
    }

    public synchronized void addUser(String name, String type) {
       //if im here the user doesnt exsists. so i dont need to check
        User newUser = new User(name, type); //dont have items yet
        users.put(name, newUser);
        userNames.add(name);
    }

    @Override
    public boolean isUserExists(String name) {
        return userNames.contains(name);
    }

    @Override
    public synchronized void addChatString(String chatString, String username) {
        chatDataList.add(new SingleChatEntry(chatString, username));
    }

    @Override
    public synchronized List<SingleChatEntry> getChatEntries(int fromIndex){
        if (fromIndex < 0 || fromIndex > chatDataList.size()) {
            fromIndex = 0;
        }
        return chatDataList.subList(fromIndex, chatDataList.size());
    }

    @Override
    public int getVersion() {
        return chatDataList.size();
    }

    @Override
    public void load(InputStream inputStream, String username) throws Exception {
        RizpaStockExchangeDescriptor stockz;
        try {
            stockz = deserializeFrom(inputStream);
        } catch (JAXBException ex) {
            throw new Exception("ERROR! The XML file is corrupted" + System.lineSeparator());
        }
        // Fetching stocks
        List<String> stocksValidNames = new ArrayList<>();
        List<String> stocksValidNamesButTaken = new ArrayList<>();
        try {
            for (RseStock currStock : stockz.getRseStocks().getRseStock()) {
                Stock newStock = new Stock(currStock.getRseSymbol(), currStock.getRseCompanyName(), currStock.getRsePrice());

                // if we try to declare of a new stock but it already exists with our stocks map
                if (stocks.containsKey(newStock.getSymbol())) {
                    Stock stockWithSameSymbol = stocks.get(newStock.getSymbol());
                    if (stockWithSameSymbol.getCompany().equals(newStock.getCompany())) {
                        //we already have it so i dont need to add it to out map
                        stocksValidNamesButTaken.add(newStock.getSymbol());
                        stocksValidNames.add(newStock.getSymbol());
                    }
                    else { //we try to add a new stock but with a taken symbol
                        throw new ContainsSymbolException(newStock, stockWithSameSymbol);
                    }
                }

                // if we try to declare of a new stock but it already exists with a diffrent company
                Stock stockWithSameCompany = containsCompany(currStock.getRseCompanyName(), stocksNames, stocks);
                if (stockWithSameCompany != null) {
                    if (!(stockWithSameCompany.getSymbol().equals(newStock.getSymbol()))) {
                        throw new ContainsCompanyException(newStock, stockWithSameCompany);
                    }
                    // else - we can ignore it already added to the list
                }
                // all good add to the map
                if (!(stocksValidNamesButTaken.contains(currStock.getRseSymbol()))) {
                    stocks.put(currStock.getRseSymbol(), newStock);
                    stocksNames.add(currStock.getRseSymbol());
                    stocksValidNames.add(newStock.getSymbol());
                }
            }
            // Fetching Items
            List<Item> newItems = new ArrayList<>();
            int investment = 0;
            for (RseItem currItem : stockz.getRseHoldings().getRseItem()) {
                // If the stock doesn't exist in the xml
                if (!stocksValidNames.contains(currItem.getSymbol())) {
                    Item newItem = new Item(currItem.getSymbol(), currItem.getQuantity(), 0);
                    throw new MissingStockException(newItem);
                }
                Item newItem = new Item(currItem.getSymbol(), currItem.getQuantity(), stocks.get(currItem.getSymbol()).getValue());

                newItems.add(newItem);
                investment += newItem.getQuantity() * newItem.getValue();
            }
            // add items to user
            users.get(username).addInvestmentMoney(investment);

            //users.get(username).addItems(newItems);
            User userToAdd = users.get(username);
            for(Item item : newItems) {
                if(userToAdd.findItem(item.getSymbol()) != null) { // an old item
                    userToAdd.addQuantity(item.getSymbol(), item.getQuantity());
                }
                else { // a new item
                    userToAdd.getItems().add(item);
                }
            }

            //creating the action
            String timeStamp = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
            int beforeBalance = getUser(username).getMoney();

            Action newAction = new Action(7, "", timeStamp, 0, beforeBalance, beforeBalance);
            addAction(newAction, username);

            } catch (Exception ex) {
            // delete all the names that are already exists in the engine before
            for (String name : stocksValidNamesButTaken) {
                stocksValidNames.remove(name);
            }

            //delete all names i put and throw the exception
            for (String name : stocksValidNames) {
                stocks.remove(name);
                stocksNames.remove(name);
            }
            throw ex;
        }
    }

    private static RizpaStockExchangeDescriptor deserializeFrom(InputStream in) throws JAXBException {
        final String JAXB_XML_GAME_PACKAGE_NAME = "engine.jaxb.schema.generated";
        JAXBContext jc = JAXBContext.newInstance(JAXB_XML_GAME_PACKAGE_NAME);
        Unmarshaller u = jc.createUnmarshaller();
        return (RizpaStockExchangeDescriptor) u.unmarshal(in);
    }

    private Stock containsCompany(String companyName,List<String> stocksNames, Map<String,Stock> stocks) {
        for (String symbol : stocksNames) {
            if (stocks.get(symbol).getCompany().equals(companyName))
                return stocks.get(symbol);
        }
        return null;
    }

    @Override
    public synchronized List<String> getStocksNames() {
        return stocksNames;
    }

    @Override
    public synchronized StockDTO getStock(String stockSymbol) {
        String name = findStockByName(stockSymbol);
        if (name == null) {
            return null;
        }

        Stock stock = stocks.get(name);

        List<TransactionDTO> allTransactions = new ArrayList<>();
        List<TransactionDTO> purchases = new ArrayList<>();
        List<TransactionDTO> sells = new ArrayList<>();

        for (Transaction tr : stock.getPurchases()) {
            purchases.add(new TransactionDTO(tr.getValue(), tr.getQuantity(), tr.getCycle(), tr.getTimeStamp(), tr.getTypeTrType(), tr.getBuyerName(), tr.getSellerName(), false, false));
        }
        for (Transaction tr : stock.getSells()) {
            sells.add(new TransactionDTO(tr.getValue(), tr.getQuantity(), tr.getCycle(), tr.getTimeStamp(), tr.getTypeTrType(), tr.getBuyerName(), tr.getSellerName(), false, false));
        }
        for (Transaction tr : stock.getCompletedTransactions()) {
            allTransactions.add(new TransactionDTO(tr.getValue(), tr.getQuantity(), tr.getCycle(), tr.getTimeStamp(), tr.getTypeTrType(), tr.getBuyerName(), tr.getSellerName(), false, false));
        }
        return new StockDTO(stock.getSymbol(), stock.getCompany(), stock.getValue(), stock.getTotalCycle(), purchases, sells, allTransactions, stock.getValueHistory());
    }

    @Override
    public synchronized List<TransactionDTO> order(int trType, int direction, String stockSymbol, int amount, int price, String userName) throws IllegalArgumentException {
        String name = findStockByName(stockSymbol);
        if (name == null) {
            throw new IllegalArgumentException("ERROR! " + stockSymbol + " doesn't exist!");
        }
        // name can't be null if im here
        Stock stock = stocks.get(name);

        Transaction transaction;

        if (direction == 1) {
            transaction = new Transaction(price, amount, direction, trType, userName, null);  // add TR direction, LMT MKR enums
        }
        else if (direction == 2) {
            transaction = new Transaction(price, amount, direction, trType, null, userName);  // add TR type, LMT MKR enums
        }
        else { //its saved for deals closed
            throw new IllegalArgumentException("ERROR! You've entered " + direction + ", an invalid type of stocks." + System.lineSeparator() + "You can only enter 1 to buy or 2 to sell the stock.");
        }


        if (trType == 1) {
            return limit(transaction, stock, false, false);
        }
        else if (trType == 2) {
            return mkt(transaction, stock);
        }
        else if (trType == 3) {
             return fok(transaction, stock);
        }
        else {
            return limit(transaction, stock, false, true);
        }
        // maybe add else throw exception
    }

    @Override
    public String isCompanyExists(String company) {
        for (String stock : stocksNames) {
            if (stocks.get(stock).getCompany().equals(company)) return stock;
        }
        return null;
    }


    public synchronized List<TransactionDTO> mkt(Transaction transaction, Stock stock) {
        int price;
        if(transaction.getDirection() == 1) { // to buy
            if(stock.getSells().size() == 0) {
                price = stock.getValue();
            }
            else {
                price = stock.getSells().first().getValue();
            }
        }
        else { //  2 to sell
            if(stock.getPurchases().size() == 0) {
                price = stock.getValue();
            }
            else {
                price = stock.getPurchases().first().getValue();
            }
        }
        transaction.setValue(price);
        return limit(transaction, stock, true, false);
    }

    public synchronized List<TransactionDTO> limit(Transaction transaction, Stock stock, boolean isMkt, boolean isIoc) {
        List<TransactionDTO>dealsMade = new ArrayList<>();
        List<Transaction>SameUserList = new ArrayList<>();

        if(transaction.getDirection() == 1) { // we want to buy
            int size = stock.getSells().size();
            for(int i = 0; i < size; ++i) {
                Transaction sell = stock.getSells().first();

                // check if the sell offer isn't from the same user- if it is skip to next
                if(transaction.getBuyerName().equals(sell.getSellerName())) {
                    //skip next person
                    SameUserList.add(sell);
                    stock.getSells().remove(sell);
                }
                else if(sell.getValue() > transaction.getValue() && !isMkt) { // too high cant buy
                    if(!isIoc) {
                        stock.getPurchases().add(transaction);
                    }
                    // add the same user list
                    for (Transaction selr : SameUserList ) {
                        stock.getSells().add(selr);
                    }
                        return dealsMade;
                }
                else { // the price is good for us
                    if(transaction.getQuantity() == sell.getQuantity()) {
                        Transaction deal = new Transaction(sell.getValue(), sell.getQuantity(), 3, transaction.getTypeTrType(), transaction.getBuyerName(), sell.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), true, true);
                        dealsMade.add(made);
                        stock.getCompletedTransactions().add(deal);
                        stock.setValue(deal.getValue(), deal.getTimeStamp());// after every deal update stock val
                        // Add full purchase action
                        int beforeBalance = users.get(made.getBuyerName()).getMoney();
                        int actionFee = made.getQuantity()*made.getValue();
                        int afterBalance = beforeBalance - actionFee;
                        Action newAction = new Action(2, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getBuyerName());
                        // Add full sell action
                        beforeBalance = users.get(made.getSellerName()).getMoney();
                        actionFee = made.getQuantity()*made.getValue();
                        afterBalance = beforeBalance + actionFee;
                        newAction = new Action(3, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getSellerName());

                        // Update users
                        updateUsers(deal.getBuyerName(), deal.getSellerName(), deal.getQuantity(), stock.getSymbol(), stock.getValue());

                        stock.getSells().remove(sell);

                        // add the same user list
                        for (Transaction selr : SameUserList ) {
                            stock.getSells().add(selr);
                        }

                        return dealsMade;
                    }
                    else if(transaction.getQuantity() < sell.getQuantity()) { // i buy less then you sell
                        //pop to sell and add to green
                        Transaction deal = new Transaction(sell.getValue(), transaction.getQuantity(), 3, transaction.getTypeTrType(), transaction.getBuyerName(), sell.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), true, false);
                        dealsMade.add(made);
                        stock.getCompletedTransactions().add(deal);
                        stock.setValue(deal.getValue(), deal.getTimeStamp());// after every deal update stock val
                        // Add full purchase action
                        int beforeBalance = users.get(made.getBuyerName()).getMoney();
                        int actionFee = made.getQuantity()*made.getValue();
                        int afterBalance = beforeBalance - actionFee;
                        Action newAction = new Action(2, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getBuyerName());
                        // Add partial sell action
                        beforeBalance = users.get(made.getSellerName()).getMoney();
                        actionFee = made.getQuantity()*made.getValue();
                        afterBalance = beforeBalance + actionFee;
                        newAction = new Action(5, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getSellerName());

                        updateUsers(deal.getBuyerName(), deal.getSellerName(), deal.getQuantity(), stock.getSymbol(), stock.getValue());
                        sell.setQuantity(sell.getQuantity() - transaction.getQuantity());

                        // add the same user list
                        for (Transaction selr : SameUserList ) {
                            stock.getSells().add(selr);
                        }

                        return dealsMade;
                    }
                    else { // i buy all you sell, but i want to buy more
                        Transaction deal = new Transaction(sell.getValue(), sell.getQuantity(), 3, transaction.getTypeTrType(), transaction.getBuyerName(), sell.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), false, true);
                        dealsMade.add(made);
                        stock.getCompletedTransactions().add(deal);
                        stock.setValue(deal.getValue(), deal.getTimeStamp());// after every deal update stock val
                        // Add partial purchase action
                        int beforeBalance = users.get(made.getBuyerName()).getMoney();
                        int actionFee = made.getQuantity()*made.getValue();
                        int afterBalance = beforeBalance - actionFee;
                        Action newAction = new Action(4, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getBuyerName());
                        // Add full sell action
                        beforeBalance = users.get(made.getSellerName()).getMoney();
                        actionFee = made.getQuantity()*made.getValue();
                        afterBalance = beforeBalance + actionFee;
                        newAction = new Action(3, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getSellerName());

                        updateUsers(deal.getBuyerName(), deal.getSellerName(), deal.getQuantity(), stock.getSymbol(), stock.getValue());
                        transaction.setQuantity(transaction.getQuantity()-sell.getQuantity());
                        stock.getSells().remove(sell);
                    }
                }
            } // end of loop
            if((transaction.getQuantity() > 0) && (!isIoc)) {
                if(isMkt) {
                    transaction.setValue(stock.getValue()); // update transaction value by last deal
                }
                stock.getPurchases().add(transaction);
            }
            if(SameUserList.size() > 0) {
                //return the moves transactions fro, same user
                for(Transaction tr : SameUserList) {
                    stock.getSells().add(tr);
                }
            }
        } // type 1

        else { //  type 2 i want to sell
            int size = stock.getPurchases().size();
            for(int i = 0; i < size; ++i) { // i=0; i<sells.size
                Transaction purchase = stock.getPurchases().first(); //peek();

                if(transaction.getSellerName().equals(purchase.getBuyerName())) {
                    //skip next person
                    SameUserList.add(purchase);
                    stock.getPurchases().remove(purchase);
                }
                else if(purchase.getValue() < transaction.getValue() && !isMkt) { // too low won't sell
                    if(!isIoc) {
                        // add to waiting list
                        stock.getSells().add(transaction);
                        // update user real quantity of the stock
                        updateRealQuantity(transaction.getSellerName(), stock.getSymbol(), transaction.getQuantity());
                    }

                    // add the same user list
                    for (Transaction buyr : SameUserList ) {
                        stock.getSells().add(buyr);
                    }
                    // return the deals list
                    return dealsMade;
                }
                else { // the price is good for us
                    if(transaction.getQuantity() == purchase.getQuantity()) { // full deal
                        Transaction deal = new Transaction(purchase.getValue(), purchase.getQuantity(), 3, transaction.getTypeTrType(), purchase.getBuyerName(), transaction.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), true, true);
                        dealsMade.add(made);
                        stock.getCompletedTransactions().add(deal);
                        stock.setValue(deal.getValue(), deal.getTimeStamp());// after every deal update stock val
                        // Add full purchase action
                        int beforeBalance = users.get(made.getBuyerName()).getMoney();
                        int actionFee = made.getQuantity()*made.getValue();
                        int afterBalance = beforeBalance - actionFee;
                        Action newAction = new Action(2, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getBuyerName());
                        // Add full sell action
                        beforeBalance = users.get(made.getSellerName()).getMoney();
                        actionFee = made.getQuantity()*made.getValue();
                        afterBalance = beforeBalance + actionFee;
                        newAction = new Action(3, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getSellerName());

                        updateUsers(deal.getBuyerName(), deal.getSellerName(), deal.getQuantity(), stock.getSymbol(), stock.getValue());
                        updateRealQuantity(transaction.getSellerName(),stock.getSymbol(), deal.getQuantity());
                        stock.getPurchases().remove(purchase);

                        // add the same user list
                        for (Transaction buyr : SameUserList ) {
                            stock.getSells().add(buyr);
                        }

                        return dealsMade;
                    }
                    else if (transaction.getQuantity() < purchase.getQuantity()) { // i sell less then you want to buy
                        Transaction deal = new Transaction(purchase.getValue(), transaction.getQuantity(), 3, transaction.getTypeTrType(), purchase.getBuyerName(), transaction.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), false, true);
                        dealsMade.add(made);
                        stock.getCompletedTransactions().add(deal);
                        stock.setValue(deal.getValue(), deal.getTimeStamp());// after every deal update stock val

                        // Add partial purchase action
                        int beforeBalance = users.get(made.getBuyerName()).getMoney();
                        int actionFee = made.getQuantity()*made.getValue();
                        int afterBalance = beforeBalance - actionFee;
                        Action newAction = new Action(4, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getBuyerName());
                        // Add full sell action
                        beforeBalance = users.get(made.getSellerName()).getMoney();
                        actionFee = made.getQuantity()*made.getValue();
                        afterBalance = beforeBalance + actionFee;
                        newAction = new Action(3, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getSellerName());

                        updateUsers(deal.getBuyerName(), deal.getSellerName(), deal.getQuantity(), stock.getSymbol(), stock.getValue());
                        updateRealQuantity(transaction.getSellerName(),stock.getSymbol(), deal.getQuantity());
                        purchase.setQuantity(purchase.getQuantity() - transaction.getQuantity());

                        // add the same user list
                        for (Transaction buyr : SameUserList ) {
                            stock.getSells().add(buyr);
                        }

                        return dealsMade;
                    }
                    else { // i sell all you want to buy, but i want to sell more
                        Transaction deal = new Transaction(purchase.getValue(), purchase.getQuantity(), 3, transaction.getTypeTrType(), purchase.getBuyerName(), transaction.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), true, false);
                        dealsMade.add(made);
                        stock.getCompletedTransactions().add(deal);
                        stock.setValue(deal.getValue(), deal.getTimeStamp()); // after every deal update stock val

                        // Add full purchase action
                        int beforeBalance = users.get(made.getBuyerName()).getMoney();
                        int actionFee = made.getQuantity()*made.getValue();
                        int afterBalance = beforeBalance - actionFee;
                        Action newAction = new Action(2, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getBuyerName());
                        // Add partial sell action
                        beforeBalance = users.get(made.getSellerName()).getMoney();
                        actionFee = made.getQuantity()*made.getValue();
                        afterBalance = beforeBalance + actionFee;
                        newAction = new Action(5, stock.getSymbol(), made.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                        addAction(newAction, made.getSellerName());

                        updateUsers(deal.getBuyerName(), deal.getSellerName(), deal.getQuantity(), stock.getSymbol(), stock.getValue());
                        updateRealQuantity(transaction.getSellerName(),stock.getSymbol(), deal.getQuantity());
                        transaction.setQuantity(transaction.getQuantity()-purchase.getQuantity());
                        stock.getPurchases().remove(purchase);
                    }
                }
            } // end of loop
            if((transaction.getQuantity() > 0) && (!isIoc)) {
                if(isMkt) {
                    transaction.setValue(stock.getValue()); // update transaction value by last deal
                }
                stock.getSells().add(transaction);
                updateRealQuantity(transaction.getSellerName(),stock.getSymbol(), transaction.getQuantity());
            }
            //return the moves transactions fro, same user
            for(Transaction tr : SameUserList) {
                stock.getPurchases().add(tr);
            }
        } // end type 2

        return dealsMade;
    }

    public synchronized List<TransactionDTO> fok(Transaction transaction, Stock stock) throws IllegalArgumentException {
        List<TransactionDTO>dealsMade = new ArrayList<>();
        List<Transaction>SameUserList = new ArrayList<>();

        List<Transaction>waitingList = new ArrayList<>();
        List<Transaction>waitingDealsMade = new ArrayList<>();


        if(transaction.getDirection() == 1) { // we want to buy
            int size = stock.getSells().size();
            for(int i = 0; i < size; ++i) { // i=0, i<sells.size
                Transaction sell = stock.getSells().first();

                // check if the sell offer isn't from the same user- if it is skip to next
                if(transaction.getBuyerName().equals(sell.getSellerName())) {
                    //skip next person
                    SameUserList.add(sell);
                    stock.getSells().remove(sell);
                }
                else if(sell.getValue() > transaction.getValue()) { // too high cant buy
                    // cancell all i did and reutrn
                    for(Transaction redoTransactions : waitingList) {
                        stock.getSells().add(redoTransactions);
                    }

                    // add the same user list
                    for (Transaction sameUserTransaction : SameUserList ) {
                        stock.getSells().add(sameUserTransaction);
                    }

                    dealsMade.clear(); // nothing was made
                    return dealsMade;
                }
                else { // the price is good for us
                    if(transaction.getQuantity() == sell.getQuantity()) {
                        Transaction deal = new Transaction(sell.getValue(), sell.getQuantity(), 3, transaction.getTypeTrType(), transaction.getBuyerName(), sell.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), true, true);
                        dealsMade.add(made);
                        waitingDealsMade.add(deal);
                        stock.getSells().remove(sell);

                        // Create all the deals that were supposed to be made
                        for (Transaction waitingDeal : waitingDealsMade) {
                            stock.getCompletedTransactions().add(waitingDeal);
                            stock.setValue(waitingDeal.getValue(), waitingDeal.getTimeStamp()); // after every deal update stock val
                            updateUsers(waitingDeal.getBuyerName(), waitingDeal.getSellerName(), waitingDeal.getQuantity(), stock.getSymbol(), waitingDeal.getValue());
                        }

                        // add the actions
                        for (TransactionDTO waitingDeal : dealsMade){
                            // Add full purchase action
                            int beforeBalance = users.get(waitingDeal.getBuyerName()).getMoney();
                            int actionFee = waitingDeal.getCycle();     //    waitingDeal.getQuantity()*stock.getValue();
                            int afterBalance = beforeBalance - actionFee;

                            Action newAction = null;
                            if (waitingDeal.getIsBuyerFull()) {
                                newAction = new Action(2, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            else { // partly buy
                                newAction = new Action(4, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            addAction(newAction, waitingDeal.getBuyerName());

                            // Add full sell action
                            beforeBalance = users.get(waitingDeal.getSellerName()).getMoney();
                            actionFee = waitingDeal.getCycle(); //waitingDeal.getQuantity()*stock.getValue();
                            afterBalance = beforeBalance + actionFee;
                            newAction = null;

                            if (waitingDeal.getIsSellerFull()) {
                                newAction = new Action(3, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            else { // partly buy
                                newAction = new Action(5, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            addAction(newAction, waitingDeal.getSellerName());
                        }

                        // add the same user list
                        for (Transaction selr : SameUserList ) {
                            stock.getSells().add(selr);
                        }

                        return dealsMade;
                    }
                    else if(transaction.getQuantity() < sell.getQuantity()) { // i buy less then you sell still a "full deal" for me
                        //pop to sell and add to made
                        Transaction deal = new Transaction(sell.getValue(), transaction.getQuantity(), 3, transaction.getTypeTrType(), transaction.getBuyerName(), sell.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), true, false);
                        dealsMade.add(made);
                        waitingDealsMade.add(deal);

                        // update the sell quantity
                        sell.setQuantity(sell.getQuantity() - transaction.getQuantity());

                        // Create all the deals that were supposed to be made
                        for (Transaction waitingDeal : waitingDealsMade) {
                            stock.getCompletedTransactions().add(waitingDeal);
                            stock.setValue(waitingDeal.getValue(), waitingDeal.getTimeStamp()); // after every deal update stock val
                            updateUsers(waitingDeal.getBuyerName(), waitingDeal.getSellerName(), waitingDeal.getQuantity(), stock.getSymbol(), stock.getValue());
                        }

                        // add the actions
                        for (TransactionDTO waitingDeal : dealsMade){
                            // Add full purchase action
                            int beforeBalance = users.get(waitingDeal.getBuyerName()).getMoney();
                            int actionFee = waitingDeal.getCycle();     //    waitingDeal.getQuantity()*stock.getValue();
                            int afterBalance = beforeBalance - actionFee;

                            Action newAction = null;
                            if (waitingDeal.getIsBuyerFull()) {
                                newAction = new Action(2, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            else { // partly buy
                                newAction = new Action(4, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            addAction(newAction, waitingDeal.getBuyerName());

                            // Add full sell action
                            beforeBalance = users.get(waitingDeal.getSellerName()).getMoney();
                            actionFee = waitingDeal.getCycle(); //waitingDeal.getQuantity()*stock.getValue();
                            afterBalance = beforeBalance + actionFee;
                            newAction = null;

                            if (waitingDeal.getIsSellerFull()) {
                                newAction = new Action(3, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            else { // partly buy
                                newAction = new Action(5, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            addAction(newAction, waitingDeal.getSellerName());
                        }

                        // add the same user list
                        for (Transaction selr : SameUserList ) {
                            stock.getSells().add(selr);
                        }

                        return dealsMade;
                    }
                    else { // i buy all you sell, but i want to buy more
                        Transaction deal = new Transaction(sell.getValue(), sell.getQuantity(), 3, transaction.getTypeTrType(), transaction.getBuyerName(), sell.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName() , false, true);
                        dealsMade.add(made);
                        waitingDealsMade.add(deal);
                        transaction.setQuantity(transaction.getQuantity()-sell.getQuantity());
                        waitingList.add(sell);
                        stock.getSells().remove(sell);
                    }
                }
            } // end of loop
            if(transaction.getQuantity() > 0) {
                // cancell all i did and reutrn
                for(Transaction redoTransactions : waitingList) {
                    stock.getSells().add(redoTransactions);
                }
                dealsMade.clear(); // nothing was made
            }

            //return the transactions from same user to list
            for(Transaction tr : SameUserList) {
                stock.getSells().add(tr);
            }
        } // type 1

        else { //  type 2 i want to sell
            int size = stock.getPurchases().size();
            for(int i = 0; i < size; ++i) { // i=0; i<buy.size
                Transaction purchase = stock.getPurchases().first(); //peek();

                if(transaction.getSellerName().equals(purchase.getBuyerName())) {
                    //skip next person
                    SameUserList.add(purchase);
                    stock.getPurchases().remove(purchase);
                }
                else if(purchase.getValue() < transaction.getValue() ) { // too low won't sell
                    // cancell all i did and reutrn
                    for(Transaction redoTransactions : waitingList) {
                        stock.getSells().add(redoTransactions);
                    }

                    // add the same user list
                    for (Transaction sameUserTransaction : SameUserList ) {
                        stock.getSells().add(sameUserTransaction);
                    }

                    dealsMade.clear(); // nothing was made
                    return dealsMade;
                }
                else { // the price is good for us
                    if(transaction.getQuantity() == purchase.getQuantity()) { // full deal
                        Transaction deal = new Transaction(purchase.getValue(), purchase.getQuantity(), 3, transaction.getTypeTrType(), purchase.getBuyerName(), transaction.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), true, true);
                        dealsMade.add(made);
                        waitingDealsMade.add(deal);
                        stock.getPurchases().remove(purchase);

                        // Create all the deals that were supposed to be made
                        for (Transaction waitingDeal : waitingDealsMade) {
                            stock.getCompletedTransactions().add(waitingDeal);
                            stock.setValue(waitingDeal.getValue(), waitingDeal.getTimeStamp()); // after every deal update stock val

                            updateUsers(waitingDeal.getBuyerName(), waitingDeal.getSellerName(), waitingDeal.getQuantity(), stock.getSymbol(), stock.getValue());
                            updateRealQuantity(transaction.getSellerName(),stock.getSymbol(), waitingDeal.getQuantity());
                        }

                        // add the actions
                        for (TransactionDTO waitingDeal : dealsMade){
                            // Add full purchase action
                            int beforeBalance = users.get(waitingDeal.getBuyerName()).getMoney();
                            int actionFee = waitingDeal.getCycle();     //    waitingDeal.getQuantity()*stock.getValue();
                            int afterBalance = beforeBalance - actionFee;

                            Action newAction = null;
                            if (waitingDeal.getIsBuyerFull()) {
                                newAction = new Action(2, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            else { // partly buy
                                newAction = new Action(4, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            addAction(newAction, waitingDeal.getBuyerName());

                            // Add full sell action
                            beforeBalance = users.get(waitingDeal.getSellerName()).getMoney();
                            actionFee = waitingDeal.getCycle(); //waitingDeal.getQuantity()*stock.getValue();
                            afterBalance = beforeBalance + actionFee;
                            newAction = null;

                            if (waitingDeal.getIsSellerFull()) {
                                newAction = new Action(3, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            else { // partly buy
                                newAction = new Action(5, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            addAction(newAction, waitingDeal.getSellerName());
                        }

                        // add the same user list
                        for (Transaction selr : SameUserList ) {
                            stock.getSells().add(selr);
                        }

                        return dealsMade;
                    }
                    else if (transaction.getQuantity() < purchase.getQuantity()) { // i sell less then you want to buy
                        Transaction deal = new Transaction(purchase.getValue(), transaction.getQuantity(), 3, transaction.getTypeTrType(), purchase.getBuyerName(), transaction.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), false, true);
                        dealsMade.add(made);
                        waitingDealsMade.add(deal);
                        // update the buy quantity
                        purchase.setQuantity(purchase.getQuantity() - transaction.getQuantity());


                        // Create all the deals that were supposed to be made
                        for (Transaction waitingDeal : waitingDealsMade) {
                            stock.getCompletedTransactions().add(waitingDeal);
                            stock.setValue(waitingDeal.getValue(), waitingDeal.getTimeStamp()); // after every deal update stock val

                            updateUsers(waitingDeal.getBuyerName(), waitingDeal.getSellerName(), waitingDeal.getQuantity(), stock.getSymbol(), stock.getValue());
                            updateRealQuantity(transaction.getSellerName(),stock.getSymbol(), waitingDeal.getQuantity());
                        }

                        // add the actions
                        for (TransactionDTO waitingDeal : dealsMade){
                            // Add full purchase action
                            int beforeBalance = users.get(waitingDeal.getBuyerName()).getMoney();
                            int actionFee = waitingDeal.getCycle();     //    waitingDeal.getQuantity()*stock.getValue();
                            int afterBalance = beforeBalance - actionFee;

                            Action newAction = null;
                            if (waitingDeal.getIsBuyerFull()) {
                                newAction = new Action(2, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            else { // partly buy
                                newAction = new Action(4, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            addAction(newAction, waitingDeal.getBuyerName());

                            // Add full sell action
                            beforeBalance = users.get(waitingDeal.getSellerName()).getMoney();
                            actionFee = waitingDeal.getCycle(); //waitingDeal.getQuantity()*stock.getValue();
                            afterBalance = beforeBalance + actionFee;
                            newAction = null;

                            if (waitingDeal.getIsSellerFull()) {
                                newAction = new Action(3, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            else { // partly buy
                                newAction = new Action(5, stock.getSymbol(), waitingDeal.getTimeStamp(), actionFee, beforeBalance, afterBalance);
                            }
                            addAction(newAction, waitingDeal.getSellerName());
                        }
                        // add the same user list
                        for (Transaction selr : SameUserList ) {
                            stock.getSells().add(selr);
                        }

                        return dealsMade;
                    }
                    else { // i sell all you want to buy, but i want to sell more
                        Transaction deal = new Transaction(purchase.getValue(), purchase.getQuantity(), 3, transaction.getTypeTrType(), purchase.getBuyerName(), transaction.getSellerName()); // change 3
                        TransactionDTO made = new TransactionDTO(deal.getValue(), deal.getQuantity(), deal.getCycle() ,deal.getTimeStamp(), deal.getTypeTrType(), deal.getBuyerName(), deal.getSellerName(), true, false);
                        dealsMade.add(made);
                        waitingDealsMade.add(deal);
                        transaction.setQuantity(transaction.getQuantity()-purchase.getQuantity());
                        waitingList.add(purchase);
                        stock.getPurchases().remove(purchase);
                    }
                }
            } // end of loop
            if(transaction.getQuantity() > 0) {
                // cancell all i did and reutrn
                for(Transaction redoTransactions : waitingList) {
                    stock.getPurchases().add(redoTransactions);
                }
                dealsMade.clear(); // nothing was made
            }
            //return the moves transactions fro, same user
            for(Transaction tr : SameUserList) {
                stock.getPurchases().add(tr);
            }
        } // end type 2

        return dealsMade;
    }


    private synchronized void updateRealQuantity(String sellerName, String symbol, int quantity) {
        //users.get(transaction.getSellerName()).getItems().get(stock.getSymbol()).setQuantity();
        for (String userName : userNames) {
            if (userName.equals(sellerName)) {
                for (Item item : users.get(sellerName).getItems()) {
                    if (item.getSymbol().equals(symbol)) {
                        item.setRealQuantity(item.getRealQuantity() - quantity);
                        // if (item.getRalQuantity() == 0) maybe do something ?
                        break;
                    }
                }
            }
        }
    }

    private synchronized void updateUsers(String buyerName, String sellerName, int quantity, String symbol, int value) {
        boolean buyerHasStock = false;

        for (String userName : userNames) {
            if (userName.equals(buyerName)) {
                users.get(buyerName).addMoney((-1) * quantity * value);
                users.get(buyerName).addInvestmentMoney(quantity*value);
                for (Item item : users.get(buyerName).getItems()) {
                    if (item.getSymbol().equals(symbol)) {
                        buyerHasStock = true;
                        item.setQuantity(item.getQuantity() + quantity);
                        item.setRealQuantity(item.getRealQuantity() + quantity);
                        item.setValue(value);
                        if (item.getQuantity() == 0) {
                            users.get(buyerName).getItems().remove(item);
                        }
                        break;
                    }
                }

                if (!buyerHasStock) {
                    Item newStock = new Item(symbol, quantity, value);
                    users.get(buyerName).getItems().add(newStock);
                }
            }

            else if (userName.equals(sellerName)) {
                users.get(sellerName).addMoney(quantity*value);
                users.get(sellerName).addInvestmentMoney((-1)*quantity*value);
                for (Item item : users.get(sellerName).getItems()) {
                    if (item.getSymbol().equals(symbol)) {
                        item.setQuantity(item.getQuantity() - quantity); // didnt add real quantity here cuz it already happened\
                        item.setValue(value);
                        if (item.getQuantity() == 0) {
                            users.get(sellerName).getItems().remove(item);
                        }
                        break;
                    }
                }
            }
            // Update all other users with stock new value
            else {
                for (Item item : users.get(userName).getItems()) {
                    if (item.getSymbol().equals(symbol)) {
                        item.setValue(value);
                        break;
                    }
                }
            }
        }


    }

    @Override
    public synchronized UserDTO getUser(String name) {
        if (!userNames.contains(name)) {
            return null; // handle problem
        }
        return new UserDTO(name, users.get(name).getItems(), users.get(name).getType(), users.get(name).getMoney(), users.get(name).getInvestmentMoney(), users.get(name).getUserActions());
    }

    @Override
    public synchronized List<String> getUsersNames() {
        return userNames;
    }

    @Override
    public synchronized List<String> getUsersTypes() {
        List<String> usersTypes = new ArrayList<>();
        for (String name : userNames) {
            usersTypes.add(users.get(name).getType());
        }
        return usersTypes;
    }

    public String findStockByName(String SYMBOL){
        for (String str: stocksNames) {
            if (SYMBOL.compareToIgnoreCase(str) == 0) return str;
        }
        return null;
    }

    @Override
    public synchronized List<StockDTO> getAllStocks() { // the transactions array are null
        List<StockDTO> allStocks = new ArrayList<>();
        for (String name : stocksNames) {
            Stock stock = stocks.get(name);
            StockDTO stockDto = new StockDTO(stock.getSymbol(), stock.getCompany(), stock.getValue(), stock.getTotalCycle(), stock.getCompletedTransactions().size());
            allStocks.add(stockDto);
        }
        return allStocks; // the transactions array are null
    }

    @Override
    public synchronized List<UserDTO> getAllUsers() { // the transactions array are null
        List<UserDTO> allUsers = new ArrayList<>();
        for (String name : userNames) {
            User user = users.get(name);
            UserDTO userDto = new UserDTO(user.getName(), user.getItems(), user.getType(), user.getMoney(), user.getInvestmentMoney(), user.getUserActions());
            allUsers.add(userDto);
        }
        return allUsers; // the transactions array are null
    }

    @Override
    public synchronized void addMoney(int val, String username) throws Exception {
        if (!userNames.contains(username)) {
            throw new Exception("there is no user named: " + username + " in the system" );
        }

        int beforeBalance = users.get(username).getMoney();
        users.get(username).addMoney(val);
        int afterBalance = users.get(username).getMoney();

        String timestamp = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
        Action newAction = new Action(1, null, timestamp, val, beforeBalance, afterBalance);
        users.get(username).addActions(newAction);
    }

    @Override
    public synchronized int getTotalMoneyPerUser(String username) throws Exception {
        if (!userNames.contains(username)) {
            throw new Exception("there is no user named: " + username + " in the system" );
        }
        return users.get(username).getMoney();
    }

    @Override
    public synchronized void addStock(String company, String symbol, int quantity, int value, String username) throws Exception {
        String symbolExists = isCompanyExists(company);
        if (company == null || symbol == null || username == null){
            throw new NullPointerException();
        }
        if (value < quantity) {
            throw new Exception("ERROR! Company value can't be smaller than the quantity!");
        }
        else if (symbolExists != null) {
            throw new Exception("ERROR! You've tried to insert the stock: " + symbol + " by " + company + ", but a similar stock already exists with the same company name: "
                    + symbolExists + " by " + company + ". Please rename the company name of one of the stocks.");

        }
        else if (stocks.containsKey(symbol)) {
            throw new Exception("ERROR! You've tried to insert the stock: " + symbol + " by " + company + ", but a similar stock already exists with the same symbol name: "
                    + symbol + " by " + stocks.get(symbol).getCompany() + ". Please rename the symbol name of one of the stocks.");
        }
        else {
            Stock newStock = new Stock(symbol, company, (value / quantity));
            stocks.put(symbol, newStock);
            stocksNames.add(symbol);
            List<Item> newItems = new ArrayList<>();
            newItems.add(new Item(symbol, quantity, (value / quantity)));
            users.get(username).addItems(newItems);
            users.get(username).addInvestmentMoney(value*quantity);
        }
    }

    @Override
    public synchronized void addAction(Action newAction, String username){
        users.get(username).addActions(newAction);
    }

    @Override
    public synchronized void addAlert(String username, String alert) {
        // If user already has a list
        if (usersAlerts.containsKey(username)){
            usersAlerts.get(username).add(alert);
        }
        // else make new one but check if user is in the user list
        else if (userNames.contains(username)) {
            usersAlerts.put(username, new ArrayList<>());
            usersAlerts.get(username).add(alert);
        }
    }

    @Override
    public synchronized List<String> getUserAlerts(String username) {
        List<String> alerts = usersAlerts.get(username);
        usersAlerts.remove(username);
        return alerts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EngineImpl engine = (EngineImpl) o;
        return Objects.equals(stocks, engine.stocks) && Objects.equals(stocksNames, engine.stocksNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stocks, stocksNames);
    }

    @Override
    public String toString() {
        return "Engine{" +
                "stocks=" + stocks +
                ", names=" + stocksNames +
                '}';
    }
}
