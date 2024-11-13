package com.teammoeg.frostedheart.base.item.rankine.alloys;


import java.util.*;

public class AlloyRecipeHelper {


    public static String getDirectComposition(List<Integer> percents, List<String> inputs) {
        StringBuilder ret = new StringBuilder();
        Map<String,Integer> map = new HashMap<>();

        for (int i = 0; i < inputs.size(); i++)
        {
            map.put(inputs.get(i),percents.get(i));
        }
        List<Integer> sPercents = new ArrayList<>();
        List<String> sInputs = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : mapStringsSort(map).entrySet())
        {
            if (entry.getValue() > 0) {
                sPercents.add(entry.getValue());
                sInputs.add(entry.getKey());
            }

        }
        Collections.reverse(sPercents);
        Collections.reverse(sInputs);
        for (int i = 0; i < sPercents.size(); i++)
        {
            ret.append(sPercents.get(i)).append(sInputs.get(i));
            if (i != sPercents.size() - 1) {
                ret.append("-");
            }
        }
        //System.out.println("Result: " + ret.toString());
        return ret.toString();
    }

    public static String getDirectComposition(Map<ElementRecipe,Integer> elementMap) {
        StringBuilder ret = new StringBuilder();
        Map<String,Integer> map = new HashMap<>();

        for (Map.Entry<ElementRecipe, Integer> entry : elementMap.entrySet())
        {
            map.put(entry.getKey().getSymbol(),entry.getValue());
        }
        List<Integer> sPercents = new ArrayList<>();
        List<String> sInputs = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : mapStringsSort(map).entrySet())
        {
            if (entry.getValue() > 0) {
                sPercents.add(entry.getValue());
                sInputs.add(entry.getKey());
            }

        }
        Collections.reverse(sPercents);
        Collections.reverse(sInputs);
        for (int i = 0; i < sPercents.size(); i++)
        {
            ret.append(sPercents.get(i)).append(sInputs.get(i));
            if (i != sPercents.size() - 1) {
                ret.append("-");
            }
        }
        //System.out.println("Result: " + ret.toString());
        return ret.toString();
    }

    public static Map<String, Integer> mapStringsSort(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort((o1, o2) -> {
            if (o1.getValue().equals(o2.getValue())) {
                return o2.getKey().compareToIgnoreCase(o1.getKey());
            }
            return o1.getValue() < o2.getValue() ? -1 : 1;
        });

        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /*public static String getAlloyFromComposition(String comp, World worldIn) {
        List<PeriodicTableUtils.Element> elements = new ArrayList<>();
        List<Integer> percents = new ArrayList<>();
        for (String s : comp.split("-")) {
            String str = s.replaceAll("[^A-Za-z]+", "");
            int num = Integer.parseInt(s.replaceAll("[A-Za-z]+",""));
            elements.add(utils.getElementBySymbol(str));
            percents.add(num);
        }

        for (AlloyingRecipe recipe: worldIn.getRecipeManager().getRecipesForType(RankineRecipeTypes.ALLOYING)) {
            boolean flag = true;
            List<PeriodicTableUtils.Element> req = recipe.getElementList(true);
            List<PeriodicTableUtils.Element> all = recipe.getElements();
            List<Float> mins = recipe.getMins();
            List<Float> maxes = recipe.getMaxes();

            for (PeriodicTableUtils.Element e : req) {
                if (!elements.contains(e)) {
                    flag = false;
                    break;
                } else {
                    int recindex = all.indexOf(e);
                    int elemindex = elements.indexOf(e);
                    int perc = percents.get(elemindex);
                    if (Math.round(mins.get(recindex) * 100) > perc || Math.round(maxes.get(recindex) * 100) < perc) {
                        flag = false;
                        break;
                    }
                }
            }
            if (flag) {
                for (PeriodicTableUtils.Element el : elements) {
                    if (!all.contains(el)) {
                        flag = false;
                        break;
                    } else {
                        int recindex = all.indexOf(el);
                        int elemindex = elements.indexOf(el);
                        int perc = percents.get(elemindex);
                        if (Math.round(mins.get(recindex) * 100) > perc || Math.round(maxes.get(recindex) * 100) < perc) {
                            flag = false;
                            break;
                        }
                    }
                }
            }


            if (flag) {
                String name = new TranslationTextComponent(recipe.getRecipeOutput().getTranslationKey()).getString();
                if (name.contains(" Ingot")) {
                    name = name.split(" Ingot")[0];
                }
                if (name.contains(" Alloy") && !name.contains("Heavy Alloy")) {
                    name = name.split(" Alloy")[0];
                }
                return name;
            }
        }
        if (elements.size() >= 3) {
            return elements.get(0).name().charAt(0) + elements.get(0).name().substring(1).toLowerCase() + " Alloy";
        } else if (elements.size() == 2) {
            return elements.get(0).name().charAt(0) + elements.get(0).name().substring(1).toLowerCase() + "-" + elements.get(1).name().charAt(0) + elements.get(1).name().substring(1).toLowerCase() + " Alloy";
        } else if (elements.size() == 1) {
            return elements.get(0).name().charAt(0) + elements.get(0).name().substring(1).toLowerCase();
        }
        return "false";
    }

        public static AlloyingRecipe getRecipeFromComposition(String comp, World worldIn) {

    }
    */


}
