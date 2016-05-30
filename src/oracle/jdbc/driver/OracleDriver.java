package oracle.jdbc.driver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

public class OracleDriver {
  public boolean acceptsURL(String url) {
    throw new UnsupportedOperationException();
  }

  public Connection connect(String url, Properties props) throws SQLException {
    throw new UnsupportedOperationException();
  }

  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

}
