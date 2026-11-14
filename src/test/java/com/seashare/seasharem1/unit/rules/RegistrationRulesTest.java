package com.seashare.seasharem1.unit.rules;

import static com.seashare.seasharem1.domain.rules.RegistrationRules.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.domain.model.VesselType;
import com.seashare.seasharem1.domain.rules.Parsed;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RegistrationRulesTest {

  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

  @Test
  void normalizeTrimsAndUnifiesAccents() {
    assertEquals("Río", normalize("  Río  "));
    assertEquals("Río", normalize("Ri\u0301o"));
    assertNull(normalize(null));
  }

  @Test
  void nameAcceptsLettersDigitsSpacesAccentsAndHyphens() {
    assertNull(nameError("Río Magdalena II", NAME_INVALID));
    assertNull(nameError("Río Magdalena 2", NAME_INVALID));
    assertNull(nameError("Peñón del Norte", NAME_INVALID));
    assertNull(nameError("Ana-1", NAME_INVALID));
    assertNull(nameError("abc", NAME_INVALID));
    assertNull(nameError("a".repeat(50), NAME_INVALID));
  }

  @Test
  void nameRejectsBadLengthNoLetterOrForbiddenCharacters() {
    assertEquals(NAME_INVALID, nameError("ab", NAME_INVALID));
    assertEquals(NAME_INVALID, nameError("a".repeat(51), NAME_INVALID));
    assertEquals(NAME_INVALID, nameError("123", NAME_INVALID));
    assertEquals(NAME_INVALID, nameError("---", NAME_INVALID));
    assertEquals(NAME_INVALID, nameError("Bote@", NAME_INVALID));
    assertEquals(PORT_NAME_INVALID, nameError("12", PORT_NAME_INVALID));
  }

  @Test
  void emptyNameIsRequired() {
    assertEquals(REQUIRED, nameError(null, NAME_INVALID));
    assertEquals(REQUIRED, nameError("", NAME_INVALID));
  }

  @Test
  void legalRegistrationNeedsTheExactFormat() {
    assertNull(legalRegistrationError("CP-05-1042-A"));
    assertEquals(LEGAL_REGISTRATION_INVALID, legalRegistrationError("CP-5-1042-A"));
    assertEquals(LEGAL_REGISTRATION_INVALID, legalRegistrationError("cp-05-1042-a"));
    assertEquals(LEGAL_REGISTRATION_INVALID, legalRegistrationError("CP-05-1042-AB"));
    assertEquals(REQUIRED, legalRegistrationError(""));
    assertEquals(REQUIRED, legalRegistrationError(null));
  }

  @Test
  void typeIsParsedFromItsCode() {
    assertEquals(VesselType.YACHT, parseType("YACHT"));
    assertEquals(VesselType.YACHT, parseType(" YACHT "));
    assertNull(parseType("BARGE"));
    assertNull(parseType(null));
    assertNull(typeError("SAILBOAT"));
    assertEquals(REQUIRED, typeError(" "));
    assertEquals(REQUIRED, typeError(null));
    assertEquals(TYPE_INVALID, typeError("BARGE"));
  }

  @Test
  void capacityMustBePositiveIntegerWithinTheTypeLimit() {
    assertEquals(REQUIRED, capacityError(null, VesselType.MOTORBOAT));
    assertEquals(NO_DECIMALS, capacityError(-3, VesselType.MOTORBOAT));
    assertEquals(MUST_BE_POSITIVE, capacityError(0, VesselType.MOTORBOAT));
    assertEquals(CAPACITY_EXCEEDED, capacityError(11, VesselType.MOTORBOAT));
    assertNull(capacityError(10, VesselType.MOTORBOAT));
    assertNull(capacityError(30, VesselType.CATAMARAN));
    assertEquals(CAPACITY_EXCEEDED, capacityError(31, VesselType.CATAMARAN));
    assertNull(capacityError(999, null));
  }

  @Test
  void baseRateAcceptsWholePositiveNumbersAndIgnoresLeadingZeros() {
    assertEquals(180000L, parseBaseRate("180000").value().longValue());
    assertEquals(180000L, parseBaseRate("0180000").value().longValue());
    assertEquals(1L, parseBaseRate("00000000000000001").value().longValue());
    assertEquals(9_999_999_999L, parseBaseRate("9999999999").value().longValue());
  }

  @Test
  void baseRateGivesTheSpecMessages() {
    assertEquals(REQUIRED, parseBaseRate("").error());
    assertEquals(REQUIRED, parseBaseRate(null).error());
    assertEquals(NOT_NUMERIC, parseBaseRate("abc").error());
    assertEquals(NO_DECIMALS, parseBaseRate("12.5").error());
    assertEquals(NO_DECIMALS, parseBaseRate("1,5").error());
    assertEquals(NO_DECIMALS, parseBaseRate("-5").error());
    assertEquals(RATE_MUST_BE_POSITIVE, parseBaseRate("0").error());
    assertEquals(RATE_MUST_BE_POSITIVE, parseBaseRate("000").error());
    assertEquals(RATE_TOO_HIGH, parseBaseRate("10000000000").error());
    assertEquals(RATE_TOO_HIGH, parseBaseRate("99999999999999").error());
  }

  @Test
  void coordinatesAreRoundedToFourDecimals() {
    assertEquals(new BigDecimal("11.2408"), parseCoordinate("11.24083", 90).value());
    assertEquals(new BigDecimal("11.2409"), parseCoordinate("11.24085", 90).value());
    assertEquals(new BigDecimal("-74.1990"), parseCoordinate("-74.199", 180).value());
    assertEquals(new BigDecimal("90.0000"), parseCoordinate("90", 90).value());
  }

  @Test
  void coordinatesRejectMissingNonNumericOrOutOfRange() {
    assertEquals(COORDINATES_REQUIRED, parseCoordinate("", 90).error());
    assertEquals(COORDINATES_REQUIRED, parseCoordinate(null, 90).error());
    assertEquals(COORDINATES_INVALID, parseCoordinate("abc", 90).error());
    assertEquals(COORDINATES_INVALID, parseCoordinate("91", 90).error());
    assertEquals(COORDINATES_INVALID, parseCoordinate("181", 180).error());
  }

  @Test
  void imageTypeIsDetectedFromContent() {
    assertEquals("image/png", detectImageType(PNG));
    assertEquals("image/jpeg", detectImageType(JPEG));
    assertNull(detectImageType("hola".getBytes()));
    assertNull(detectImageType(new byte[] {1}));
    assertNull(detectImageType(null));
  }

  @Test
  void imageMustBePresentRealAndWithinTenMegabytes() {
    assertEquals(REQUIRED, imageError(null));
    assertEquals(REQUIRED, imageError(new byte[0]));
    assertEquals(IMAGE_INVALID_FORMAT, imageError("hola".getBytes()));
    assertNull(imageError(PNG));
    assertNull(imageError(JPEG));
  }

  @Test
  void imageSizeLimitIsExactlyTenMebibytes() {
    byte[] exact = new byte[MAX_IMAGE_BYTES];
    System.arraycopy(PNG, 0, exact, 0, PNG.length);
    byte[] over = new byte[MAX_IMAGE_BYTES + 1];
    System.arraycopy(PNG, 0, over, 0, PNG.length);
    byte[] overNotImage = new byte[MAX_IMAGE_BYTES + 1];

    assertNull(imageError(exact));
    assertEquals(IMAGE_TOO_LARGE, imageError(over));
    assertEquals(IMAGE_INVALID_FORMAT, imageError(overNotImage));
  }

  @Test
  void parsedExposesValueOrError() {
    assertFalse(Parsed.of(1).isError());
    assertTrue(Parsed.error("x").isError());
  }

  @Test
  void normalizeNameCollapsesInnerAndNonStandardSpaces() {
    assertEquals("Río Magdalena II", normalizeName("  Río \t  Magdalena\u00A0\u00A0II  "));
    assertEquals("Río", normalizeName("Ri\u0301o"));
    assertNull(normalizeName(null));
  }

  @Test
  void parseCapacityFollowsTheEditCascade() {
    VesselType motorboat = VesselType.MOTORBOAT;
    assertEquals(REQUIRED, parseCapacity(null, motorboat).error());
    assertEquals(REQUIRED, parseCapacity("  ", motorboat).error());
    assertEquals(NOT_NUMERIC, parseCapacity("12a", motorboat).error());
    assertEquals(NOT_NUMERIC, parseCapacity("1 2", motorboat).error());
    assertEquals(NO_DECIMALS, parseCapacity("1.5", motorboat).error());
    assertEquals(NO_DECIMALS, parseCapacity("-5", motorboat).error());
    assertEquals(MUST_BE_POSITIVE, parseCapacity("0", motorboat).error());
    assertEquals(MUST_BE_POSITIVE, parseCapacity("000", motorboat).error());
    assertEquals(CAPACITY_EXCEEDED, parseCapacity("11", motorboat).error());
    assertEquals(CAPACITY_EXCEEDED, parseCapacity("100", motorboat).error());
    assertEquals(CAPACITY_EXCEEDED, parseCapacity("99999999999", motorboat).error());
    assertEquals(7, parseCapacity("007", motorboat).value().intValue());
    assertEquals(10, parseCapacity("10", motorboat).value().intValue());
    assertEquals(30, parseCapacity("30", VesselType.CATAMARAN).value().intValue());
    assertEquals(50, parseCapacity("50", null).value().intValue());
  }
}
