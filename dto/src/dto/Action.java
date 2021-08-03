package dto;

public class Action {
    private final String description;
    private final String timestamp;
    private final int actionFee;
    private final int beforeBalance;
    private final int afterBalance;

    public Action(int newType, String newSymbol, String newTimeStamp, int newActionFee, int newBeforeBalance, int newAfterBalance) {
        // type: 1 - transfer money, 2 - full purchase, 3 - full sell, 4 - partial purchase, 5 - partial sell, 6 - new Company, 7 - Load XML
        timestamp = newTimeStamp;
        if (newType == 2 ) actionFee = (-1) * newActionFee;
        else actionFee = newActionFee;
        beforeBalance = newBeforeBalance;
        afterBalance = newAfterBalance;
        description = makeDescription(newType, newSymbol);
    }

    private String makeDescription(int type, String symbol) {
        String description;
        if (type == 1) {
            description = "Transferred Money to account";
        }
        else if (type == 2) {
            description = "Purchased " + symbol + " stocks";
        }
        else if (type == 3){
            description = "Sold " + symbol + " stocks";
        }
        else if (type == 4) {
            description = "Partially purchased " + symbol + " stocks";
        }
        else if (type == 5) {
            description = "Partially sold " + symbol + " stocks";
        }
        else if (type == 6) {
            description = "Created " + symbol + ", a new stock and add it to account";
        }
        else {
            description = "Loaded a XML file to account";
        }
        return description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public int getAmount() {
        return actionFee;
    }

    public int getBeforeBalance() {
        return beforeBalance;
    }

    public int getAfterBalance() {
        return afterBalance;
    }
}
