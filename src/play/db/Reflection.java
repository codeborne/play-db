package play.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class Reflection {
  public static <T> T invokeUnwrappingExceptions(Method method, Object target, Object ... args) throws Throwable {
    try {
      return (T) method.invoke(target, args);
    }
    catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
  }
}
