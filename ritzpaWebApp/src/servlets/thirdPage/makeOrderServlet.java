package servlets.thirdPage;

import dto.Action;
import dto.TransactionDTO;
import engine.Engine;
import utils.ServletUtils;
import utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class makeOrderServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            response.setContentType("text/html");
            String usernameFromSession = SessionUtils.getUsername(request);
            String directionMsg, pendingDirectionMsg, partialDirectionMsg;
            Engine ritzpa = ServletUtils.getEngine(getServletContext());
            if (usernameFromSession != null) {
                int direction, type, amount, price;
                // Determine direction: Buy = 1, sell = 2
                if (request.getParameter("transactionDirection").equals("BUY")) {
                    direction = 1;
                    pendingDirectionMsg = ", you requested to purchase ";
                } else {
                    direction = 2;
                    pendingDirectionMsg = ", you requested to sell ";
                }

                // determine type: LMT = 1, MKT = 2, FOK = 3, IOC = 4
                switch (request.getParameter("transactionType")) {
                    case "LMT":
                        type = 1;
                        break;
                    case "MKT":
                        type = 2;
                        break;
                    case "FOK":
                        type = 3;
                        break;
                    case "IOC":
                        type = 4;
                        break;
                    default:
                        type = 0;
                        break;
                }

                // determine amount
                amount = Integer.parseInt(request.getParameter("amount"));

                // determine price
                price = Integer.parseInt(request.getParameter("price"));
                String stock = request.getParameter("stock");


                // make order
                String timeStamp = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
                List<TransactionDTO> made = ritzpa.order(type, direction, stock, amount, price, usernameFromSession);
                String message = "Hey " + usernameFromSession + "!" + " Successfully committed your " + request.getParameter("transactionType") + " trade request: " + "\n" +
                        "At " + timeStamp + pendingDirectionMsg + amount + " stocks of " + stock + " at a price of " + price + "$!";
                // Create alerts
                // Only alert if deal made
                if (made.size() != 0) {
                    int quantity = 0;
                    for (TransactionDTO tr : made) {
                        quantity += tr.getQuantity();
                    }
                    String resultMessage;

                    if (quantity == amount) { // full deal
                        resultMessage = "Hey " + usernameFromSession + "!" + " you made a full deal, here are the details about the transactions you made: " + "\n";
                    }
                    else { // partial deal
                        resultMessage = "Hey " + usernameFromSession + "!" + " you made a partial deal, here are the details about the transactions you made: " + "\n";
                    }

                    for (TransactionDTO tr : made) {
                        if (direction == 1) {    // i want to buy
                            resultMessage = resultMessage.concat("At " + tr.getTimeStamp() + ", you bought " + tr.getQuantity() + " stocks of " + stock + " at a price of " + tr.getValue() + "$!" + "\n" ); //System.lineSeparator()

                            if(tr.getIsSellerFull()) {
                                ritzpa.addAlert(tr.getSellerName(), "At " + tr.getTimeStamp() + ", you fully sold " + tr.getQuantity() + " stocks of " + stock + " at a price of " + tr.getValue() + "$!" + "\n");
                            }
                            else {
                                ritzpa.addAlert(tr.getSellerName(), "At " + tr.getTimeStamp() + ", You partially sold " + tr.getQuantity() + " stocks of " + stock + " at a price of " + tr.getValue() + "$!" + "\n");
                            }
                        }
                        else { // i want to sell
                            resultMessage = resultMessage.concat("At " + tr.getTimeStamp() + ", you sold " + tr.getQuantity() + " stocks of " + stock + " at a price of " + tr.getValue() + "$\n");
                            if(tr.getIsBuyerFull()) {
                            ritzpa.addAlert(tr.getBuyerName(), "At " + tr.getTimeStamp() + ", you fully bought " + tr.getQuantity() + " stocks of " + stock + " at a price of " + tr.getValue() + "$!" + "\n");
                            }
                            else {
                                ritzpa.addAlert(tr.getBuyerName(), "At " + tr.getTimeStamp() + ", you partially bought " + tr.getQuantity() + " stocks of " + stock + " at a price of " + tr.getValue() + "$!" + "\n");
                            }
                        }
                    }

                    ritzpa.addAlert(usernameFromSession, resultMessage );
                }
                else { // no deal has beed made]
                    String noDeal = "Hey " + usernameFromSession + "!" + " No deals were made! " + "\n";
                    if (type == 3 || type == 4) {
                        if (direction == 1) noDeal = noDeal.concat("Your purchase was deleted from the system!");
                        else noDeal = noDeal.concat("Your sell was deleted from the system!");
                    }
                    else {
                        if (direction == 1) noDeal = noDeal.concat("Your purchase is waiting in the awaiting purchases");
                        else noDeal = noDeal.concat("Your sell is waiting in the awaiting sells!");
                    }
                    ritzpa.addAlert(usernameFromSession, noDeal );

                }
            response.getOutputStream().println(message);
            }
        }
        catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().println("Failed to submit trade request!");
        }
    }
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
