package com.nstut.firstworks.content.quern;

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
     * @return true if the quern is currently engaged in continuous driven rotation.
     */
    boolean isDriven();

    /**
     * Sets whether external rotational drive is applied to this quern.
     *
     * @param driven true to engage continuous rotation, false to disengage.
     */
    void setDriven(boolean driven);
}