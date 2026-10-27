/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.sileliz.api;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "ApiSaludServlet", urlPatterns = {"/api/salud"})
public class ApiSaludServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        
        JsonObject json = new JsonObject();
        json.addProperty("estado", "OK");
        json.addProperty("proyecto", "Sileliz");
        json.addProperty("version", "v3.1.2");

        try (PrintWriter out = response.getWriter()) {
            out.print(json.toString());
        }
    }
}