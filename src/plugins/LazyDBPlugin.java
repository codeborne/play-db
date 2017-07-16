package plugins;

import com.mysql.jdbc.AbandonedConnectionCleanupThread;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.db.DB;
import play.db.DBPlugin;
import play.db.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static play.db.LoggingConnectionDecorator.loggingConnectionDataSourceProxy;

public class LazyDBPlugin extends DBPlugin {
  private static final Logger logger = LoggerFactory.getLogger(LazyDBPlugin.class);
  private DBModifier dbModifier = new DBModifier();

  @Override public void onLoad() {
    // disable built-in DBPlugin (if it's not already disabled)
    DBPlugin dbPlugin = Play.pluginCollection.getPluginInstance(DBPlugin.class);
    if (dbPlugin != this) {
      Play.pluginCollection.disablePlugin(dbPlugin);
    }
  }

  @Override public void onApplicationStart() {
    if (DB.datasource != null) {
      logger.warn("Not rebuilding DB connection pool on restart");
      return;
    }

    super.onApplicationStart();
    setPreferredTestQueryForConnectionPool();
    dbModifier.makeDataSourceLazyAndTrackable();
    stopUselessMySqlCleanupThread();
  }

  @Override public void onApplicationStop() {
    DB.destroyAll();
  }

  @Override public String getStatus() {
    return null; // wrapped DataSource is not supported by DBPlugin, but we have ConnectionMonitoringPlugin for that
  }

  private void setPreferredTestQueryForConnectionPool() {
    HikariDataSource ds = (HikariDataSource) DB.datasource;
    String testQuery = Play.configuration.getProperty("db.testquery");
    if (Play.mode.isProd() && isNotEmpty(testQuery) && ds.getJdbcUrl().contains("oracle")) {
      ds.setConnectionTestQuery(testQuery);
//      ds.setConnectionCustomizerClassName(OracleConnectionCustomizer.class.getName());
    }
  }
  
  private class DBModifier extends DB {
    void makeDataSourceLazyAndTrackable() {
      // use Lazy connections with Play datasource to avoid getting connections for read-only requests
      datasource = wrapDataSource(datasource);

      Map<String, ExtendedDatasource> originalDataSources = new HashMap<>(datasources);
      for (Map.Entry<String, ExtendedDatasource> entry : originalDataSources.entrySet()) {
        DataSource lazyDataSource = wrapDataSource(entry.getValue().getDataSource());
        datasources.put(entry.getKey(), new ExtendedDatasource(lazyDataSource, destroyMethod));
      }
    }

    private DataSource wrapDataSource(DataSource originalDataSource) {
      DataSource dataSource = new LazyConnectionDataSourceProxy(originalDataSource);
      if ("true".equals(Play.configuration.getProperty("trackSlowSQL", "false"))) {
        dataSource = loggingConnectionDataSourceProxy(dataSource);
      }
      return dataSource;
    }
  }

  private void stopUselessMySqlCleanupThread() {
    if (!"com.mysql.jdbc.Driver".equals(Play.configuration.getProperty("db.driver"))) {
      AbandonedConnectionCleanupThread.uncheckedShutdown();
    }
  }
}
