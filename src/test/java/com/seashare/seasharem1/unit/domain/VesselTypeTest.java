package com.seashare.seasharem1.unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.domain.model.VesselType;
import org.junit.jupiter.api.Test;

class VesselTypeTest {

  @Test
  void eachTypeHasItsMaxCapacity() {
    assertEquals(10, VesselType.MOTORBOAT.maxCapacity());
    assertEquals(20, VesselType.YACHT.maxCapacity());
    assertEquals(30, VesselType.CATAMARAN.maxCapacity());
    assertEquals(12, VesselType.SAILBOAT.maxCapacity());
  }

  @Test
  void allowsCapacitiesFromOneUpToTheMaximum() {
    assertTrue(VesselType.YACHT.allows(1));
    assertTrue(VesselType.YACHT.allows(20));
    assertTrue(VesselType.SAILBOAT.allows(12));
  }

  @Test
  void rejectsCapacitiesOutOfRange() {
    assertFalse(VesselType.YACHT.allows(0));
    assertFalse(VesselType.YACHT.allows(21));
    assertFalse(VesselType.MOTORBOAT.allows(11));
    assertFalse(VesselType.CATAMARAN.allows(-5));
  }
}
