// Copyright (c) Yugabyte, Inc.

package com.yugabyte.yw.models;

import static com.yugabyte.yw.common.audit.AuditService.SECRET_REPLACEMENT;
import static com.yugabyte.yw.models.Users.Role;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.contextComponents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.yugabyte.yw.common.FakeDBApplication;
import com.yugabyte.yw.common.ModelFactory;
import com.yugabyte.yw.common.audit.AuditService;
import com.yugabyte.yw.models.extended.UserWithFeatures;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import play.libs.Json;
import play.mvc.Http;

@RunWith(MockitoJUnitRunner.class)
public class AuditTest extends FakeDBApplication {

  Users user;
  Customer customer;
  Http.Request request;
  Http.Context context;

  AuditService auditService;

  @Before
  public void setUp() {
    auditService = new AuditService();

    customer = ModelFactory.testCustomer("tc1", "Test Customer 1");
    user = ModelFactory.testUser(customer);
    Map<String, String> flashData = Collections.emptyMap();
    Map<String, Object> argData = ImmutableMap.of("user", new UserWithFeatures().setUser(user));
    request = mock(Http.Request.class);
    Long id = 2L;
    play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
    context =
        new Http.Context(id, header, request, flashData, flashData, argData, contextComponents());
    Http.Context.current.set(context);
    when(request.method()).thenReturn("PUT");
    when(request.path()).thenReturn("/api/customer/test/universe/test");
  }

  public Audit createEntry(UUID taskUUID, Users user) {
    return Audit.create(
        user, "/test/api/call", "PUT", null, null, null, null, taskUUID, null, null);
  }

  @Test
  public void testCreate() {
    for (long i = 0; i < 2; i++) {
      UUID randUUID = UUID.randomUUID();
      Audit entry = createEntry(randUUID, user);
      assertSame(i + 1, entry.getAuditID());
      assertEquals("/test/api/call", entry.getApiCall());
      assertEquals("PUT", entry.getApiMethod());
      assertEquals(randUUID, entry.getTaskUUID());
      assertNotNull(entry.getTimestamp());
    }
  }

  @Test
  public void testCreateAuditEntry() {
    auditService.createAuditEntry(context, request);
    List<Audit> entries = Audit.getAll(customer.uuid);
    assertEquals(entries.size(), 1);
    assertEquals(entries.get(0).getUserUUID(), user.uuid);
    assertEquals(entries.get(0).getUserEmail(), user.email);
    assertEquals(entries.get(0).getApiCall(), "/api/customer/test/universe/test");
    assertEquals(entries.get(0).getApiMethod(), "PUT");
    assertNull(entries.get(0).getTarget());
    assertNull(entries.get(0).getTargetID());
    assertNull(entries.get(0).getAction());
    assertNull(entries.get(0).getTaskUUID());
    assertNull(entries.get(0).getPayload());
    assertNotNull(entries.get(0).getTimestamp());
    assertEquals(entries.get(0).getUserAddress(), request.remoteAddress());
  }

  @Test
  public void testCreateAuditEntryWithTaskUUID() {
    UUID randUUID = UUID.randomUUID();
    auditService.createAuditEntry(context, request, randUUID);
    List<Audit> entries = Audit.getAll(customer.uuid);
    assertEquals(entries.size(), 1);
    assertEquals(entries.get(0).getUserUUID(), user.uuid);
    assertEquals(entries.get(0).getUserEmail(), user.email);
    assertEquals(entries.get(0).getApiCall(), "/api/customer/test/universe/test");
    assertEquals(entries.get(0).getApiMethod(), "PUT");
    assertNull(entries.get(0).getTarget());
    assertNull(entries.get(0).getTargetID());
    assertNull(entries.get(0).getAction());
    assertEquals(entries.get(0).getTaskUUID(), randUUID);
    assertNull(entries.get(0).getPayload());
    assertNotNull(entries.get(0).getTimestamp());
    assertEquals(entries.get(0).getUserAddress(), request.remoteAddress());
  }

  @Test
  public void testCreateAuditEntryWithTargetAndAction() {
    Audit.TargetType target = Audit.TargetType.Universe;
    String targetID = "Test TargetID";
    Audit.ActionType action = Audit.ActionType.Create;
    auditService.createAuditEntry(context, request, target, targetID, action);
    List<Audit> entries = Audit.getAll(customer.uuid);
    assertEquals(entries.size(), 1);
    assertEquals(entries.get(0).getUserUUID(), user.uuid);
    assertEquals(entries.get(0).getUserEmail(), user.email);
    assertEquals(entries.get(0).getApiCall(), "/api/customer/test/universe/test");
    assertEquals(entries.get(0).getApiMethod(), "PUT");
    assertEquals(entries.get(0).getTarget(), target);
    assertEquals(entries.get(0).getTargetID(), targetID);
    assertEquals(entries.get(0).getAction(), action);
    assertNull(entries.get(0).getTaskUUID());
    assertNull(entries.get(0).getPayload());
    assertNotNull(entries.get(0).getTimestamp());
    assertEquals(entries.get(0).getUserAddress(), request.remoteAddress());
  }

  @Test
  public void testCreateAuditEntryWithPayload() {
    ObjectNode basePayload = Json.newObject().put("foo", "bar").put("abc", "xyz");

    ObjectNode passwordChildNode = Json.newObject().put("password", "qwerty2");
    JsonNode testPayload =
        basePayload.deepCopy().put("password", "qwerty").set("child", passwordChildNode);

    ObjectNode expectedChildNode = Json.newObject().put("password", SECRET_REPLACEMENT);
    JsonNode expectedPayload =
        basePayload.deepCopy().put("password", SECRET_REPLACEMENT).set("child", expectedChildNode);

    auditService.createAuditEntry(context, request, testPayload);
    List<Audit> entries = Audit.getAll(customer.uuid);
    assertEquals(entries.size(), 1);
    assertEquals(entries.get(0).getUserUUID(), user.uuid);
    assertEquals(entries.get(0).getUserEmail(), user.email);
    assertEquals(entries.get(0).getApiCall(), "/api/customer/test/universe/test");
    assertEquals(entries.get(0).getApiMethod(), "PUT");
    assertNull(entries.get(0).getTarget());
    assertNull(entries.get(0).getTargetID());
    assertNull(entries.get(0).getAction());
    assertNull(entries.get(0).getTaskUUID());
    assertEquals(entries.get(0).getPayload(), expectedPayload);
    assertNotNull(entries.get(0).getTimestamp());
    assertEquals(entries.get(0).getUserAddress(), request.remoteAddress());
  }

  @Test
  public void testCreateAuditEntryWithPayloadAndTaskUUID() {
    UUID randUUID = UUID.randomUUID();
    ObjectNode testPayload = Json.newObject().put("foo", "bar").put("abc", "xyz");
    auditService.createAuditEntry(context, request, testPayload, randUUID);
    List<Audit> entries = Audit.getAll(customer.uuid);
    assertEquals(entries.size(), 1);
    assertEquals(entries.get(0).getUserUUID(), user.uuid);
    assertEquals(entries.get(0).getUserEmail(), user.email);
    assertEquals(entries.get(0).getApiCall(), "/api/customer/test/universe/test");
    assertEquals(entries.get(0).getApiMethod(), "PUT");
    assertNull(entries.get(0).getTarget());
    assertNull(entries.get(0).getTargetID());
    assertNull(entries.get(0).getAction());
    assertEquals(entries.get(0).getTaskUUID(), randUUID);
    assertEquals(entries.get(0).getPayload(), testPayload);
    assertNotNull(entries.get(0).getTimestamp());
    assertEquals(entries.get(0).getUserAddress(), request.remoteAddress());
  }

  @Test
  public void testCreateAuditEntryWithPayloadAndTargetAndAction() {
    ObjectNode testPayload = Json.newObject().put("foo", "bar").put("abc", "xyz");
    Audit.TargetType target = Audit.TargetType.Universe;
    String targetID = "Test TargetID";
    Audit.ActionType action = Audit.ActionType.Create;
    auditService.createAuditEntry(context, request, target, targetID, action, testPayload);
    List<Audit> entries = Audit.getAll(customer.uuid);
    assertEquals(entries.size(), 1);
    assertEquals(entries.get(0).getUserUUID(), user.uuid);
    assertEquals(entries.get(0).getUserEmail(), user.email);
    assertEquals(entries.get(0).getApiCall(), "/api/customer/test/universe/test");
    assertEquals(entries.get(0).getApiMethod(), "PUT");
    assertEquals(entries.get(0).getTarget(), target);
    assertEquals(entries.get(0).getTargetID(), targetID);
    assertEquals(entries.get(0).getAction(), action);
    assertNull(entries.get(0).getTaskUUID());
    assertEquals(entries.get(0).getPayload(), testPayload);
    assertNotNull(entries.get(0).getTimestamp());
    assertEquals(entries.get(0).getUserAddress(), request.remoteAddress());
  }

  @Test
  public void testCreateAuditEntryWithAll() {
    UUID taskUUID = UUID.randomUUID();
    ObjectNode testPayload = Json.newObject().put("foo", "bar").put("abc", "xyz");
    Audit.TargetType target = Audit.TargetType.Universe;
    String targetID = "Test TargetID";
    Audit.ActionType action = Audit.ActionType.Create;
    ObjectNode testAdditionalDetails = Json.newObject().put("fizz", "buzz").put("123", "321");
    auditService.createAuditEntry(
        context, request, target, targetID, action, testPayload, taskUUID, testAdditionalDetails);
    List<Audit> entries = Audit.getAll(customer.uuid);
    assertEquals(entries.size(), 1);
    assertEquals(entries.get(0).getUserUUID(), user.uuid);
    assertEquals(entries.get(0).getUserEmail(), user.email);
    assertEquals(entries.get(0).getApiCall(), "/api/customer/test/universe/test");
    assertEquals(entries.get(0).getApiMethod(), "PUT");
    assertEquals(entries.get(0).getTarget(), target);
    assertEquals(entries.get(0).getTargetID(), targetID);
    assertEquals(entries.get(0).getAction(), action);
    assertEquals(entries.get(0).getTaskUUID(), taskUUID);
    assertEquals(entries.get(0).getPayload(), testPayload);
    assertEquals(entries.get(0).getAdditionalDetails(), testAdditionalDetails);
    assertNotNull(entries.get(0).getTimestamp());
    assertEquals(entries.get(0).getUserAddress(), request.remoteAddress());
  }

  @Test
  public void testGetAll() {
    UUID randUUID = UUID.randomUUID();
    UUID randUUID1 = UUID.randomUUID();
    createEntry(randUUID, user);
    createEntry(randUUID1, user);
    List<Audit> entries = Audit.getAll(customer.uuid);
    assertEquals(entries.size(), 2);
  }

  @Test
  public void testGetFromTaskUUID() {
    UUID randUUID = UUID.randomUUID();
    UUID randUUID1 = UUID.randomUUID();
    createEntry(randUUID, user);
    createEntry(randUUID1, user);
    Audit entry = Audit.getFromTaskUUID(randUUID1);
    assertEquals(entry.getTaskUUID(), randUUID1);
  }

  @Test
  public void testGetAllUserEntries() {
    Users u1 = Users.create("foo@foo.com", "password", Role.Admin, customer.uuid, false);
    UUID randUUID = UUID.randomUUID();
    UUID randUUID1 = UUID.randomUUID();
    UUID randUUID2 = UUID.randomUUID();
    createEntry(randUUID, user);
    createEntry(randUUID1, u1);
    createEntry(randUUID2, u1);
    List<Audit> entries = Audit.getAllUserEntries(u1.uuid);
    List<Audit> entries1 = Audit.getAllUserEntries(user.uuid);
    assertEquals(entries.size(), 2);
    assertEquals(entries1.size(), 1);
  }
}
