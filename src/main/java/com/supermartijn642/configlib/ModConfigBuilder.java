package com.supermartijn642.configlib;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

import java.util.*;
import java.util.function.Supplier;

/**
 * Created 1/19/2021 by SuperMartijn642
 */
public class ModConfigBuilder {

    private final List<ModConfigValue<?>> allValues = new ArrayList<>();
    private final Map<String,String> categoryComments = new HashMap<>();
    private String category = "";
    private String comment = "";
    private boolean requiresGameRestart = false;
    private boolean syncWithClient = true;

    private final String modid;
    private final ModContainer modContainer;
    private final ModConfig.Type type;

    public ModConfigBuilder(String modid, ModConfig.Type type){
        this.modid = modid;
        this.type = type;

        Optional<? extends ModContainer> optional = ModList.get().getModContainerById(this.modid);
        if(!optional.isPresent())
            throw new IllegalArgumentException("can't find mod for modid '" + modid + "'");
        this.modContainer = optional.get();
    }

    public ModConfigBuilder(String modid){
        this(modid, ModConfig.Type.COMMON);
    }

    /**
     * Pushes a new category
     * @param category the new category
     */
    public ModConfigBuilder push(String category){
        if(category == null)
            throw new IllegalArgumentException("category must not be null");
        if(category.isEmpty())
            throw new IllegalArgumentException("category must not be empty");

        if(this.category.isEmpty())
            this.category = category;

        return this;
    }

    /**
     * Pops a category
     */
    public ModConfigBuilder pop(){
        if(this.category.isEmpty())
            throw new IllegalStateException("no more categories to pop");

        int index = this.category.lastIndexOf(".");
        if(index == -1)
            this.category = "";
        else
            this.category = this.category.substring(0, index);

        return this;
    }

    /**
     * Adds a comment to the current category
     * @param comment comment to be added
     */
    public ModConfigBuilder categoryComment(String comment){
        if(comment == null)
            throw new IllegalArgumentException("comment must not be null");
        if(comment.isEmpty())
            throw new IllegalArgumentException("comment must not be empty");
        if(this.category.isEmpty())
            throw new IllegalStateException("no category pushed");
        if(this.categoryComments.containsKey(this.category))
            throw new IllegalStateException("category " + this.category + " already has a comment");

        this.categoryComments.put(this.category, comment);

        return this;
    }

    /**
     * Makes the next defined value require a world game before being changed
     */
    public ModConfigBuilder gameRestart(){
        this.requiresGameRestart = true;
        return this;
    }

    /**
     * Makes the next defined value not be synced with client
     */
    public ModConfigBuilder dontSync(){
        this.syncWithClient = false;
        return this;
    }

    /**
     * Adds a comment to the next defined value
     * @param comment comment to be added
     */
    public ModConfigBuilder comment(String comment){
        if(comment == null)
            throw new IllegalArgumentException("comment must not be null");
        if(comment.isEmpty())
            throw new IllegalArgumentException("comment must not be empty");
        if(!this.comment.isEmpty())
            throw new IllegalStateException("a comment is already specified");

        this.comment = comment;

        return this;
    }

    public Supplier<Boolean> define(String name, boolean defaultValue){
        ModConfigValue<Boolean> value = new ModConfigValue.BooleanValue(this.getPath(name), this.comment, this.requiresGameRestart, this.syncWithClient, defaultValue);
        this.allValues.add(value);
        this.resetValues();
        return value::get;
    }

    public Supplier<Integer> define(String name, int defaultValue, int minValue, int maxValue){
        ModConfigValue<Integer> value = new ModConfigValue.IntegerValue(this.getPath(name), this.comment, this.requiresGameRestart, this.syncWithClient, defaultValue, minValue, maxValue);
        this.allValues.add(value);
        this.resetValues();
        return value::get;
    }

    public Supplier<Double> define(String name, double defaultValue, double minValue, double maxValue){
        ModConfigValue<Double> value = new ModConfigValue.FloatingValue(this.getPath(name), this.comment, this.requiresGameRestart, this.syncWithClient, defaultValue, minValue, maxValue);
        this.allValues.add(value);
        this.resetValues();
        return value::get;
    }

    public <T extends Enum<T>> Supplier<T> define(String name, T defaultValue){
        ModConfigValue<T> value = new ModConfigValue.EnumValue<>(this.getPath(name), this.comment, this.requiresGameRestart, this.syncWithClient, defaultValue);
        this.allValues.add(value);
        this.resetValues();
        return value::get;
    }

    private String getPath(String name){
        if(name == null)
            throw new IllegalArgumentException("name must not be null");
        if(name.isEmpty())
            throw new IllegalArgumentException("name must not be empty");

        return (this.category.isEmpty() ? "" : this.category + '.') + name;
    }

    private void resetValues(){
        this.requiresGameRestart = false;
        this.syncWithClient = true;
        this.comment = "";
    }

    public void build(){
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        this.build(builder);
        ForgeConfigSpec spec = builder.build();

        net.minecraftforge.fml.config.ModConfig forgeConfig =
            new net.minecraftforge.fml.config.ModConfig(this.type.forgeType, spec, this.modContainer);
        this.modContainer.addConfig(forgeConfig);

        ModConfig config = new ModConfig(this.modid, this.type, this.allValues);

        config.updateValues(true);

        ConfigLib.addConfig(config);
    }

    private void build(ForgeConfigSpec.Builder builder){
        for(ModConfigValue<?> value : this.allValues)
            value.build(builder);

        for(Map.Entry<String,String> category : this.categoryComments.entrySet()){
            builder.comment(category.getValue());
            builder.push(category.getKey());
            builder.pop();
        }
    }

}
