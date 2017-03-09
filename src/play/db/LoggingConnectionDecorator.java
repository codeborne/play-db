package play.db;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static java.lang.reflect.Proxy.newProxyInstance;
import static play.db.Reflection.invokeUnwrappingExceptions;

public class LoggingConnectionDecorator implements InvocationHandler {
  private final Connection connection;

  LoggingConnectionDecorator(Connection connection) {
    this.connection = connection;
  }

  @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object result = invokeUnwrappingExceptions(method, connection, args);
    if ("prepareStatement".equals(method.getName())) {
      return newProxyInstance(PreparedStatement.class.getClassLoader(),
          new Class<?>[] {PreparedStatement.class}, new LoggingStatementDecorator((PreparedStatement) result, (String) args[0]));
    }
    return result;
  }

  public static DataSource loggingConnectionDataSourceProxy(DataSource datasource) {
    return (DataSource) Proxy.newProxyInstance(datasource.getClass().getClassLoader(), new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
      Object result = invokeUnwrappingExceptions(method, datasource, args);
      if ("getConnection".equals(method.getName())) {
        return Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, new LoggingConnectionDecorator((Connection) result));
      }
      return result;
    });
  }
}
