/** */
package org.sunbird.learner.util;

import java.util.concurrent.ScheduledExecutorService;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

/** @author Manzarul All the scheduler job will be handle by this class. */
public class SchedulerManager {

  /*
   * service ScheduledExecutorService object
   */
  public static ScheduledExecutorService service = ExecutorManager.getExecutorService();

  /** all scheduler job will be configure here. */
  public static void schedule() {
    ProjectLogger.log(
        "SchedulerManager:schedule: Started scheduler job for cache refresh.",
        LoggerEnum.INFO.name());
  }
}
