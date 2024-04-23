// Copyright 2012 Square Inc.
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.

package com.squareup.squash;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Creates the Squash stacktrace format for serialization by gson. */
public final class SquashBacktrace {

  private SquashBacktrace() {
    // Should not be instantiated: this is a utility class.
  }

  /**
   * takes a `Throwable` parameter and returns a list of `SquashException` objects
   * representing the backtraces of the thrown exception, with each object containing
   * the thread name, the true flag, and the stack trace array.
   * 
   * @param error Throwable object to be analyzed for backtraces.
   * 
   * 1/ If `error` is null, then `null` is returned as output.
   * 2/ Otherwise, an instance of `SquashException` is created with the name of the
   * current thread and a boolean flag indicating whether it is a nested exception or
   * not.
   * 3/ The `getStacktraceArray` function is called on the `error` object to obtain an
   * array of stack traces for the current thread. This array is then added to the
   * instance of `SquashException`.
   * 
   * @returns a list of `SquashException` objects representing the backtraces of the
   * Throwable argument.
   * 
   * 	- The output is a `List` of `SquashException` objects.
   * 	- Each `SquashException` object in the list represents an exception that occurred
   * in a particular thread.
   * 	- The `SquashException` objects contain information about the exception, including
   * the thread name, the error message, and the stack trace.
   * 	- The stack trace is represented as an array of `Throwable` objects, which provides
   * information about the nesting of exceptions.
   */
  public static List<SquashException> getBacktraces(Throwable error) {
    if (error == null) {
      return null;
    }
    final List<SquashException> threadList = new ArrayList<SquashException>();
    final SquashException currentThread =
        new SquashException(Thread.currentThread().getName(), true, getStacktraceArray(error));
    threadList.add(currentThread);
    return threadList;
  }

  /**
   * generates a list of `StackElement` objects representing the elements of a stack
   * trace associated with a `Throwable` object.
   * 
   * @param error Throwable object containing the stack trace information to be processed
   * and returned as a list of StackElement objects.
   * 
   * 	- `error`: This is a `Throwable` object that represents an exception or error in
   * the code. It provides information about the error, such as its class name, file
   * name, line number, and method name.
   * 	- `StackTraceElement[]`: This is an array of objects that represent the stack
   * trace elements of the error. Each element contains information about the location
   * in the code where the error occurred.
   * 
   * @returns a list of `StackElement` objects containing information about each stack
   * trace element.
   * 
   * 	- `List<StackElement>`: The output is a list of StackElement objects representing
   * the stack trace elements.
   * 	- `StackElement`: Each element in the list represents a stack frame, consisting
   * of four attributes:
   * 	+ `className`: The name of the class that defines the method being executed.
   * 	+ `fileName`: The name of the file where the method is defined.
   * 	+ `lineNumber`: The line number of the method definition within the file.
   * 	+ `methodName`: The name of the method being executed.
   * 
   * The list contains elements representing each frame in the stack trace, starting
   * from the innermost frame and proceeding outward to the outermost frame.
   */
  private static List<StackElement> getStacktraceArray(Throwable error) {
    List<StackElement> stackElems = new ArrayList<StackElement>();
    for (StackTraceElement element : error.getStackTrace()) {
      StackElement elementList =
          new StackElement(element.getClassName(), element.getFileName(), element.getLineNumber(),
              element.getMethodName());
      stackElems.add(elementList);
    }
    return stackElems;
  }

  /**
   * returns a map of instance variables (ivars) of a given Throwable object, including
   * non-static and non-mockito fields.
   * 
   * @param error Throwable object for which the IVARs are to be extracted.
   * 
   * 	- `null`: The possible value of `error`, which indicates an absence of any error.
   * 	- `Field[] fields`: An array of `Field` objects representing the fields of the
   * deserialized `error`.
   * 	- `Modifier.isStatic(field.getModifiers())`: A boolean value indicating whether
   * the field is a static field or not. The function ignores static fields, as they
   * are not relevant to the task at hand.
   * 	- `!field.getName().startsWith("CGLIB")`: Another boolean value indicating whether
   * the field name starts with "CGLIB", which represents mockito stuff in tests. The
   * function ignores these fields, as they are not part of the main function's functionality.
   * 	- `field.isAccessible()`: A boolean value indicating whether the field is accessible
   * or not. If the field is not accessible, it is made accessible before being accessed.
   * 	- `Object val`: The value of the field.
   * 
   * These properties/attributes are used to create a map of field names and corresponding
   * values, which represents the ivars (instance variables) of the `error` object.
   * 
   * @returns a map of class fields with their values for an provided `Throwable` error.
   * 
   * 	- The output is a `Map` of `String` to `Object`, where each key is the name of a
   * field in the `Throwable` class and each value is the value of that field in the
   * `error` object.
   * 	- The `Map` is created using a `HashMap` instance, which is initialized with the
   * `null` value for the keys if the input `Throwable` is null.
   * 	- The fields are identified using the `getClass().getDeclaredFields()` method,
   * which returns an array of `Field` objects representing all the public and protected
   * fields in the class, including static fields.
   * 	- Each field is checked against a series of conditions before being included in
   * the `Map`:
   * 	+ It must not be static (ignored using `Modifier.isStatic(field.getModifiers())`).
   * 	+ It must not start with "CGLIB" (ignored using a regular expression).
   * 	+ It must have non-private access modifier (checked using `field.isAccessible()`
   * and `field.setAccessible(true)` if necessary).
   * 	- If any of the conditions are false, the field is added to the `Map` with its
   * name as key and its value as value.
   * 	- The `catch` block handles any exceptions that occur during field access using
   * the `IllegalAccessException`. In this case, the field name is printed along with
   * an exception message.
   * 
   * In summary, the `getIvars` function returns a `Map` of fields in the input `Throwable`
   * object to their corresponding values, excluding static and mockito fields, and
   * handles any exceptions that may occur during field access.
   */
  public static Map<String, Object> getIvars(Throwable error) {
    if (error == null) {
      return null;
    }
    Map<String, Object> ivars = new HashMap<String, Object>();
    final Field[] fields = error.getClass().getDeclaredFields();
    for (Field field : fields) {
      try {
        if (!Modifier.isStatic(field.getModifiers()) // Ignore static fields.
            && !field.getName().startsWith("CGLIB")) { // Ignore mockito stuff in tests.
          if (!field.isAccessible()) {
            field.setAccessible(true);
          }
          Object val = field.get(error);
          ivars.put(field.getName(), val);
        }
      } catch (IllegalAccessException e) {
        ivars.put(field.getName(), "Exception accessing field: " + e);
      }
    }
    return ivars;
  }

  /**
   * Recursive method that follows the "cause" exceptions all the way down the stack, adding them to
   * the passed-in list.
   */
  public static void populateNestedExceptions(List<NestedException> nestedExceptions,
      Throwable error) {
    // Only keep processing if the "cause" exception is set and != the "parent" exception.
    if (error == null || error.getCause() == null || error.getCause() == error) {
      return;
    }
    final Throwable cause = error.getCause();
    NestedException doc =
        new NestedException(cause.getClass().getName(), cause.getMessage(), getBacktraces(cause),
            getIvars(cause));
    nestedExceptions.add(doc);
    // Exceptions all the way down!
    populateNestedExceptions(nestedExceptions, cause);
  }

  /** Wrapper object for top-level exceptions. */
  static final class SquashException {
    final String name;
    final boolean faulted;
    final List<StackElement> backtrace;

    public SquashException(String name, boolean faulted, List<StackElement> backtrace) {
      this.backtrace = backtrace;
      this.name = name;
      this.faulted = faulted;
    }
  }

  /** Wrapper object for nested exceptions. */
  static final class NestedException {
    final String class_name;
    final String message;
    final List<SquashException> backtraces;
    final Map<String, Object> ivars;

    public NestedException(String className, String message, List<SquashException> backtraces,
        Map<String, Object> ivars) {
      this.class_name = className;
      this.message = message;
      this.backtraces = backtraces;
      this.ivars = ivars;
    }
  }

  /** Wrapper object for a stacktrace entry. */
  static final class StackElement {
    // This field is necessary so Squash knows that this is a java stacktrace that might need
    // obfuscation lookup and git filename lookup.  Our stacktrace elements don't give us the full
    // path to the java file, so Squash has to do a SCM lookup to try and do its cause analysis.
    @SuppressWarnings("UnusedDeclaration") final String type = "obfuscated";
    final String file;
    final int line;
    final String symbol;
    final String class_name;

    private StackElement(String className, String file, int line, String methodName) {
      this.class_name = className;
      this.file = file;
      this.line = line;
      this.symbol = methodName;
    }
  }
}
