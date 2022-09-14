package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Dot", "bryan.etzine@ufl.edu", true),
                Arguments.of("Underscore", "bryan_etzine@ufl.edu", true),
                Arguments.of("Capitalized", "BRYAN.ETZINE@ufl.edu", true),
                Arguments.of("Wacky Allowed Email", "its_a_trap@~.ack", true),
                Arguments.of("Chained Dots", "bryan.etzine@ufl.is.good.at.football.edu", true),
                Arguments.of("Broken Chained Dots", "bryan.etzine@ufl.is.good.at!football.edu", false),
                Arguments.of("Hyphenated", "bryan-etzine@ufl.edu", false),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Invalid domain", "notreal@jobs.co", false),
                Arguments.of("Too Short", "Q@gmail.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("11 Whitespace Characters", "           ", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("15 Characters", "i<3pancakes15!!", true),
                Arguments.of("17 Characters", "i<3pancakes17!!!!", true),
                Arguments.of("19 Characters", "i<3pancakes19!!!!!!", true),
                Arguments.of("9 Characters", "character", false),
                Arguments.of("10 Characters", "automobile", false),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("20 Characters", "i<3pancakes20!!!!!!!", false),
                Arguments.of("21 Characters", "i<3pancakes21!!!!!!!!", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Empty List", "[]", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Different Spacing", "['a','b', 'c']", true),
                Arguments.of("Multiple Non-alphabetical Elements", "['3','$','!']", true),
                Arguments.of("Whitespace Element", "[' ']", true),
                Arguments.of("Empty Element", "['']", false),
                Arguments.of("Newline Element", "['\n']", false),
                Arguments.of("Missing Element", "['a','b',]", false),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Missing Single Quotes", "[a,b,c]", false),
                Arguments.of("Too many characters", "['aa', 'bb', 'cc']", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("Many Decimal Places", "10100.001", true),
                Arguments.of("Negative Decimal Number", "-1.0", true),
                Arguments.of("Negative Many Decimal Places", "-10100.001", true),
                Arguments.of("Pre Decimal Zero", "0.5", true),
                Arguments.of("Zero", "0.0", true),
                Arguments.of("Missing Decimal", "1", false),
                Arguments.of("Missing Digits", ".", false),
                Arguments.of("Missing Post Decimal Digit", "1.", false),
                Arguments.of("Missing Pre Decimal Digit", ".5", false),
                Arguments.of("Leading Zero", "01.5", false)
        ); //TODO
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success); //TODO
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Empty Quotes", "\"\"", true),
                Arguments.of("Backslash and Quotes", "\"\\\"\"", true),
                Arguments.of("Quote", "\"Hello, World!\"", true),
                Arguments.of("Escape Backslash", "\"1\\t2\"", true),
                Arguments.of("Whitespace Characters", "\"\b\n\r\t\'\"", true),
                Arguments.of("Symbols", "\"!@#$%^&*()_=+\"", true),
                Arguments.of("Empty String", "", false),
                Arguments.of("Unquoted String", "Hello, World!", false),
                Arguments.of("Unmatched Quote", "\"unterminated", false),
                Arguments.of("Invalid Escaping", "\"invalid\\escape\"", false),
                Arguments.of("Incorrect Quotes", "'Hello, World!'", false)
        ); //TODO
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
