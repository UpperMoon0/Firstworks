package com.nstut.firstworks.content.quern;

import com.nstut.firstworks.FirstworksConfig;

/**
 * Public interface for workstations that can receive external continuous rotational drive,
 * such as animal sweeps (Create Horse Power), water wheels, or kinetic power systems.
 */
public interface QuernDriveable {
    /**
     * @return true if this quern currently contains a valid, workable batch and is able to accept drive.
     */
    boolean canDrive();

    /**
     * @return the work progress added per tick from external drive (0 if not driven).
     */
    int getDriveRate();

    /**
     * Sets the external continuous drive rate applied to this quern.
     *
     * @param workPerTick work progress added per tick (0 to stop/disconnect).
     */
    void setDriveRate(int workPerTick);

    /**
     * @return true if the quern is currently engaged in continuous driven rotation.
     */
    default boolean isDriven() {
        return getDriveRate() > 0;
    }

    /**
     * Sets whether external rotational drive is applied to this quern using default rate.
     *
     * @param driven true to engage continuous rotation, false to disengage.
     */
    default void setDriven(boolean driven) {
        setDriveRate(driven ? FirstworksConfig.QUERN_DEFAULT_DRIVEN_WORK_PER_TICK.get() : 0);
    }
}
