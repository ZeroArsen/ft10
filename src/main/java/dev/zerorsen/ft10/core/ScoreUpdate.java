package dev.zerorsen.ft10.core;

/**
 * One point was scored.
 *
 * @param me   score after the point (0 - 0 again when the point finished a set and a new set started)
 * @param them see {@code me}
 * @param set  non-null when this point reached the first-to goal
 */
public record ScoreUpdate(Rivalry rivalry, Side scorer, int me, int them, SetResult set) {

    /** A finished first-to set. {@code finalMe}/{@code finalThem} is the score that ended it. */
    public record SetResult(Side winner, int goal, int finalMe, int finalThem, int setsMe, int setsThem) {}
}
