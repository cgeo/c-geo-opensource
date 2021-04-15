package cgeo.geocaching.calculator;

import cgeo.geocaching.models.CalcState;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FormulaParserTest {

    @Test
    public void testParseLatitude() {
        final FormulaParser formulaParser = new FormulaParser();
        final String parsedLatitudeNorth = formulaParser.parseLatitude("N AB° CD.EFG");
        assertThat(parsedLatitudeNorth).isNotNull();

        final String parsedLatitudeSouth = formulaParser.parseLatitude("S AB° CD.EFG");
        assertThat(parsedLatitudeSouth).isNotNull();
    }

    @Test
    public void testParseLongitude() {
        final FormulaParser formulaParser = new FormulaParser();
        final String parsedLongitudeWest = formulaParser.parseLongitude("W A° BC.DEF");
        assertThat(parsedLongitudeWest).isNotNull();


        final String parsedLongitudeEast = formulaParser.parseLongitude("E A° BC.DEF");
        assertThat(parsedLongitudeEast).isNotNull();

        final String parsedLongitudeOst = formulaParser.parseLongitude("O A° BC.DEF");
        assertThat(parsedLongitudeOst).isNotNull();
    }

    @Test
    public void testParseFullCoordinatesDirections() {
        final FormulaParser formulaParser = new FormulaParser();

        final ImmutablePair<String, String> parsedFullCoordinatesNorthEast = formulaParser.parse("N AB° CD.EFG  E H° IJ.KLM");
        assertThat(parsedFullCoordinatesNorthEast).isNotNull();

        final ImmutablePair<String, String> parsedFullCoordinatesSouthWest = formulaParser.parse("S AB° CD.EFG  W H° IJ.KLM");
        assertThat(parsedFullCoordinatesSouthWest).isNotNull();
    }

    @Test
    public void testParseFullCoordinates() {
        final FormulaParser formulaParser = new FormulaParser();
        final ImmutablePair<String, String> parsedFullCoordinates = formulaParser.parse("N 49° AB.031  E 8° 38.DEF");
        assertThat(parsedFullCoordinates).isNotNull();
        final String parsedLatitude = parsedFullCoordinates.left;
        final String parsedLongitude = parsedFullCoordinates.right;
        assertThat(parsedLatitude).isNotNull();
        assertThat(parsedLongitude).isNotNull();

        final List<VariableData> variables = new ArrayList<>();
        final CalcState calcState = CoordinatesCalculateUtils.createCalcState(parsedLatitude, parsedLongitude, variables);
        assertThat(calcState.plainLat).isEqualTo("N 49° AB.031'");
        assertThat(calcState.plainLon).isEqualTo("E 8° 38.DEF'");
    }

    @Test
    public void testParseFullCoordinatesWithFormula() {
        final FormulaParser formulaParser = new FormulaParser();
        final ImmutablePair<String, String> parsedFullCoordinates = formulaParser.parse("(FORMULA-PLAIN) N  AB° 48.[B+C-A]^2  E (B%C)°  38.(D+F)*2 | a = 2) test");
        assertThat(parsedFullCoordinates).isNotNull();
        final String parsedLatitude = parsedFullCoordinates.left;
        final String parsedLongitude = parsedFullCoordinates.right;
        assertThat(parsedLatitude).isNotNull();
        assertThat(parsedLongitude).isNotNull();

        final List<VariableData> variables = new ArrayList<>();
        final CalcState calcState = CoordinatesCalculateUtils.createCalcState(parsedLatitude, parsedLongitude, variables);
        assertThat(calcState.plainLat).isEqualTo("N AB° 48.[B+C-A]^2'");
        assertThat(calcState.plainLon).isEqualTo("E (B%C)° 38.(D+F)*2'");
    }

    @Test
    public void testParseFullCoordinatesWithIncompleteFormula() {
        final FormulaParser formulaParser = new FormulaParser();
        final ImmutablePair<String, String> parsedFullCoordinates = formulaParser.parse("(FORMULA-PLAIN) N  AB° 48.B+C-A^2  E (B%C)°  38.(D+F)^2 | a = 2) test");
        assertThat(parsedFullCoordinates).isNotNull();
        final String parsedLatitude = parsedFullCoordinates.left;
        final String parsedLongitude = parsedFullCoordinates.right;
        assertThat(parsedLatitude).isNotNull();
        assertThat(parsedLongitude).isNotNull();

        final List<VariableData> variables = new ArrayList<>();
        final CalcState calcState = CoordinatesCalculateUtils.createCalcState(parsedLatitude, parsedLongitude, variables);
        assertThat(calcState.plainLat).isEqualTo("N AB° 48.B+C-A^2'");
        assertThat(calcState.plainLon).isEqualTo("E (B%C)° 38.(D+F)^2'");
    }

    @Test
    public void testParseFullCoordinatesWithNoValidFormula() {
        try {
            final FormulaParser formulaParser = new FormulaParser();
            formulaParser.parse("(FORMULA-PLAIN) N  AB° 48.[B+C-A^2  E (B%C)°  38!.(D+F)2 | a = 2) test");
            failBecauseExceptionWasNotThrown(FormulaParser.ParseException.class);
        } catch (final FormulaParser.ParseException e) {
            // expected
        }
    }

    @Test
    public void testParseFullCoordinatesException() {
        try {
            final FormulaParser formulaParser = new FormulaParser();
            formulaParser.parse("N 49° AB.031 | E 8° 38.DEF");
            failBecauseExceptionWasNotThrown(FormulaParser.ParseException.class);
        } catch (final FormulaParser.ParseException e) {
            // expected
        }
    }
}
