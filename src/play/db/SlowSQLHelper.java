package play.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import play.mvc.Http;
import play.mvc.Scope;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.groupingBy;

public class SlowSQLHelper {
  private static final Cache<String, LogEntry> logs = CacheBuilder.newBuilder()
      .expireAfterWrite(10, MINUTES)
      .maximumSize(10000)
      .build();
  
  public static void addSlowSQLLog(String sql, long duration) throws ExecutionException {
    Http.Request request = Http.Request.current();
    String requestId = request != null ? "[" + request.args.get("requestId") + "] " + request.action : "[job]";
    String sessionId = Scope.Session.current() != null ? Scope.Session.current().getId() : "job";

    String key = String.format("%s|%s|%s", sessionId, requestId, sql);
    LogEntry log = logs.get(key, () -> new LogEntry(sql, requestId, sessionId));
    log.addExecution(duration);
  }
  
  private static Stream<LogEntry> sessionLogs(String sessionId) {
    return logs.asMap().values().stream().filter(userSessionOrJob(sessionId));
  }

  public static long slowSQLCount(String sessionId) {
    return sessionLogs(sessionId).filter(LogEntry::isSlow).map(log -> log.sql).distinct().count();
  }

  public static long uniqueSQLCount(String sessionId) {
    return sessionLogs(sessionId).map(log -> log.sql).distinct().count();
  }

  public static Map<String, List<LogEntry>> logsByRequest(String sessionId) {
    return sessionLogs(sessionId).collect(groupingBy(log -> log.request));
  }

  public static void reset(String sessionId) {
    Predicate<LogEntry> predicate = userSessionOrJob(sessionId);
    for (Map.Entry<String, LogEntry> entry : logs.asMap().entrySet()) {
      if (predicate.test(entry.getValue())) {
        logs.asMap().remove(entry.getKey());
      }
    }
  }

  private static Predicate<LogEntry> userSessionOrJob(String sessionId) {
    return log -> log.sessionId.equals(sessionId) || "job".equals(log.sessionId);
  }
}
