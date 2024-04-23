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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * tests the ability to serialize and deserialize SquashEntries with complex backtraces
 * containing infinitely nested exceptions. The test class creates mock Throwable
 * objects representing the exception hierarchy, sets their messages and stack traces,
 * and then serializes and deserializes a SquashEntry containing these exceptions.
 * The test verifies that the deserialized entry contains the correct backtrace
 * information, including the nested exceptions, and that the fields of the entry are
 * correctly populated.
 */
public class SquashEntryTest {

  private Gson gson = new Gson();
  private EntryFactory factory = new EntryFactory();

  /**
   * tests that a deserialized `SquashEntry` object has no exception backtraces, ivars,
   * or parent exceptions, and its log message is equal to the original message passed
   * in.
   */
  @Test public void testNoException() throws Exception {
    final String message = "I LOVE TACOS";
    final SquashEntry logEntry = factory.create(message, null);
    SquashEntry deserialized = serializeAndDeserialize(logEntry);
    assertThat(deserialized.backtraces).isNull();
    assertThat(deserialized.ivars).isNull();
    assertThat(deserialized.log_message).isEqualTo(message);
    assertThat(deserialized.parent_exceptions).isEmpty();
    assertThat(deserialized.class_name).isNull();
  }

  /**
   * takes a `SquashEntry` log entry and returns its serialized form using `Gson`
   * library, and then deserializes it back to the original log entry format.
   * 
   * @param logEntry `SquashEntry` object to be serialized and deserialized.
   * 
   * 	- `gson`: The `Gson` class used for serialization and deserialization.
   * 	- `SquashEntry`: The class representing a single entry in a squash log, which
   * contains various attributes such as `id`, `timestamp`, `data`, and `type`.
   * 
   * @returns a serialized version of the input `SquashEntry` object.
   * 
   * 	- The output is a `SquashEntry` object, which represents a log entry in the Squash
   * format.
   * 	- The object contains the log entry data, including its timestamp, message, and
   * other attributes.
   * 	- The `SquashEntry` class is from the `com.google.gson.annotations.SerializedName`
   * package, indicating that the class has been annotated with serialization information.
   * 	- The `fromJson` method of the `Gson` class is used to convert the JSON representation
   * of the log entry into a `SquashEntry` object.
   */
  private SquashEntry serializeAndDeserialize(SquashEntry logEntry) throws IOException {
    return gson.fromJson(gson.toJson(logEntry), SquashEntry.class);
  }

  /**
   * tests the deserialization of a SquashEntry containing an exception with a stack
   * trace and no instance variables.
   */
  @Test public void testWithSimpleExceptionNoIvars() throws Exception {
    final String logMessage = "I LOVE TACOS";
    final Throwable exception = mock(Throwable.class);
    StackTraceElement s0 =
        new StackTraceElement("com.taco.Taco", "digest", "core-android/src/com/taco/Taco.java", 50);
    StackTraceElement s1 =
        new StackTraceElement("com.taco.Taco", "eat", "core-android/src/com/taco/Taco.java", 80);
    StackTraceElement s2 =
        new StackTraceElement("com.taco.Dude", "purchase", "core-android/src/com/taco/Dude.java",
            112);
    StackTraceElement[] myLittleStackTrace = new StackTraceElement[] {s0, s1, s2};
    final String message = "ExceptionMessage";
    when(exception.getMessage()).thenReturn(message);
    when(exception.getStackTrace()).thenReturn(myLittleStackTrace);
    final SquashEntry logEntry = factory.create(logMessage, exception);
    SquashEntry deserialized = serializeAndDeserialize(logEntry);
    assertThat(deserialized.backtraces).isNotEmpty();
    final SquashBacktrace.SquashException backtrace = deserialized.backtraces.get(0);
    assertThat(backtrace.name).isEqualTo(Thread.currentThread().getName());
    assertThat(backtrace.faulted).isEqualTo(true);
    List<SquashBacktrace.StackElement> stackElements = backtrace.backtrace;
    assertBacktracesMatch(myLittleStackTrace, stackElements);
    assertThat(deserialized.ivars).isEmpty();
    assertThat(deserialized.log_message).isEqualTo(logMessage);
    assertThat(deserialized.message).isEqualTo(message);
    assertThat(deserialized.parent_exceptions).isEmpty();
    assertThat(deserialized.class_name).isEqualTo(exception.getClass().getName());
  }

  /**
   * tests a `Throwable` object with no message and a stack trace containing two elements,
   * serializes it, and then compares the deserialized message to the original one.
   */
  @Test public void testExceptionWithNoMessage() throws Exception {
    final Throwable exception = mock(Throwable.class);

    StackTraceElement s0 = new StackTraceElement("com.jake", "CantProgram",
        "core-android/src/com/jake/Brain.java", 50);
    StackTraceElement s1 = new StackTraceElement("com.jake", "IsDrunk",
        "core-android/src/com/jake/Status.java", 510);
    StackTraceElement[] stackTrace = { s0, s1 };

    when(exception.getMessage()).thenReturn(null);
    when(exception.getStackTrace()).thenReturn(stackTrace);

    String logMessage = "Jake can't program";
    final SquashEntry logEntry = factory.create(logMessage, exception);
    SquashEntry deserialized = serializeAndDeserialize(logEntry);
    assertThat(deserialized.message).isEqualTo(logMessage);
  }

  /**
   * tests whether a mocked `Throwable` object's `getMessage()` method returns `null`
   * and its `getStackTrace()` method returns an array of `StackTraceElement` objects
   * that represent the stack trace of the exception.
   */
  @Test public void testExceptionWithNoMessageOrLogMessage() throws Exception {
    final Throwable exception = mock(Throwable.class);

    StackTraceElement s0 = new StackTraceElement("com.jake", "CantProgram",
        "core-android/src/com/jake/Brain.java", 50);
    StackTraceElement s1 = new StackTraceElement("com.jake", "IsDrunk",
        "core-android/src/com/jake/Status.java", 510);
    StackTraceElement[] stackTrace = { s0, s1 };

    when(exception.getMessage()).thenReturn(null);
    when(exception.getStackTrace()).thenReturn(stackTrace);

    final SquashEntry logEntry = factory.create(null, exception);
    SquashEntry deserialized = serializeAndDeserialize(logEntry);
    assertThat(deserialized.message).isEqualTo("No message");
  }

  /**
   * compares two stack traces and asserts that each element in one match exactly with
   * their corresponding counterpart in the other.
   * 
   * @param myLittleStackTrace local stack trace of the current method, which is being
   * compared to a list of expected stack elements provided by the `stackElements` parameter.
   * 
   * 	- `myLittleStackTrace`: A `StackTraceElement[]` array containing the stack traces
   * to be compared.
   * 	- `stackElements`: A list of `SquashBacktrace.StackElement` objects representing
   * the elements in the stack traces to be compared.
   * 
   * The function iterates through each element in `stackElements`, comparing the
   * properties of each element (`file`, `line`, `symbol`, and `class_name`) with their
   * corresponding properties in `myLittleStackTrace`. The `assertThat()` method is
   * used for these comparisons, which provides a way to assert that two objects are equal.
   * 
   * @param stackElements list of stack elements that will be compared with the expected
   * backtrace elements.
   * 
   * 	- `stackElementsSize`: The size of the list of `StackElement` objects in `stackElements`.
   * 	- `stackElement`: Each element in the list represents a stack trace element, which
   * has the following attributes:
   * 	+ `file`: The file name of the source code that caused the exception.
   * 	+ `line`: The line number where the exception was thrown within the file.
   * 	+ `symbol`: The method name that was executing when the exception was thrown.
   * 	+ `class_name`: The fully qualified class name of the class that threw the exception.
   */
  private void assertBacktracesMatch(StackTraceElement[] myLittleStackTrace,
      List<SquashBacktrace.StackElement> stackElements) {
    for (int i = 0, stackElementsSize = stackElements.size(); i < stackElementsSize; i++) {
      SquashBacktrace.StackElement stackElement = stackElements.get(i);
      StackTraceElement expected = myLittleStackTrace[i];
      assertThat(stackElement.file).isEqualTo(expected.getFileName());
      assertThat(stackElement.line).isEqualTo(expected.getLineNumber());
      assertThat(stackElement.symbol).isEqualTo(expected.getMethodName());
      assertThat(stackElement.class_name).isEqualTo(expected.getClassName());
    }
  }

  /**
   * tests that a nested exception is properly serialized and deserialized with its
   * stack trace and cause.
   */
  @Test public void testNestedExceptions() throws Exception {
    final String logMessage = "I LOVE TACOS";
    final Throwable nestedException = mock(Throwable.class);
    final Throwable doublyNestedException = mock(Throwable.class);
    final Throwable exception = mock(Throwable.class);
    StackTraceElement n0 = new StackTraceElement("com.taco.Burrito", "digest",
        "core-android/src/com/burrito/Burrito.java", 45);
    StackTraceElement n1 = new StackTraceElement("com.taco.Burrito", "eat",
        "core-android/src/com/burrito/Burrito.java", 10);
    StackTraceElement n2 =
        new StackTraceElement("com.taco.Dude", "purchase", "core-android/src/com/taco/Dude.java",
            65);
    StackTraceElement[] nestedStackTrace = new StackTraceElement[] {n0, n1, n2};
    StackTraceElement z0 =
        new StackTraceElement("com.taco.Dude", "wheresmycar", "core-android/src/com/taco/Dude.java",
            455);
    StackTraceElement z1 =
        new StackTraceElement("com.bro.Bro", "hollerback", "core-android/src/com/bro/Bro.java",
            105);
    StackTraceElement z2 =
        new StackTraceElement("com.taco.Dude", "holler", "core-android/src/com/taco/Dude.java",
            655);
    StackTraceElement[] doublyNestedStackTrace = new StackTraceElement[] {z0, z1, z2};
    StackTraceElement s0 =
        new StackTraceElement("com.taco.Taco", "digest", "core-android/src/com/taco/Taco.java", 50);
    StackTraceElement s1 =
        new StackTraceElement("com.taco.Taco", "eat", "core-android/src/com/taco/Taco.java", 80);
    StackTraceElement s2 =
        new StackTraceElement("com.taco.Dude", "purchase", "core-android/src/com/taco/Dude.java",
            112);
    StackTraceElement[] myLittleStackTrace = new StackTraceElement[] {s0, s1, s2};
    final String message = "ExceptionMessage";
    when(exception.getMessage()).thenReturn(message);
    when(exception.getStackTrace()).thenReturn(myLittleStackTrace);
    when(exception.getCause()).thenReturn(nestedException);

    final String nestedExceptionMessage = "NestedExceptionMessage";
    when(nestedException.getMessage()).thenReturn(nestedExceptionMessage);
    when(nestedException.getStackTrace()).thenReturn(nestedStackTrace);
    when(nestedException.getCause()).thenReturn(doublyNestedException);

    final String doublyNestedExceptionMessage = "DoublyNestedExceptionMessage";
    when(doublyNestedException.getMessage()).thenReturn(doublyNestedExceptionMessage);
    when(doublyNestedException.getStackTrace()).thenReturn(doublyNestedStackTrace);

    final SquashEntry logEntry = factory.create(logMessage, exception);
    SquashEntry deserialized = serializeAndDeserialize(logEntry);
    assertThat(deserialized.backtraces).isNotEmpty();
    SquashBacktrace.SquashException backtrace = deserialized.backtraces.get(0);
    assertThat(backtrace.name).isEqualTo(Thread.currentThread().getName());
    assertThat(backtrace.faulted).isEqualTo(true);
    List<SquashBacktrace.StackElement> stackElements = backtrace.backtrace;
    assertBacktracesMatch(myLittleStackTrace, stackElements);
    assertThat(deserialized.ivars).isEmpty();
    assertThat(deserialized.log_message).isEqualTo(logMessage);
    assertThat(deserialized.message).isEqualTo(message);
    final List<SquashBacktrace.NestedException> nestedExceptions = deserialized.parent_exceptions;
    assertThat(nestedExceptions).hasSize(2);

    final SquashBacktrace.NestedException nested1 = nestedExceptions.get(0);
    assertThat(nested1.class_name).isEqualTo(nestedException.getClass().getName());
    assertThat(nested1.ivars).isEmpty();
    assertThat(nested1.message).isEqualTo(nestedExceptionMessage);
    backtrace = nested1.backtraces.get(0);
    assertThat(backtrace.name).isEqualTo(Thread.currentThread().getName());
    assertThat(backtrace.faulted).isEqualTo(true);
    assertBacktracesMatch(nestedStackTrace, backtrace.backtrace);

    final SquashBacktrace.NestedException nested2 = nestedExceptions.get(1);
    assertThat(nested2.class_name).isEqualTo(doublyNestedException.getClass().getName());
    assertThat(nested2.ivars).isEmpty();
    assertThat(nested2.message).isEqualTo(doublyNestedExceptionMessage);
    backtrace = nested1.backtraces.get(0);
    assertThat(backtrace.name).isEqualTo(Thread.currentThread().getName());
    assertThat(backtrace.faulted).isEqualTo(true);
    assertBacktracesMatch(nestedStackTrace, backtrace.backtrace);
  }

  /**
   * tests the serialization and deserialization of exceptions with infinitely nested
   * exceptions and doubly nested exceptions.
   */
  @Test public void testInfinitelyNestedExceptions() throws Exception {
    final String logMessage = "I LOVE TACOS";
    final Throwable nestedException = mock(Throwable.class);
    final Throwable doublyNestedException = mock(Throwable.class);
    final Throwable exception = mock(Throwable.class);
    StackTraceElement n0 = new StackTraceElement("com.taco.Burrito", "digest",
        "core-android/src/com/burrito/Burrito.java", 45);
    StackTraceElement n1 = new StackTraceElement("com.taco.Burrito", "eat",
        "core-android/src/com/burrito/Burrito.java", 10);
    StackTraceElement n2 =
        new StackTraceElement("com.taco.Dude", "purchase", "core-android/src/com/taco/Dude.java",
            65);
    StackTraceElement[] nestedStackTrace = new StackTraceElement[] {n0, n1, n2};
    StackTraceElement z0 =
        new StackTraceElement("com.taco.Dude", "wheresmycar", "core-android/src/com/taco/Dude.java",
            455);
    StackTraceElement z1 =
        new StackTraceElement("com.bro.Bro", "hollerback", "core-android/src/com/bro/Bro.java",
            105);
    StackTraceElement z2 =
        new StackTraceElement("com.taco.Dude", "holler", "core-android/src/com/taco/Dude.java",
            655);
    StackTraceElement[] doublyNestedStackTrace = new StackTraceElement[] {z0, z1, z2};
    StackTraceElement s0 =
        new StackTraceElement("com.taco.Taco", "digest", "core-android/src/com/taco/Taco.java", 50);
    StackTraceElement s1 =
        new StackTraceElement("com.taco.Taco", "eat", "core-android/src/com/taco/Taco.java", 80);
    StackTraceElement s2 =
        new StackTraceElement("com.taco.Dude", "purchase", "core-android/src/com/taco/Dude.java",
            112);
    StackTraceElement[] myLittleStackTrace = new StackTraceElement[] {s0, s1, s2};
    final String message = "ExceptionMessage";
    when(exception.getMessage()).thenReturn(message);
    when(exception.getStackTrace()).thenReturn(myLittleStackTrace);
    when(exception.getCause()).thenReturn(nestedException);

    final String nestedExceptionMessage = "NestedExceptionMessage";
    when(nestedException.getMessage()).thenReturn(nestedExceptionMessage);
    when(nestedException.getStackTrace()).thenReturn(nestedStackTrace);
    when(nestedException.getCause()).thenReturn(doublyNestedException);

    final String doublyNestedExceptionMessage = "DoublyNestedExceptionMessage";
    when(doublyNestedException.getMessage()).thenReturn(doublyNestedExceptionMessage);
    when(doublyNestedException.getStackTrace()).thenReturn(doublyNestedStackTrace);
    when(doublyNestedException.getCause()).thenReturn(doublyNestedException);

    final SquashEntry logEntry = factory.create(logMessage, exception);
    SquashEntry deserialized = serializeAndDeserialize(logEntry);
    assertThat(deserialized.backtraces).isNotEmpty();
    SquashBacktrace.SquashException backtrace = deserialized.backtraces.get(0);
    assertThat(backtrace.name).isEqualTo(Thread.currentThread().getName());
    assertThat(backtrace.faulted).isEqualTo(true);
    List<SquashBacktrace.StackElement> stackElements = backtrace.backtrace;
    assertBacktracesMatch(myLittleStackTrace, stackElements);
    assertThat(deserialized.ivars).isEmpty();
    assertThat(deserialized.log_message).isEqualTo(logMessage);
    assertThat(deserialized.message).isEqualTo(message);
    final List<SquashBacktrace.NestedException> nestedExceptions = deserialized.parent_exceptions;
    assertThat(nestedExceptions).hasSize(2);

    final SquashBacktrace.NestedException nested1 = nestedExceptions.get(0);
    assertThat(nested1.class_name).isEqualTo(nestedException.getClass().getName());
    assertThat(nested1.ivars).isEmpty();
    assertThat(nested1.message).isEqualTo(nestedExceptionMessage);
    backtrace = nested1.backtraces.get(0);
    assertThat(backtrace.name).isEqualTo(Thread.currentThread().getName());
    assertThat(backtrace.faulted).isEqualTo(true);
    assertBacktracesMatch(nestedStackTrace, backtrace.backtrace);

    final SquashBacktrace.NestedException nested2 = nestedExceptions.get(1);
    assertThat(nested2.class_name).isEqualTo(doublyNestedException.getClass().getName());
    assertThat(nested2.ivars).isEmpty();
    assertThat(nested2.message).isEqualTo(doublyNestedExceptionMessage);
    backtrace = nested1.backtraces.get(0);
    assertThat(backtrace.name).isEqualTo(Thread.currentThread().getName());
    assertThat(backtrace.faulted).isEqualTo(true);
    assertBacktracesMatch(nestedStackTrace, backtrace.backtrace);
  }

  /**
   * is an Android library that creates instances of SquashEntry, which are used to
   * represent events in a log. The class takes a log message and a Throwable exception
   * as input and returns a complete SquashEntry object with various attributes such
   * as client name, API key, log message, exception, app version, etc.
   */
  private class EntryFactory {
    /**
     * creates a new instance of `SquashEntry`, setting various fields such as API key,
     * log message, exception, app version, and device ID.
     * 
     * @param logMessage message to be logged in the SquashEntry object created by the function.
     * 
     * @param exception throwable object that occurred during the API call, providing
     * additional context for error handling and logging purposes.
     * 
     * 	- `logMessage`: The log message associated with the exception.
     * 	- `exception`: A Throwable object containing information about the exception,
     * such as its cause and stack trace.
     * 	- `appVersion`: The version number of the application that triggered the error.
     * 	- `sha`: The SHA hash of the application's binary code.
     * 	- `deviceId`: The ID of the device that triggered the error.
     * 	- `endpoint`: The endpoint that was used to trigger the error.
     * 	- `userId`: The user ID associated with the error.
     * 	- `Debug`: A boolean indicating whether the error is in debug mode or not.
     * 
     * @returns a `SquashEntry` object containing various information about the log message
     * and exception.
     * 
     * 	- `SquashEntry`: The type of object created is specified as `SquashEntry`.
     * 	- `logMessage`: A string variable representing the log message associated with
     * this entry.
     * 	- `exception`: An instance of `Throwable` containing any exceptions encountered
     * during the creation process.
     * 	- `appVersion`: A string variable representing the application version associated
     * with this entry.
     * 	- `sha`: A string variable representing the SHA hash value for this entry.
     * 	- `deviceId`: A string variable representing the device ID associated with this
     * entry.
     * 	- `endpoint`: A string variable representing the endpoint associated with this entry.
     * 	- `userId`: A string variable representing the user ID associated with this entry.
     * 	- `Debug`: A boolean variable indicating whether this entry is for debugging
     * purposes or not.
     */
    public SquashEntry create(String logMessage, Throwable exception) {
      return new SquashEntry("testclient", "testAPIKey", logMessage, exception, "testAppVersion",
          42, "testSHA", "testDeviceId", "testEndpoint", "testUserId", "Debug");
    }
  }
}
