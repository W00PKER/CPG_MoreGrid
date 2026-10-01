package com.feb.moregrid.component.rotary;

import java.util.ArrayList;
import java.util.List;

import static com.feb.moregrid.component.rotary.KnobDirection.*;

public enum RotaryLayout {
    ONE_TWO(on(TOP_LEFT), on(TOP_RIGHT)),
    ONE_ZERO_TWO(on(TOP_LEFT), off(TOP), on(TOP_RIGHT)),
    ONE_TO_THREE(on(TOP_LEFT), on(TOP), on(TOP_RIGHT)),
    ONE_TO_FOUR(on(LEFT), on(TOP_LEFT), on(TOP), on(TOP_RIGHT)),
    ONE_TO_FIVE(on(LEFT), on(TOP_LEFT), on(TOP), on(TOP_RIGHT), on(RIGHT)),
    ONE_TO_SIX(on(BOTTOM_LEFT), on(LEFT), on(TOP_LEFT), on(TOP), on(TOP_RIGHT), on(RIGHT)),
    ONE_TO_SEVEN(on(BOTTOM_LEFT), on(LEFT), on(TOP_LEFT), on(TOP), on(TOP_RIGHT), on(RIGHT), on(BOTTOM_RIGHT)),
    ONE_SPRING_TWO(on(TOP_LEFT), spring(TOP_RIGHT)),
    SPRING_ONE_TWO_SPRING_THREE(spring(TOP_LEFT), on(TOP), spring(TOP_RIGHT)),
    ONE_TWO_SPRING_THREE(on(TOP_LEFT), on(TOP), spring(TOP_RIGHT)),
    ONE_TO_THREE_SPRING_FOUR(on(LEFT), on(TOP_LEFT), on(TOP), spring(TOP_RIGHT));

    public static final int MAX_DETENTS = 7;

    private final Detent[] detents;
    private final int[] throwAt;
    private final List<KnobDirection> throwPads;

    RotaryLayout(Detent... detents) {
        this.detents = detents;
        this.throwAt = new int[detents.length];
        List<KnobDirection> pads = new ArrayList<>();
        for (int i = 0; i < detents.length; i++) {
            if (detents[i].connected()) {
                pads.add(detents[i].knob());
                throwAt[i] = pads.size();
            }
        }
        this.throwPads = List.copyOf(pads);
    }

    public int detents() {
        return detents.length;
    }

    public int throwCount() {
        return throwPads.size();
    }

    public int contactAt(int detent) {
        return throwAt[clamp(detent)];
    }

    public KnobDirection knobAt(int detent) {
        return detents[clamp(detent)].knob();
    }

    public KnobDirection throwPad(int throwNumber) {
        return throwPads.get(throwNumber - 1);
    }

    public String label(int detent) {
        String number = Integer.toString(contactAt(detent));
        return detents[clamp(detent)].momentary() ? "(" + number + ")" : number;
    }

    public int springReturn(int detent) {
        int from = clamp(detent);
        if (!detents[from].momentary()) {
            return from;
        }
        for (int distance = 1; distance < detents.length; distance++) {
            if (isLatched(from - distance)) {
                return from - distance;
            }
            if (isLatched(from + distance)) {
                return from + distance;
            }
        }
        return from;
    }

    public String displayName() {
        String[] labels = new String[detents.length];
        for (int i = 0; i < detents.length; i++) {
            labels[i] = label(i);
        }
        return String.join("-", labels);
    }

    private boolean isLatched(int detent) {
        return detent >= 0 && detent < detents.length && !detents[detent].momentary();
    }

    private int clamp(int detent) {
        return Math.min(detent, detents.length - 1);
    }

    private static Detent on(KnobDirection knob) {
        return new Detent(knob, true, false);
    }

    private static Detent off(KnobDirection knob) {
        return new Detent(knob, false, false);
    }

    private static Detent spring(KnobDirection knob) {
        return new Detent(knob, true, true);
    }

    private record Detent(KnobDirection knob, boolean connected, boolean momentary) {
    }
}
