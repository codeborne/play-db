package play.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;

import static play.db.Reflection.invokeUnwrappingExceptions;

class LoggingStatementDecorator implements InvocationHandler {
  private PreparedStatement statement;
  private String sql;

  LoggingStatementDecorator(PreparedStatement statement, String sql) {
    this.statement = statement;
    this.sql = sql;
  }

  @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    switch (method.getName()) {
      case "executeBatch":
      case "executeQuery":
      case "executeUpdate":
        return invokeWithLogging(method, args);
      default:
        return invokeUnwrappingExceptions(method, statement, args);
    }
  }

  private Object invokeWithLogging(Method method, Object[] args) throws Throwable {
    long now = System.currentTimeMillis();
    try {
      return invokeUnwrappingExceptions(method, statement, args);
    }
    finally {
      SlowSQLHelper.addSlowSQLLog(sql, System.currentTimeMillis() - now);
    }
  }
}
