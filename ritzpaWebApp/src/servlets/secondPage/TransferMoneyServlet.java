package servlets.secondPage;

import engine.Engine;
import utils.ServletUtils;
import utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Collection;

public class TransferMoneyServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");

        /*
        // we could extract the 3rd member (not the file one) also as 'part' using the same 'key'
        // we used to upload it on the formData object in JS....
        Part name = request.getPart("name");
        String nameValue = readFromInputStream(name.getInputStream());
         */
        String usernameFromSession = SessionUtils.getUsername(request);
        Engine ritzpa = ServletUtils.getEngine(getServletContext());

        int val = Integer.parseInt(request.getParameter("transferMoney"));
        try {
            ritzpa.addMoney(val, usernameFromSession);
        }
        catch(Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().println("Failed to submit trade request!");
        }
    }

}
