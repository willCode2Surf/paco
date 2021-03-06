/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance  with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.google.sampling.experiential.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTimeZone;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * Servlet that answers requests for experiments.
 *
 * Only used by the Android client to get the current list of experiments.
 *
 * @author Bob Evans
 *
 */
@SuppressWarnings("serial")
public class ExperimentServlet extends HttpServlet {

  public static final Logger log = Logger.getLogger(ExperimentServlet.class.getName());
  public static final String DEV_HOST = "<Your machine name here>";
  private UserService userService;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
  IOException {
    resp.setContentType("application/json;charset=UTF-8");

    User user = getWhoFromLogin();

    if (user == null) {
      redirectUserToLogin(req, resp);
    } else {
      DateTimeZone timezone = TimeUtil.getTimeZoneForClient(req);

      logPacoClientVersion(req);

      String email = getEmailOfUser(req, user);

      String shortParam = req.getParameter("short");
      String experimentsPublishedToMeParam = req.getParameter("mine");
      String selectedExperimentsParam = req.getParameter("id");

      String experimentsJson = null;
      ExperimentServletHandler handler;
      if (experimentsPublishedToMeParam != null) {
        handler = new ExperimentServletExperimentsForMeLoadHandler(email, timezone);
      } else if (shortParam != null) {
        handler = new ExperimentServletShortLoadHandler(email, timezone);
      } else if (selectedExperimentsParam != null) {
        handler = new ExperimentServletSelectedExperimentsFullLoadHandler(email, timezone, selectedExperimentsParam);
      } else {
        handler = new ExperimentServletAllExperimentsFullLoadHandler(user.getUserId(), email, timezone);
      }
      experimentsJson = handler.performLoad();
      resp.getWriter().println(scriptBust(experimentsJson));
    }
  }



  private void redirectUserToLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
  }

  private void logPacoClientVersion(HttpServletRequest req) {
    String pacoVersion = req.getHeader("paco.version");
    if (pacoVersion != null) {
      log.info("Paco version of request = " + pacoVersion);
    }
  }

  private String scriptBust(String experimentsJson) {
    // TODO add scriptbusting prefix to this and client code.
    return experimentsJson;
  }

  private String getEmailOfUser(HttpServletRequest req, User user) {
    String email = user != null ? user.getEmail() : null;
    if (email == null && isDevInstance(req)) {
      email = "<put your email here to test in developer mode>";
    }
    if (email == null) {
      throw new IllegalArgumentException("You must login!");
    }
    return email.toLowerCase();
  }

  private User getWhoFromLogin() {
    UserService userService = UserServiceFactory.getUserService();
    return userService.getCurrentUser();
  }

  public static boolean isDevInstance(HttpServletRequest req) {
    try {
      return DEV_HOST.equals(InetAddress.getLocalHost().toString());
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (userService.isUserAdmin()) {
      readExperimentDefinitions(req, resp);
    }
  }

  private void readExperimentDefinitions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String postBodyString;
    try {
      postBodyString = org.apache.commons.io.IOUtils.toString(req.getInputStream(), "UTF-8");
    } catch (IOException e) {
      log.info("IO Exception reading post data stream: " + e.getMessage());
      throw e;
    }
    if (postBodyString.equals("")) {
      throw new IllegalArgumentException("Empty Post body");
    }

    String appIdHeader = req.getHeader("http.useragent");
    String pacoVersion = req.getHeader("paco.version");
    log.info("Paco version = " + pacoVersion);
    String results = EventJsonUploadProcessor.create().processJsonEvents(postBodyString, getWhoFromLogin().getEmail().toLowerCase(), appIdHeader, pacoVersion);
    resp.setContentType("application/json;charset=UTF-8");
    resp.getWriter().write(results);
  }
}
