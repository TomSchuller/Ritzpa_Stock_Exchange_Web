package servlets.firstPage;
import constants.Constants;
import static constants.Constants.USERNAME;

import engine.Engine;
import utils.ServletUtils;
import utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FeedServlet extends HttpServlet { // login

    private final String BROKER_FEED_URL = "pages/brokerFeedPage/brokerFeedPage.html";
    private final String ADMIN_FEED_URL = "pages/adminFeedPage/adminFeedPage.html";
    private final String LOGIN_ERROR_URL = "/pages/loginerror/login_attempt_after_error.jsp";  // must start with '/' since will be used in request dispatcher...
    private final String SIGN_UP_URL = "index.html";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8"); // tell the resp what i will return

        String usernameFromSession = SessionUtils.getUsername(req);
        Engine ritzpa = ServletUtils.getEngine(getServletContext());
        if (usernameFromSession == null) {
            // user is not logged in yet
            String usernameFromParameter = req.getParameter(USERNAME);
            String userTypeParameter = req.getParameter(Constants.USER_TYPE);

            if (usernameFromParameter == null || usernameFromParameter.isEmpty()) {
                req.setAttribute(Constants.USER_NAME_ERROR, "ERROR! You didn't add a name. Please write a name before submitting!");
                getServletContext().getRequestDispatcher(LOGIN_ERROR_URL).forward(req, resp);
            }
            else if (userTypeParameter == null || userTypeParameter.isEmpty()) {
                //no username in session and no username in parameter -
                //redirect back to the index page
                //this return an HTTP code back to the browser telling it to load

                //change it will send error
//                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                resp.getOutputStream().println("ERROR! You didn't complete filling the form! Please try again!");

               // resp.sendRedirect(SIGN_UP_URL);

                req.setAttribute(Constants.USER_NAME_ERROR, "ERROR! You didn't choose a type! Please choose a type before submitting!");
                getServletContext().getRequestDispatcher(LOGIN_ERROR_URL).forward(req, resp);
            } else {
                usernameFromParameter = usernameFromParameter.trim(); //normalize the username value

                /*
                One can ask why not enclose all the synchronizations inside the userManager object ?
                Well, the atomic action we need to perform here includes both the question (isUserExists) and (potentially) the insertion
                of a new user (addUser). These two actions needs to be considered atomic, and synchronizing only each one of them, solely, is not enough.
                (of course there are other more sophisticated and performable means for that (atomic objects etc) but these are not in our scope)

                The synchronized is on this instance (the servlet).
                As the servlet is singleton - it is promised that all threads will be synchronized on the very same instance (crucial here)

                A better code would be to perform only as little and as necessary things we need here inside the synchronized block and avoid
                do here other not related actions (such as request dispatcher\redirection etc. this is shown here in that manner just to stress this issue
                 */
                synchronized (this) {
                    if (ritzpa.isUserExists(usernameFromParameter)) {
                        String errorMessage = "Username " + usernameFromParameter + " already exists. Please enter a different username!";
                        // username already exists, forward the request back to index.jsp
                        // with a parameter that indicates that an error should be displayed
                        // the request dispatcher obtained from the servlet context is one that MUST get an absolute path (starting with'/')
                        // and is relative to the web app root
                        // see this link for more details:
                        // http://timjansen.github.io/jarfiller/guide/servlet25/requestdispatcher.xhtml
                        req.setAttribute(Constants.USER_NAME_ERROR, errorMessage);
                        getServletContext().getRequestDispatcher(LOGIN_ERROR_URL).forward(req, resp);
                    }
                    else {
                        //add the new user to the users list
                        ritzpa.addUser(usernameFromParameter, userTypeParameter);
                        //set the username in a session so it will be available on each request
                        //the true parameter means that if a session object does not exists yet
                        //create a new one
                        req.getSession(true).setAttribute(Constants.USERNAME, usernameFromParameter);

                        //redirect the request to the feed room - in order to actually change the URL
                        System.out.println("On login, request URI is: " + req.getRequestURI());

                        if (userTypeParameter.equals("admin")) {
                            resp.sendRedirect(ADMIN_FEED_URL);
                        }
                        else {
                            resp.sendRedirect(BROKER_FEED_URL);
                        }
                    }
                }
            }
        } else {
            //user is already logged in send it by its type
            if ( ritzpa.getUser(usernameFromSession).getType().equals("admin")) {
                resp.sendRedirect(ADMIN_FEED_URL);
            }
            else {
                resp.sendRedirect(BROKER_FEED_URL);
            }
        }
    }




    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}