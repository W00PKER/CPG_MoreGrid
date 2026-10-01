package com.feb.moregrid.component.rotary;

public enum KnobDirection {
    BOTTOM_LEFT(135, 0, 2),
    LEFT(90, 0, 1),
    TOP_LEFT(45, 0, 0),
    TOP(0, 1, 0),
    TOP_RIGHT(-45, 2, 0),
    RIGHT(-90, 2, 1),
    BOTTOM_RIGHT(-135, 2, 2);

    private final float angle;
    private final int padX;
    private final int padY;

    KnobDirection(float angle, int padX, int padY) {
        this.angle = angle;
        this.padX = padX;
        this.padY = padY;
    }

    public float angle() {
        return angle;
    }

    public int padX() {
        return padX;
    }

    public int padY() {
        return padY;
    }
}
