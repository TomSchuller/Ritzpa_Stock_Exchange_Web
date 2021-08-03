package servlets.secondPage;

import engine.Engine;
import utils.ServletUtils;
import utils.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Scanner;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class FileUploadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response); // response.sendRedirect("fileupload/form.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        response.setContentType("text/html");
        Collection<Part> parts = request.getParts();

        /*
        // we could extract the 3rd member (not the file one) also as 'part' using the same 'key'
        // we used to upload it on the formData object in JS....
        Part name = request.getPart("name");
        String nameValue = readFromInputStream(name.getInputStream());
         */
        String usernameFromSession = SessionUtils.getUsername(request);
        Engine ritzpa = ServletUtils.getEngine(getServletContext());

        try {
            for (Part part : parts) {
                if (!part.getContentType().equals("text/xml")){
                    throw new Exception("ERROR! The file you entered is not a XML file! please choose a XML file!" + System.lineSeparator());
                }
                ritzpa.load(part.getInputStream(), usernameFromSession);
                response.getOutputStream().println("Successfully added the file: '" + part.getSubmittedFileName() + "' to the system!");
                break;
            }
        } catch (NullPointerException ex) {
        // you didnt enterd a file name
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().println("ERROR! You didn't add a file to upload, please choose a file first!");

        }
        catch (Exception ex) {
           // response.sendError(HttpServletResponse.SC_NOT_FOUND);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.getOutputStream().println(ex.getMessage());

            //  throw ex; //e.printStackTrace();
        }
    }

//    private String readFromInputStream(InputStream inputStream) {
//        return new Scanner(inputStream).useDelimiter("\\Z").next();
//    }
}