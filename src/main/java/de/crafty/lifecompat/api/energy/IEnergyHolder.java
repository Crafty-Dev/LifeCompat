package de.crafty.lifecompat.api.energy;

public interface IEnergyHolder {

    //Should return the maximun capacity of this block
    int getEnergyCapacity();

    //Should return the current energy amount this block contains
    int getStoredEnergy();

    void setStoredEnergy(int energy);

    void setEnergyCapacity(int capacity);
}
