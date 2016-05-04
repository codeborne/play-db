package play.db;

import com.google.common.collect.EvictingQueue;
import play.mvc.Http;
import play.mvc.Scope;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Collections.synchronizedCollection;
import static java.util.stream.Collectors.groupingBy;

public class SlowSQLHelper {

  private static final Collection<LogEntry> logs = synchronizedCollection(EvictingQueue.create(10000));
  public static final int SLOW_AVERAGE_SQL_THRESHOLD_IN_MS = 100;
  public static final int SLOW_TOTAL_SQL_THRESHOLD_IN_MS = 200;

  public static void addSlowSQLLog(String sql, long duration) {
    Http.Request request = Http.Request.current();
    String requestId = request != null ? "[" + request.args.get("requestId") + "] " + request.action : "[job]";
    String sessionId = Scope.Session.current() != null ? Scope.Session.current().getId() : "job";

    Optional<LogEntry> logEntry = doInSynchronizedBlock(
        () -> logs.stream().filter(l -> l.request.equals(requestId) && l.sql.equals(sql) && l.sessionId.equals(sessionId)).findFirst());
    if (logEntry.isPresent()) {
      LogEntry entry = logEntry.get();
      entry.totalDuration += duration;
      entry.count++;
    } else {
      logs.add(new LogEntry(sql, requestId, duration, sessionId));
    }
  }

  public static class LogEntry {
    public String sql;
    public String request;
    public String sessionId;
    public Long totalDuration;
    public Long count = 1L;
    public static Pattern shortenSelect = Pattern.compile("\\bselect\\s.+?\\sfrom\\s", Pattern.DOTALL);
    public static Pattern shortenUpdate = Pattern.compile("(\\bupdate\\s.+?\\sset)\\s.+?\\swhere\\s", Pattern.DOTALL);

    LogEntry(String sql, String requestId, Long initialDuration, String sessionId) {
      this.sql = sql;
      this.request = requestId;
      this.totalDuration = initialDuration;
      this.sessionId = sessionId;
    }

    public boolean isSlow() {
      return totalDuration > SLOW_TOTAL_SQL_THRESHOLD_IN_MS || totalDuration/count > SLOW_AVERAGE_SQL_THRESHOLD_IN_MS;
    }

    // eclipse compiler requires it for logsByRequest() to work
    public String getRequest() {
      return request;
    }

    public String getShortenedSQL() {
      String result = shortenSelect.matcher(sql).replaceAll("select ... from ");
      return shortenUpdate.matcher(result).replaceAll("$1 ... where ").trim();
    }
  }

  public static class LoggingConnectionDecorator implements InvocationHandler {
    private Connection connection;

    public LoggingConnectionDecorator(Connection connection) {
      this.connection = connection;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (method.getName()) {
        case "prepareStatement":
          PreparedStatement preparedStatement = (PreparedStatement) method.invoke(connection, args);
          return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
              new Class<?>[] {PreparedStatement.class}, new LoggingStatementDecorator(preparedStatement, (String) args[0]));
        default: return method.invoke(connection, args);
      }
    }

  }

  public static class LoggingStatementDecorator implements InvocationHandler {
    private PreparedStatement statement;
    private String sql;

    public LoggingStatementDecorator(PreparedStatement statement, String sql) {
      this.statement = statement;
      this.sql = sql;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (method.getName()) {
        case "executeBatch": return executeWithLogging(method, args);
        case "executeQuery": return executeWithLogging(method, args);
        case "executeUpdate": return executeWithLogging(method, args);
        default: return method.invoke(statement, args);
      }
    }

    private Object executeWithLogging(Method method, Object[] args) throws Throwable {
      long now = System.currentTimeMillis();
      try {
        return method.invoke(statement, args);
      }
      finally {
        addSlowSQLLog(sql, System.currentTimeMillis() - now);
      }
    }
  }

  public static long slowSQLCount(String sessionId) {
    return doInSynchronizedBlock(() -> logs.stream().filter(userSessionOrJob(sessionId)).filter(LogEntry::isSlow).map(l -> l.sql).distinct().count());
  }

  public static long uniqueSQLCount(String sessionId) {
    return doInSynchronizedBlock(() -> logs.stream().filter(userSessionOrJob(sessionId)).map(l -> l.sql).distinct().count());
  }

  public static Map<String, List<LogEntry>> logsByRequest(String sessionId) {
    return doInSynchronizedBlock(() -> logs.stream().filter(userSessionOrJob(sessionId)).collect(groupingBy(LogEntry::getRequest)));
  }

  public static void reset(String sessionId) {
    doInSynchronizedBlock(() -> {logs.removeIf(userSessionOrJob(sessionId)); return null;});
  }

  private static Predicate<LogEntry> userSessionOrJob(String sessionId) {
    return l -> l.sessionId.equals(sessionId) || "job".equals(l.sessionId);
  }

  // synchronized collection iteration must be done in synchronized block according to javadoc
  private static <T> T doInSynchronizedBlock(Callable<T> callable) {
    synchronized (logs) {
      try {
        return callable.call();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
