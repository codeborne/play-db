package play.db;

import org.junit.After;
import org.junit.Test;
import play.Play;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TimeoutAwareOracleDriverTest {
  Connection connection = mock(Connection.class);

  @After
  public void tearDown() {
    Play.configuration.clear();
  }

  @Test
  public void setsTimeoutsToOracleConnection() throws SQLException {
    Play.configuration.setProperty("db.default.connectTimeout", "5s");
    Play.configuration.setProperty("db.default.readTimeout", "2mn");

    TimeoutAwareOracleDriver driver = spy(new TimeoutAwareOracleDriver());
    doReturn(connection).when(driver).superConnect(anyString(), any());
    Properties properties = new Properties();

    driver.connect("jdbc:ex:oracle:thin:@host:port:schema", properties);

    verify(driver).superConnect("jdbc:oracle:thin:@host:port:schema", properties);
    assertEquals("5000", properties.getProperty("oracle.net.CONNECT_TIMEOUT"));
    assertEquals("120000", properties.getProperty("oracle.jdbc.ReadTimeout"));
  }
}