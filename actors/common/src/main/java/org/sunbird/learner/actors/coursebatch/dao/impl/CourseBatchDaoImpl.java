package org.sunbird.learner.actors.coursebatch.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.CourseBatchDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.course.batch.CourseBatch;

public class CourseBatchDaoImpl implements CourseBatchDao {
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo courseBatchDb = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
  private static CourseBatchDao courseBatchDao;
  private Util.DbInfo userCourseDb = Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB);
  private ObjectMapper mapper = new ObjectMapper();

  public static CourseBatchDao getInstance() {
    if (courseBatchDao == null) {
      courseBatchDao = new CourseBatchDaoImpl();
    }
    return courseBatchDao;
  }

  @Override
  public Response create(Map<String, Object> map) {
    return cassandraOperation.insertRecord(
        courseBatchDb.getKeySpace(), courseBatchDb.getTableName(), map);
  }

  @Override
  public Response update(Map<String, Object> map) {
    return cassandraOperation.updateRecord(
        courseBatchDb.getKeySpace(), courseBatchDb.getTableName(), map);
  }

  @Override
  public CourseBatch readById(String id) {
    Response courseBatchResult =
        cassandraOperation.getRecordById(
            courseBatchDb.getKeySpace(), courseBatchDb.getTableName(), id);
    List<Map<String, Object>> courseList =
        (List<Map<String, Object>>) courseBatchResult.get(JsonKey.RESPONSE);
    if ((courseList.isEmpty())) {
      throw new ProjectCommonException(
          ResponseCode.invalidCourseBatchId.getErrorCode(),
          ResponseCode.invalidCourseBatchId.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    } else {
      return mapper.convertValue(courseList.get(0), CourseBatch.class);
    }
  }

  @Override
  public Response delete(String id) {
    return cassandraOperation.deleteRecord(
        courseBatchDb.getKeySpace(), courseBatchDb.getTableName(), id);
  }

  @Override
  public Response createCourseEnrolment(Map<String, Object> map) {
    return cassandraOperation.insertRecord(
        userCourseDb.getKeySpace(), userCourseDb.getTableName(), map);
  }
}
