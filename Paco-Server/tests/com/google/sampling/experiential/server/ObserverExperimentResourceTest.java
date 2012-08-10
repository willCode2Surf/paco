// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.sampling.experiential.server;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;

import com.google.sampling.experiential.shared.Experiment;

public class ObserverExperimentResourceTest extends PacoResourceTest {
  /*
   * show tests
   */
  @Test
  public void testShow() {
    Request request = ServerTestHelper.createJsonGetRequest("/observer/experiments/1");
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testShowAfterCreate() {
    ExperimentTestHelper.createUnpublishedExperiment();
    Request request = ServerTestHelper.createJsonGetRequest("/observer/experiments/1");
    Response response = new PacoApplication().handle(request);

    Experiment experiment = ExperimentTestHelper.constructExperiment();
    experiment.setId(1l);
    experiment.setVersion(1);
    experiment.addObserver("subject@google.com");

    assertEquals(Status.SUCCESS_OK, response.getStatus());
    assertEquals(
        DAOHelper.toJson(experiment, Experiment.Observer.class), response.getEntityAsText());
  }

  @Test
  public void testShowAsImposterAfterCreate() {
    ExperimentTestHelper.createUnpublishedExperiment();

    helper.setEnvEmail("impostor@google.com");

    Request request = ServerTestHelper.createJsonGetRequest("/observer/experiments/1");
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.CLIENT_ERROR_FORBIDDEN, response.getStatus());
  }

  /*
   * update tests
   */
  @Test
  public void testUpdate() {
    Experiment experiment = ExperimentTestHelper.constructExperiment();
    experiment.setPublished(false);
    experiment.setViewers(null);
    experiment.setSignalSchedule(null);

    Request request = ServerTestHelper.createJsonPostRequest(
        "/observer/experiments/1", DAOHelper.toJson(experiment));
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateWithVersionEqualAfterCreate() {
    ExperimentTestHelper.createPublishedPublicExperiment();

    Experiment experiment = ExperimentTestHelper.constructExperiment();
    experiment.setVersion(1);

    Request request = ServerTestHelper.createJsonPostRequest(
        "/observer/experiments/1", DAOHelper.toJson(experiment));
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.SUCCESS_NO_CONTENT, response.getStatus());
  }

  @Test
  public void testUpdateWithVersionGreaterAfterCreate() {
    ExperimentTestHelper.createPublishedPublicExperiment();

    Experiment experiment = ExperimentTestHelper.constructExperiment();
    experiment.setVersion(2);

    Request request = ServerTestHelper.createJsonPostRequest(
        "/observer/experiments/1", DAOHelper.toJson(experiment));
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.CLIENT_ERROR_CONFLICT, response.getStatus());
  }

  @Test
  public void testUpdateWithVersionLessAfterCreate() {
    ExperimentTestHelper.createPublishedPublicExperiment();

    Experiment experiment = ExperimentTestHelper.constructExperiment();
    experiment.setVersion(0);

    Request request = ServerTestHelper.createJsonPostRequest(
        "/observer/experiments/1", DAOHelper.toJson(experiment));
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.CLIENT_ERROR_CONFLICT, response.getStatus());
  }

  @Test
  public void testUpdateAsImpostorAfterCreate() {
    ExperimentTestHelper.createPublishedPublicExperiment();

    helper.setEnvEmail("imposter@google.com");

    Experiment experiment = ExperimentTestHelper.constructExperiment();
    experiment.setVersion(1);

    Request request = ServerTestHelper.createJsonPostRequest(
        "/observer/experiments/1", DAOHelper.toJson(experiment));
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.CLIENT_ERROR_FORBIDDEN, response.getStatus());
  }

  /*
   * destroy tests
   */
  @Test
  public void testDestroy() {
    Request request = ServerTestHelper.createJsonDeleteRequest("/observer/experiments/1");
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testDestroyAfterCreate() {
    ExperimentTestHelper.createPublishedPublicExperiment();

    Request request = ServerTestHelper.createJsonDeleteRequest("/observer/experiments/1");
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.SUCCESS_NO_CONTENT, response.getStatus());
  }

  @Test
  public void testDestroyAsImpostorAfterCreate() {
    ExperimentTestHelper.createPublishedPublicExperiment();

    helper.setEnvEmail("imposter@google.com");

    Request request = ServerTestHelper.createJsonDeleteRequest("/observer/experiments/1");
    Response response = new PacoApplication().handle(request);

    assertEquals(Status.CLIENT_ERROR_FORBIDDEN, response.getStatus());
  }
}
