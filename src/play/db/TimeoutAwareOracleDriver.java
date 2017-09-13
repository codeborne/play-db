package play.db;

import oracle.jdbc.driver.OracleDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import static play.libs.Time.parseDuration;

public class TimeoutAwareOracleDriver extends OracleDriver {
  private static final Logger logger = LoggerFactory.getLogger(TimeoutAwareOracleDriver.class);

  private int CONNECT_TIMEOUT_SEC = parseDuration(Play.configuration.getProperty("db.default.connectTimeout", "5s"));
  private String CONNECT_TIMEOUT_MS = String.valueOf(1000 * CONNECT_TIMEOUT_SEC);
  private String READ_TIMEOUT_MS = String.valueOf(1000 * parseDuration(Play.configuration.getProperty("db.default.readTimeout", "1mn")));

  public TimeoutAwareOracleDriver() {
    DriverManager.setLoginTimeout(CONNECT_TIMEOUT_SEC);
    logger.info("Using " + getClass().getSimpleName());
  }

  @Override public boolean acceptsURL(String url) {
    return url.startsWith("jdbc:ex:oracle");
  }

  @Override public Connection connect(String url, Properties props) throws SQLException {
    url = url.replace("jdbc:ex:", "jdbc:");
    props.setProperty("oracle.net.CONNECT_TIMEOUT", CONNECT_TIMEOUT_MS);
    props.setProperty("oracle.jdbc.ReadTimeout", READ_TIMEOUT_MS);
    return superConnect(url, props);
  }

  Connection superConnect(String url, Properties props) throws SQLException {
    return super.connect(url, props);
  }

  @Override public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }
}
