package de.l3s.learnweb.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ActionTest {

    @Test
    void idsAreUniqueAndNotRetired() {
        assertEquals(Action.values().length, Arrays.stream(Action.values()).mapToInt(Action::getId).distinct().count());
        for (Action action : Action.values()) {
            assertFalse(Action.RETIRED_IDS.contains(action.getId()), action + " reuses a retired id");
            assertEquals(action, Action.findByIdOrElseThrow(action.getId()));
        }
    }

    @Test
    void retiredIdsAreUnknown() {
        Action.RETIRED_IDS.forEach(id -> assertTrue(Action.findById(id).isEmpty()));
        assertThrows(IllegalArgumentException.class, () -> Action.findByIdOrElseThrow(4));
    }
}
