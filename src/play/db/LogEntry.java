package play.db;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class LogEntry {
  private static final int SLOW_AVERAGE_SQL_THRESHOLD_IN_MS = 100;
  private static final int SLOW_TOTAL_SQL_THRESHOLD_IN_MS = 200;

  public final String sql;
  public final String request;
  public final String sessionId;
  private final AtomicLong totalDuration = new AtomicLong();
  private final AtomicLong count = new AtomicLong();
  private static final Pattern shortenSelect = Pattern.compile("\\bselect\\s.+?\\sfrom\\s", Pattern.DOTALL);
  private static final Pattern shortenUpdate = Pattern.compile("(\\bupdate\\s.+?\\sset)\\s.+?\\swhere\\s", Pattern.DOTALL);

  LogEntry(String sql, String requestId, String sessionId) {
    this.sql = sql;
    this.request = requestId;
    this.sessionId = sessionId;
  }

  public boolean isSlow() {
    return totalDuration.get() > SLOW_TOTAL_SQL_THRESHOLD_IN_MS ||
        totalDuration.get()/count.get() > SLOW_AVERAGE_SQL_THRESHOLD_IN_MS;
  }

  public String getShortenedSQL() {
    String result = shortenSelect.matcher(sql).replaceAll("select ... from ");
    return shortenUpdate.matcher(result).replaceAll("$1 ... where ").trim();
  }

  public void addExecution(long duration) {
    totalDuration.addAndGet(duration);
    count.incrementAndGet();
  }
}
