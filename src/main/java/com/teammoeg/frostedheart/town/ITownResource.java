package com.teammoeg.frostedheart.town;

/**
 * Interface for accessing town data
 */
public interface ITownResource {
	/**
	 * Gets the whole town.
	 *
	 * @param name the resouce type
	 * @return resource amount
	 */
	TownData getTown();
	/**
	 * Gets resource.
	 *
	 * @param name the resouce type
	 * @return resource amount
	 */
	double get(TownResourceType name);
	
	/**
	 * Adds resource.
	 *
	 * @param name the resouce type
	 * @param val procuded
	 * @param simulate simulate process, not actually add.
	 * @return the value that has been add
	 */
	double add(TownResourceType name,double val,boolean simulate);
	
	/**
	 * Adds a service, 
	 * Service is kind or resource that only valid in this tick, and removed when tick ends. It would still count as a resource for get or cost
	 * @param name the resouce type
	 * @param val procuded
	 * @return the value that has been add
	 */
	double addService(TownResourceType name,double val);
	
	/**
	 * Cost a resource 
	 *
	 * @param name the resouce type
	 * @param val to be cost
	 * @param simulate simulate process, not actually cost.
	 * @return the value that has been actually cost.
	 */
	double cost(TownResourceType name,double val,boolean simulate);
	/**
	 * Cost a service 
	 *
	 * @param name the resouce type
	 * @param val to be cost
	 * @param simulate simulate process, not actually cost.
	 * @return the value that has been actually cost.
	 */
	double costService(TownResourceType name, double val, boolean simulate);
	/**
	 * Cost a resource as a service, that means does not actually cost the resource, but would cost temporary at this tick.
	 *
	 * @param name the resouce type
	 * @param val to be cost
	 * @param simulate simulate process, not actually cost.
	 * @return the value that has been actually cost.
	 */
	double costAsService(TownResourceType name, double val, boolean simulate);
	
}
