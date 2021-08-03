package servlets.alerts;

import com.google.gson.Gson;
import constants.Constants;
import engine.Engine;
import engine.SingleChatEntry;
import servlets.chat.ChatServlet;
import utils.ServletUtils;
import utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TransactionAlerts extends HttpServlet {

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Engine ritzpa = ServletUtils.getEngine(getServletContext());
        String username = SessionUtils.getUsername(request);
        if (username != null) {
            List<String> alerts = ritzpa.getUserAlerts(username);
            String bigAlert = "";
            if (alerts != null) {
                int j = 1;
                for (String alert : alerts) {
                    bigAlert = bigAlert.concat(j + ". " + alert + '\n');
                    ++j;
                }
            response.setContentType("text/html");
            response.getOutputStream().println(bigAlert);
            }
        }

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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
    }// </editor-fold>
}
