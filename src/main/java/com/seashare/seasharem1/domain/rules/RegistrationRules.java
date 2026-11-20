package com.seashare.seasharem1.domain.rules;

import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Reglas de validación del registro de una embarcación, con los mensajes exactos de la
 * especificación. Cada función devuelve el mensaje de error, o null si el valor es válido.
 */
public final class RegistrationRules {

  public static final String REQUIRED = "Este campo es obligatorio para continuar con el registro";

  /**
   * Mensaje de campo vacío en la edición (el del registro habla de "continuar con el registro").
   */
  public static final String EDIT_REQUIRED = "Este campo es obligatorio";

  public static final String NAME_INVALID =
      "El nombre debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede"
          + " incluir letras, números, espacios, tildes y guiones";
  public static final String PORT_NAME_INVALID =
      "El nombre del puerto debe tener entre 3 y 50 caracteres, contener al menos una letra y solo"
          + " puede incluir letras, números, espacios, tildes y guiones";
  public static final String LEGAL_REGISTRATION_INVALID = "Formato de matrícula legal inválido";
  public static final String LEGAL_REGISTRATION_TAKEN = "Matrícula existente en el sistema";
  public static final String TYPE_INVALID = "Tipo de embarcación inválido";
  public static final String NOT_NUMERIC = "Solo se aceptan valores numéricos";
  public static final String NO_DECIMALS = "Ingrese un número entero sin puntos ni comas";
  public static final String MUST_BE_POSITIVE = "Ingrese un número entero mayor que 0";
  public static final String CAPACITY_EXCEEDED =
      "Este valor excede la capacidad máxima de la embarcación";
  public static final String RATE_MUST_BE_POSITIVE = "La tarifa debe ser mayor que 0";
  public static final String RATE_TOO_HIGH = "La tarifa excede el valor permitido";
  public static final String COORDINATES_REQUIRED = "Las coordenadas son obligatorias";
  public static final String COORDINATES_INVALID = "Las coordenadas no son válidas";
  public static final String IMAGE_INVALID_FORMAT = "Formato de imagen inválido";
  public static final String IMAGE_TOO_LARGE = "El tamaño de imagen se excede";

  /** Tope técnico de la tarifa: lo que aguanta la columna DECIMAL(12,2). */
  public static final long MAX_BASE_RATE = 9_999_999_999L;

  /** 10 MB exactos: 10 × 1024 × 1024 bytes. */
  public static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

  private static final int MAX_RATE_DIGITS = 10;
  private static final int COORDINATE_DECIMALS = 4;
  private static final Pattern NAME = Pattern.compile("[\\p{L}0-9 -]+");
  private static final Pattern LETTER = Pattern.compile("\\p{L}");
  private static final Pattern LEGAL_REGISTRATION = Pattern.compile("CP-[0-9]{2}-[0-9]{4}-[A-Z]");
  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };

  private RegistrationRules() {}

  /** Quita los espacios del inicio y del final y unifica las tildes (forma NFC). */
  public static String normalize(String raw) {
    return raw == null ? null : Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
  }

  /**
   * Normalización de nombres (embarcación y puerto): como {@link #normalize}, y además convierte
   * cualquier espacio no estándar (tabulaciones, espacios no separables) en un espacio simple y
   * colapsa los espacios internos repetidos.
   */
  public static String normalizeName(String raw) {
    if (raw == null) {
      return null;
    }
    return Normalizer.normalize(raw, Normalizer.Form.NFC)
        .replaceAll("[\\s\\u00A0\\u2007\\u202F]+", " ")
        .strip();
  }

  /**
   * Nombre de embarcación o de puerto ya normalizado: 3 a 50 caracteres, con al menos una letra.
   */
  public static String nameError(String name, String invalidMessage) {
    if (name == null || name.isEmpty()) {
      return REQUIRED;
    }
    boolean validLength = name.length() >= 3 && name.length() <= 50;
    if (!validLength || !NAME.matcher(name).matches() || !LETTER.matcher(name).find()) {
      return invalidMessage;
    }
    return null;
  }

  /** Matrícula ya normalizada, con el formato CP-NN-NNNN-L. */
  public static String legalRegistrationError(String legalRegistration) {
    if (legalRegistration == null || legalRegistration.isEmpty()) {
      return REQUIRED;
    }
    return LEGAL_REGISTRATION.matcher(legalRegistration).matches()
        ? null
        : LEGAL_REGISTRATION_INVALID;
  }

  /** Tipo recibido como texto (por ejemplo "YACHT"), o null si no existe. */
  public static VesselType parseType(String code) {
    if (code == null) {
      return null;
    }
    try {
      return VesselType.valueOf(code.strip());
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  public static String typeError(String code) {
    if (code == null || code.isBlank()) {
      return REQUIRED;
    }
    return parseType(code) == null ? TYPE_INVALID : null;
  }

  /** Capacidad entera mayor que 0 que no supere el máximo del tipo (si ya hay tipo). */
  public static String capacityError(Integer capacity, VesselType type) {
    if (capacity == null) {
      return REQUIRED;
    }
    if (capacity < 0) {
      return NO_DECIMALS;
    }
    if (capacity == 0) {
      return MUST_BE_POSITIVE;
    }
    if (type != null && !type.allows(capacity)) {
      return CAPACITY_EXCEEDED;
    }
    return null;
  }

  /**
   * Capacidad recibida como texto, con la cascada de la edición: obligatoria, solo dígitos, entero
   * mayor que 0 (los ceros a la izquierda se ignoran: "007" es 7) y sin superar el máximo del tipo.
   * Un valor de 100 o más también cuenta como superar el máximo.
   */
  public static Parsed<Integer> parseCapacity(String raw, VesselType type) {
    if (raw == null || raw.isBlank()) {
      return Parsed.error(REQUIRED);
    }
    String text = raw.strip();
    if (!text.chars().allMatch(c -> c >= '0' && c <= '9')) {
      boolean punctuation =
          text.chars().anyMatch(c -> c == '.' || c == ',' || c == '+' || c == '-');
      return Parsed.error(punctuation ? NO_DECIMALS : NOT_NUMERIC);
    }
    String digits = text.replaceFirst("^0+", "");
    if (digits.isEmpty()) {
      return Parsed.error(MUST_BE_POSITIVE);
    }
    if (digits.length() > 2) {
      return Parsed.error(CAPACITY_EXCEEDED);
    }
    int value = Integer.parseInt(digits);
    return type != null && !type.allows(value) ? Parsed.error(CAPACITY_EXCEEDED) : Parsed.of(value);
  }

  /** Tarifa recibida como texto: entero mayor que 0, sin separadores y hasta el tope técnico. */
  public static Parsed<Long> parseBaseRate(String raw) {
    if (raw == null || raw.isBlank()) {
      return Parsed.error(REQUIRED);
    }
    String text = raw.strip();
    if (!text.chars().allMatch(c -> c >= '0' && c <= '9')) {
      boolean punctuation =
          text.chars().anyMatch(c -> c == '.' || c == ',' || c == '+' || c == '-');
      return Parsed.error(punctuation ? NO_DECIMALS : NOT_NUMERIC);
    }
    String digits = text.replaceFirst("^0+", "");
    if (digits.isEmpty()) {
      return Parsed.error(RATE_MUST_BE_POSITIVE);
    }
    if (digits.length() > MAX_RATE_DIGITS) {
      return Parsed.error(RATE_TOO_HIGH);
    }
    long value = Long.parseLong(digits);
    return value > MAX_BASE_RATE ? Parsed.error(RATE_TOO_HIGH) : Parsed.of(value);
  }

  /** Latitud (límite 90) o longitud (límite 180) con signo, redondeada a 4 decimales. */
  public static Parsed<BigDecimal> parseCoordinate(String raw, int limit) {
    if (raw == null || raw.isBlank()) {
      return Parsed.error(COORDINATES_REQUIRED);
    }
    BigDecimal value;
    try {
      value = new BigDecimal(raw.strip());
    } catch (NumberFormatException exception) {
      return Parsed.error(COORDINATES_INVALID);
    }
    if (value.abs().compareTo(BigDecimal.valueOf(limit)) > 0) {
      return Parsed.error(COORDINATES_INVALID);
    }
    return Parsed.of(value.setScale(COORDINATE_DECIMALS, RoundingMode.HALF_UP));
  }

  /** Tipo real de la imagen según su contenido (no su extensión): PNG o JPEG, o null. */
  public static String detectImageType(byte[] content) {
    if (content == null) {
      return null;
    }
    if (content.length >= PNG_SIGNATURE.length && startsWith(content, PNG_SIGNATURE)) {
      return "image/png";
    }
    boolean jpeg =
        content.length >= 3
            && (content[0] & 0xFF) == 0xFF
            && (content[1] & 0xFF) == 0xD8
            && (content[2] & 0xFF) == 0xFF;
    return jpeg ? "image/jpeg" : null;
  }

  /**
   * Foto obligatoria, PNG o JPEG real, de máximo 10 MB. El formato se revisa antes que el tamaño.
   */
  public static String imageError(byte[] content) {
    if (content == null || content.length == 0) {
      return REQUIRED;
    }
    if (detectImageType(content) == null) {
      return IMAGE_INVALID_FORMAT;
    }
    return content.length > MAX_IMAGE_BYTES ? IMAGE_TOO_LARGE : null;
  }

  private static boolean startsWith(byte[] content, byte[] prefix) {
    for (int i = 0; i < prefix.length; i++) {
      if (content[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
