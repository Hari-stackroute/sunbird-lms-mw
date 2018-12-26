package org.sunbird.location.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, ElasticSearchUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class LocationActorTest {

  private static ObjectMapper mapper = new ObjectMapper();
  private static final ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(LocationActor.class);
  private static final Map<String, Object> esRespone = new HashMap<>();
  private static Request actorMessage;
  private static Map<String, Object> data = getDataMap();

  @BeforeClass
  public static void init() {

    esRespone.put(JsonKey.CONTENT, new ArrayList<>());
    esRespone.put(GeoLocationJsonKey.LOCATION_TYPE, "STATE");
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.deleteRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getSuccessResponse());
  }

  @Before
  public void setUp() {

    PowerMockito.mockStatic(ElasticSearchUtil.class);
    PowerMockito.when(
            ElasticSearchUtil.complexSearch(
                Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(esRespone);
    PowerMockito.when(
            ElasticSearchUtil.getDataByIdentifier(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(esRespone);
  }

  @Test
  public void testCreateLocation() {

    boolean result = testScenario(LocationActorOperation.CREATE_LOCATION, true, null, null);
    assertTrue(result);
  }

  @Test
  public void testUpdateLocation() {

    boolean result = testScenario(LocationActorOperation.UPDATE_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testDeleteLocation() {

    boolean result = testScenario(LocationActorOperation.DELETE_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testSearchLocation() {

    boolean result = testScenario(LocationActorOperation.SEARCH_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationWithInvalidValue() {

    data.put(GeoLocationJsonKey.LOCATION_TYPE, "anyType");
    boolean result =
        testScenario(
            LocationActorOperation.CREATE_LOCATION, false, data, ResponseCode.invalidValue);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationWithoutMandatoryParams() {

    data.put(GeoLocationJsonKey.LOCATION_TYPE, "block");
    boolean result =
        testScenario(
            LocationActorOperation.CREATE_LOCATION,
            false,
            data,
            ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationWithParentNotAllowed() {

    data.put(GeoLocationJsonKey.LOCATION_TYPE, "state");
    data.put(GeoLocationJsonKey.PARENT_CODE, "anyCode");
    boolean result =
        testScenario(
            LocationActorOperation.CREATE_LOCATION, false, data, ResponseCode.parentNotAllowed);
    assertTrue(result);
  }

  @Test
  public void testDeleteLocationWithInvalidLocationDeleteRequest() {

    esRespone.put(JsonKey.CONTENT, new ArrayList<>());
    PowerMockito.when(
            ElasticSearchUtil.complexSearch(
                Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getEsMap());
    boolean result =
        testScenario(
            LocationActorOperation.DELETE_LOCATION,
            false,
            data,
            ResponseCode.invalidLocationDeleteRequest);
    assertTrue(result);
  }

  private Map<String, Object> getEsMap() {

    List<Map<String, Object>> lst = new ArrayList<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("any", "any");
    lst.add(innerMap);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CONTENT, lst);
    return map;
  }

  private boolean testScenario(
      LocationActorOperation actorOperation,
      boolean isSuccess,
      Map<String, Object> data,
      ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    actorMessage = new Request();
    if (data != null) actorMessage.getRequest().putAll(data);
    actorMessage.setOperation(actorOperation.getValue());
    subject.tell(actorMessage, probe.getRef());

    if (isSuccess) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  private static Map<String, Object> getDataMap() {

    data = new HashMap();
    data.put(GeoLocationJsonKey.LOCATION_TYPE, "STATE");
    data.put(GeoLocationJsonKey.CODE, "S01");
    data.put(JsonKey.NAME, "DUMMY_STATE");
    data.put(JsonKey.ID, "id_01");
    return data;
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }
}
