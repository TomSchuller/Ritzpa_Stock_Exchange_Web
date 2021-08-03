package engine.exception;

import engine.Stock;

public class ContainsCompanyException extends Exception {
    private final String symbolTry;
    private final String symbolExists;
    private final String company;

    private final String EXCEPTION_MESSAGE;

    public ContainsCompanyException(Stock stockTry, Stock stockExists) {
        symbolTry = stockTry.getSymbol();
        symbolExists = stockExists.getSymbol();
        company = stockTry.getCompany();
        EXCEPTION_MESSAGE = "ERROR! You've tried to insert the stock: " + symbolTry + " by " + company + ", but a similar stock already exists with the same company name: "
                + symbolExists + " by " + company + ". Please rename the company name of the stock.";
    }

    @Override
    public String getMessage() {
        return EXCEPTION_MESSAGE;
    }
}