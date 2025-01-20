package com.teammoeg.frostedheart.content.town.resource;

/**
 *
 * @param allSuccess actually added/costed == amount to add/cost
 * @param actualAmount actually added/costed
 * @param lowestLevel lowest level of added/costed resources
 * @param averageLevel average level of added/costed resources. 根据数量加权平均
 */
public record ResourceActionResult (boolean allSuccess, double actualAmount, double lowestLevel, double averageLevel){
    public static final ResourceActionResult NOT_SUCCESS = new ResourceActionResult(false, 0, 0, 0);

    public ResourceActionResult(boolean allSuccess, double actualAmount, double lowestLevel, double averageLevel){
        if(actualAmount<0){
            this.allSuccess = false;
            this.actualAmount = 0;
            this.lowestLevel = 0;
            this.averageLevel = 0;
        }
        else {
            this.allSuccess = allSuccess;
            this.actualAmount = actualAmount;
            this.lowestLevel = lowestLevel;
            this.averageLevel = averageLevel;
        }
    }

    public ResourceActionResult(boolean allSuccess, double actualAmount, ITownResourceKey key){
        this(allSuccess, actualAmount, key.getLevel(), key.getLevel());
    }

    /**
     * If you just costed a ItemStack, not a ITownResourceKey, use this constructor.
     * ItemStack don't have a certain level so the lowestLevel and averageLevel are both 0.
     */
    public ResourceActionResult(boolean allSuccess, double actualAmount){
        this(allSuccess, actualAmount, 0, 0);
    }
}
