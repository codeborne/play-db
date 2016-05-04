package play.db;

import org.junit.Test;
import play.db.SlowSQLHelper.LogEntry;

import static org.junit.Assert.assertEquals;

public class LogEntryTest {
  @Test
  public void getShortenedSQL() {
    assertEquals("select ... from table1", logEntry("select * from table1").getShortenedSQL());
    assertEquals("select ... from table1", logEntry("select field1, field2, field3 from table1").getShortenedSQL());
    assertEquals("select ... from table1 where field1=?", logEntry("select field1, field2, field3 from table1 where field1=?").getShortenedSQL());
    assertEquals("select ... from table1 where field1 in (select ... from table 2)", logEntry("select field1, field2, field3 from table1 where field1 in (select id from table 2)").getShortenedSQL());
    assertEquals("select ... from table1 where field1 in (\nselect ... from table 2\n)", logEntry("select field1, field2, field3 from table1 where field1 in (\nselect id from table 2\n)").getShortenedSQL());
    assertEquals("select ... from table1\nwhere field1=?", logEntry("\nselect\n*\nfrom\ntable1\nwhere field1=?").getShortenedSQL());
    assertEquals("select ... from table1", logEntry("select 'selectize.js', field1, 'from' from table1").getShortenedSQL());
    assertEquals("update table1 set ... where field3=?", logEntry("update table1 set field1=?1 where field3=?").getShortenedSQL());
    assertEquals("update table1 set ... where field3=?", logEntry("update table1 set field1=?1, field2=? where field3=?").getShortenedSQL());
    assertEquals("update\ntable1\nset ... where field3=?", logEntry("\nupdate\ntable1\nset\nfield1=?1,\nfield2=?\nwhere\nfield3=?").getShortenedSQL());
  }

  private LogEntry logEntry(String sql) {
    return new LogEntry(sql, null, null, null);
  }
}