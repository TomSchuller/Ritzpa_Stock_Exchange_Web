package dto;

import java.util.List;

public class UserDTO {
    private final String name;
    private  final List<Item> items;
    private final String type;
    private final int money;
    private final int investmentMoney;
    private final List<Action> userActions;

    public UserDTO(String newName, List<Item> newItems,String newType, int newMoney, int newInvestmentMoney, List<Action> newUserActions) {
        name = newName;
        items = newItems;
        type= newType;
        money = newMoney;
        investmentMoney = newInvestmentMoney;
        userActions = newUserActions;
    }

    public String getName() { return name; }
    public List<Item> getItems() {
        return items;
    }
    public String getType() { return type; }
    public int getMoney() { return money; }
    public List<Action> getUserActions() { return userActions; }
    public int getInvestmentMoney() { return investmentMoney; }
    public Item findItem(String symbol) {
        for (Item item : items) {
            if (item.getSymbol().equals(symbol)) return item;
        }
        return null;
    }
}
