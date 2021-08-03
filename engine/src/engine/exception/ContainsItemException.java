package engine.exception;

import dto.Item;


public class ContainsItemException extends Exception {
    private final int quantityTry;
    private final int quantityExists;
    private final String symbol;
    private final String EXCEPTION_MESSAGE;

    public ContainsItemException(Item itemTry, Item itemExists, String userName) {
        quantityTry = itemTry.getQuantity();
        quantityExists = itemExists.getQuantity();
        symbol = itemTry.getSymbol();
        EXCEPTION_MESSAGE = "ERROR! You've tried to insert " + quantityTry + " stocks of " + symbol +
                " to " + userName + ", but in our system you've already inserted " + quantityExists + " stocks to " + userName +
               ". Please rename the symbol of the stocks.";
    }

    @Override
    public String getMessage() {
        return EXCEPTION_MESSAGE;
    }
}

