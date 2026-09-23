package io.github.scuba10steve.ssm.generator;

public class CombustionBurnState {

    public record TickResult(int remainingBurnTime, int energyToGenerate) {}

    private int remainingBurnTime;

    public void startBurning(int burnTime) {
        this.remainingBurnTime = burnTime;
    }

    public boolean isBurning() {
        return remainingBurnTime > 0;
    }

    public int getRemainingBurnTime() {
        return remainingBurnTime;
    }

    /** Used only by CombustionGeneratorBlockEntity#loadAdditional to restore state after a chunk reload. */
    public void setRemainingBurnTime(int remainingBurnTime) {
        this.remainingBurnTime = remainingBurnTime;
    }

    public TickResult tick(int generationRate, int bufferRoom) {
        if (remainingBurnTime <= 0) {
            return new TickResult(remainingBurnTime, 0);
        }
        if (bufferRoom <= 0) {
            return new TickResult(remainingBurnTime, 0); // paused: buffer full, don't consume burn time
        }
        remainingBurnTime--;
        return new TickResult(remainingBurnTime, generationRate);
    }
}
