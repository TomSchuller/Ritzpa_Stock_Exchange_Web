package servlets.secondPage;

import dto.Action;
import engine.Engine;
import engine.exception.ContainsCompanyException;
import utils.ServletUtils;
import utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateCompanyServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String usernameFromSession = null;
        Engine ritzpa = null;
        String company = null, symbol = null;
        int quantity = 0, value = 0;
        try {
            usernameFromSession = SessionUtils.getUsername(request);
            ritzpa = ServletUtils.getEngine(getServletContext());
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().println(ex.getMessage());
            return;
        }
        try {
            company = request.getParameter("stockCompany");
            symbol = request.getParameter("stockSymbol");
            if(company.equals("")) {
                throw new NullPointerException("ERROR! You didn't enter a company name therefore you didn't complete filling the form! Please try again!");
            }
            if(symbol.equals("")) {
                throw new NullPointerException("ERROR! You didn't enter a stock symbol therefore you didn't complete filling the form! Please try again!");
            }
        }
        catch (NullPointerException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().println(ex.getMessage());
            return;

        }

        try {
            quantity = Integer.parseInt(request.getParameter("stockQuantity"));
        }
        catch (NumberFormatException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().println("ERROR! You didn't enter a quantity therefore you didn't complete filling the form! Please try again!");
            return;

        }

        try {
            value = Integer.parseInt(request.getParameter("stockValue"));
            response.setContentType("text/html");
            ritzpa.addStock(company, symbol, quantity, value, usernameFromSession);
            String timeStamp = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
            int beforeBalance = ritzpa.getUser(usernameFromSession).getMoney();

            Action newAction = new Action(6, symbol, timeStamp, 0, beforeBalance, beforeBalance);
            ritzpa.addAction(newAction, usernameFromSession);
            response.getOutputStream().println("Successfully added " + symbol + "!");
            return;

        }
        catch (NumberFormatException ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().println("ERROR! You didn't enter a company value therefore you didn't complete filling the form! Please try again!");
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().println(ex.getMessage());
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
