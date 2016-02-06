package com.Joedobo27.WUmod;

import com.wurmonline.server.items.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


@SuppressWarnings("unused")
public class LoadingUnchainedMod implements WurmMod, Initable, Configurable, ServerStartedListener {

    //<editor-fold desc="Class Fields">
    private boolean loadOnlyEmptyBins = true;
    private boolean moveItemsIntoBinWithinContainer = false;
    private boolean useCustomProximity = false;
    private int loadProximityRequired = 4;
    private boolean useCustomLoadTime = false;
    private int loadDurationTime = 100;
    private boolean requireEmbarked = true;
    private boolean boatInCart = false;
    private boolean useBedInCart = false;
    private boolean useMagicChestInCart = false;
    private boolean useForgeInCart = false;
    private boolean craftWithinCart = false;
    private boolean loadAltar = false;
    private boolean loadOther = false;

    private CodeAttribute moveToItemAttribute;
    private CodeIterator moveToItemIterator;
    private MethodInfo moveToItemMInfo;

    private CodeAttribute loadCargoAttribute;
    private CodeIterator loadCargoIterator;
    private MethodInfo loadCargoMInfo;

    private CodeAttribute unloadCargoAttribute;
    private CodeIterator unloadCargoIterator;
    private MethodInfo unloadCargoMInfo;

    private CodeAttribute performerIsNotOnATransportVehicleAttribute;
    private CodeIterator performerIsNotOnATransportVehicleIterator;
    private MethodInfo performerIsNotOnATransportVehicleMInfo;

    private CodeAttribute getLoadUnloadActionsAttribute;
    private CodeIterator getLoadUnloadActionsIterator;
    private MethodInfo getLoadUnloadActionsMInfo;

    private CodeAttribute addBedOptionsAttribute;
    private CodeIterator addBedOptionsIterator;
    private MethodInfo addBedOptionsMInfo;

    private CodeAttribute askSleepAttribute;
    private CodeIterator askSleepIterator;
    private MethodInfo askSleepMInfo;

    private CodeAttribute sleepAttribute;
    private CodeIterator sleepIterator;
    private MethodInfo sleepMInfo;

    private CodeAttribute mayUseBedAttribute;
    private CodeIterator mayUseBedIterator;
    private MethodInfo mayUseBedMInfo;

    private CodeAttribute startFireAttribute;
    private CodeIterator startFireIterator;
    private MethodInfo startFireMInfo;

    private CodeAttribute setFireAttribute;
    private CodeIterator setFireIterator;
    private MethodInfo setFireMInfo;

    private CodeAttribute addCreationWindowOptionAttribute;
    private CodeIterator addCreationWindowOptionIterator;
    private MethodInfo addCreationWindowOptionMInfo;

    //</editor-fold>

    private static Logger logger = Logger.getLogger(LoadingUnchainedMod.class.getName());

    public void configure(Properties properties) {
        loadOnlyEmptyBins = Boolean.valueOf(properties.getProperty("loadOnlyEmptyBins", Boolean.toString(loadOnlyEmptyBins)));
        moveItemsIntoBinWithinContainer = Boolean.valueOf(properties.getProperty("moveItemsIntoBinWithinContainer",
                Boolean.toString(moveItemsIntoBinWithinContainer)));
        useCustomProximity = Boolean.valueOf(properties.getProperty("useCustomProximity", Boolean.toString(useCustomProximity)));
        loadProximityRequired = Integer.valueOf(properties.getProperty("loadProximityRequired", Integer.toString(loadProximityRequired)));
        loadProximityRequired = Math.max(loadProximityRequired, 1);
        loadProximityRequired = Math.min(loadProximityRequired, 160);
        useCustomLoadTime = Boolean.valueOf(properties.getProperty("useCustomLoadTime", Boolean.toString(useCustomLoadTime)));
        loadDurationTime = Integer.valueOf(properties.getProperty("loadDurationTime", Integer.toString(loadDurationTime)));
        loadDurationTime = Math.max(loadDurationTime, 1);
        loadDurationTime = Math.min(loadDurationTime, 255);
        requireEmbarked = Boolean.valueOf(properties.getProperty("requireEmbarked", Boolean.toString(requireEmbarked)));
        boatInCart = Boolean.valueOf(properties.getProperty("boatInCart", Boolean.toString(boatInCart)));
        useBedInCart = Boolean.valueOf(properties.getProperty("useBedInCart", Boolean.toString(useBedInCart)));
        useMagicChestInCart = Boolean.valueOf(properties.getProperty("useMagicChestInCart", Boolean.toString(useMagicChestInCart)));
        useForgeInCart = Boolean.valueOf(properties.getProperty("useForgeInCart", Boolean.toString(useForgeInCart)));
        craftWithinCart = Boolean.valueOf(properties.getProperty("craftWithinCart", Boolean.toString(craftWithinCart)));
        loadAltar = Boolean.valueOf(properties.getProperty("loadAltar", Boolean.toString(loadAltar)));
        loadOther = Boolean.valueOf(properties.getProperty("loadOther", Boolean.toString(loadOther)));
    }

    @Override
    public void onServerStarted() {
        if (useBedInCart || boatInCart || loadAltar || loadOther) {
            try {
                Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                        ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
                for (ItemTemplate template : fieldTemplates.values()) {
                    Integer templateId = template.getTemplateId();
                    if (useBedInCart) {
                        Field fieldUseOnGroundOnly = ReflectionUtil.getField(ItemTemplate.class, "useOnGroundOnly");
                        if (templateId == 484 || templateId == 890) {
                            if (template.isUseOnGroundOnly()) {
                                ReflectionUtil.setPrivateField(template, fieldUseOnGroundOnly, Boolean.FALSE);
                            }
                        }
                    }
                    if (boatInCart) {
                        Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                        if (template.isFloating()){
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                        }
                    }
                    if (loadAltar){
                        Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                        if (templateId == ItemList.altarWood || templateId == ItemList.altarGold || templateId == ItemList.altarSilver ||
                                templateId == ItemList.altarStone){
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                        }

                    }
                    if (loadOther){
                        Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                        if (templateId == ItemList.trashBin){
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                        }
                    }

                }
            }catch(NoSuchFieldException | IllegalAccessException e){
                logger.log(Level.WARNING, e.toString());
            }
        }
    }

    @Override
    public void init() {
        String replaceByteResult;
        String printByteResult;

        try {
            ClassPool pool = HookManager.getInstance().getClassPool();

            CtClass ctcCargoTransportationMethods = pool.get("com.wurmonline.server.behaviours.CargoTransportationMethods");
            ClassFile cfCargoTransportationMethods = ctcCargoTransportationMethods.getClassFile();

            CtClass ctcItem = pool.get("com.wurmonline.server.items.Item");
            ClassFile cfItem = ctcItem.getClassFile();

            CtClass ctcMethodsItems = pool.get("com.wurmonline.server.behaviours.MethodsItems");
            ClassFile cfMethodsItems = ctcMethodsItems.getClassFile();

            CtClass ctcItemBehaviour = pool.get("com.wurmonline.server.behaviours.ItemBehaviour");
            ClassFile cfItemBehaviour = ctcItemBehaviour.getClassFile();

            if (!loadOnlyEmptyBins){
                CtMethod ctmTargetIsNotEmptyContainerCheck = ctcCargoTransportationMethods.getMethod("targetIsNotEmptyContainerCheck",
                        "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z");
                ctmTargetIsNotEmptyContainerCheck.setBody("{return false;}");
            }

            if (moveItemsIntoBinWithinContainer){
                setMoveToItem(cfItem, "(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "ac,1908,b6057e,1908,b60265,94,99002e,1908,b60462,9a0026,2b,c60020,1306d0,04,bd0003,59,03,1908,b60174,53,b80591,3a13,2b,b605ba,1913,b605c0,03,ac,2a",
                        "1908,b6057e,1908,b60265,94,99002e,1908,b60462,9a0026,2b,c60020,1306d0,04,bd0003,59,03,1908,b60174,53,b80591,3a13,2b,b605ba,1913,b605c0,03,ac",
                        "0000,000000,0000,000000,00,000000,0000,000000,000000,00,000000,000000,00,000000,00,00,0000,000000,00,000000,0000,00,000000,0000,000000,00,00",
                        getMoveToItemIterator(), "moveToItem");
                //<editor-fold desc="Replaced code">
                /*
                Removed-
                    if (target.getTopParent() != target.getWurmId() && !target.isCrate()) {
                        if (mover != null) {
                            final String message = String.format("The %s needs to be on the ground.", target.getName());
                            mover.getCommunicator().sendNormalServerMessage(message);
                        }
                        return false;
                    }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
            }
            if (useCustomLoadTime) {
                String s = "10" + String.format("%02X", loadDurationTime & 0xff);
                setLoadCargo(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z", "loadCargo");
                replaceByteResult = jaseBT.byteCodeFindReplace("AC,1064,3608", "1064", s, getLoadCargoIterator(), "loadCargo");
                logger.log(Level.INFO, replaceByteResult);
                setUnloadCargo(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z", "unloadCargo");
                replaceByteResult = jaseBT.byteCodeFindReplace("AC,1064,3606", "1064", s, getUnloadCargoIterator(), "unloadCargo");
                logger.log(Level.INFO, replaceByteResult);
            }
            if (useCustomProximity){
                CtMethod ctmGetLoadActionDistance = ctcCargoTransportationMethods.getMethod("getLoadActionDistance",
                        "(Lcom/wurmonline/server/items/Item;)I");

                ctmGetLoadActionDistance.setBody("{return " + loadProximityRequired + ";}");
            }
            if (!requireEmbarked) {
                setPerformerIsNotOnATransportVehicle(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Vehicle;)Z",
                        "performerIsNotOnATransportVehicle");
                replaceByteResult = jaseBT.byteCodeFindReplace("2B,C6000A,2B", "2B,C6000A", "00,000000", getPerformerIsNotOnATransportVehicleIterator(), "performerIsNotOnATransportVehicle");
                //Removed "vehicle == null ||".
                logger.log(Level.INFO, replaceByteResult);
                CtMethod ctmPerformerIsNotOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotOnAVehicle",
                        "(Lcom/wurmonline/server/creatures/Creature;)Z");
                ctmPerformerIsNotOnAVehicle.setBody("{return false;}");

                CtMethod ctmPerformerIsNotSeatedOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotSeatedOnAVehicle",
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Seat;)Z");
                ctmPerformerIsNotSeatedOnAVehicle.setBody("{return false;}");
            }
            if (boatInCart) {
                setGetLoadUnloadActions(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Ljava/util/List;",
                        "getLoadUnloadActions");
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "3a05,2b,b6027c,990010,1905,b6031f,110355,9f0005,2c,b0,2b,b6042d,990025,1905,b6031f,110355,a0001a,2b,b60433,3a06,1906,c70005,2c,b0,1906,b60436,9a0005,2c,b0,2a",
                        "2b,b6027c,990010,1905,b6031f,110355,9f0005,2c,b0,2b,b6042d,990025,1905,b6031f,110355,a0001a,2b,b60433,3a06,1906,c70005,2c,b0,1906,b60436,9a0005,2c,b0",
                        "00,000000,000000,0000,000000,000000,000000,00,00,00,000000,000000,0000,000000,000000,000000,00,000000,0000,0000,000000,00,00,0000,000000,000000,00,00",
                        getGetLoadUnloadActionsIterator(), "getLoadUnloadActions");
                //<editor-fold desc="Replaced code">
                /*
                Removed-
                        if (target.isBoat() && vehicleItem.getTemplateId() != 853) {
                                return toReturn;
                            }
                            if (target.isUnfinished() && vehicleItem.getTemplateId() == 853) {
                                final ItemTemplate template = target.getRealTemplate();
                                if (template == null) {
                                    return toReturn;
                                }
                                if (!template.isBoat()) {
                                    return toReturn;
                                }
                            }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                getGetLoadUnloadActionsAttribute().computeMaxStack();
                getGetLoadUnloadActionsMInfo().rebuildStackMapIf6(pool, cfCargoTransportationMethods);
            }
            if (useBedInCart) {
                setAddBedOptions(cfItemBehaviour,
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Ljava/util/List;)V", "addBedOptions");

                getAddBedOptionsIterator().insertGap(41, 12);
                getAddBedOptionsIterator().insertExGap(373, 18);

                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "1904,c60174,1904,b601a9,c6016c,1904,b601a9,b60f60,990161,00,00,00,00,00,00,00,00,00,00,00,00,2c",
                        "c60174,1904,b601a9,c6016c,1904,b601a9,b60f60,990161,00,00,00,00,00,00,00,00,00,00,00,00",
                        "c60165,1904,b601a9,c6015d,1904,b601a9,b60f60,990152,2c,b600df,2c,b600cf,94,9a0146",
                        getAddBedOptionsIterator(), "addBedOptions");
                //<editor-fold desc="Replaced code">
                /*
                Added "&& target.getTopParent() == target.getWurmId()" to the logic statement:
                "if (t != null && t.getStructure() != null && t.getStructure().isTypeHouse()".
                If the bed is on ground and in a house use renting.
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "b6049b,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,b1",
                        "00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00",
                        "a70012,2d,b2007f,110145,32,b900850200,00,57",
                        getAddBedOptionsIterator(), "addBedOptions");
                //<editor-fold desc="Replaced code">
                /*
                Added-
                    } else {
                        toReturn.add(Actions.actionEntrys[325]);
                    }
                This else block is a complement to the if block:
                "if (t != null && t.getStructure() != null && t.getStructure().isTypeHouse()"
                When the bed is not on the ground and not in a house it can't be controlled so use free sleep.
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                getAddBedOptionsAttribute().computeMaxStack();
                getAddBedOptionsMInfo().rebuildStackMapIf6(pool, cfItemBehaviour);

                setAskSleep(cfMethodsItems, "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                        "askSleep");

                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "ac,2c,b6015a,2c,b6015d,2c,b600d3,b8026d,3a05,1905,c600b7,1905,b600e2,c6000e,1905,b600e2,b6032a,9a000f,2b,b600f1,131008,b60106,04,ac,1905,b600e2,b60276,9a000f,2b,b600f1,13100a,b60106,04,ac,2c",
                        "2c,b6015a,2c,b6015d,2c,b600d3,b8026d,3a05,1905,c600b7,1905,b600e2,c6000e,1905,b600e2,b6032a,9a000f,2b,b600f1,131008,b60106,04,ac,1905,b600e2,b60276,9a000f,2b,b600f1,13100a,b60106,04,ac",
                        "00,000000,00,000000,00,000000,000000,0000,0000,000000,0000,000000,000000,0000,000000,000000,000000,00,000000,000000,000000,00,00,0000,000000,000000,000000,00,000000,000000,000000,00,00",
                        getAskSleepIterator(), "askSleep");
                //<editor-fold desc="Replaced code">
                /*
                Removed-
                    final VolaTile t = Zones.getTileOrNull(target.getTileX(), target.getTileY(), target.isOnSurface());
                    if (t != null) {
                        if (t.getStructure() == null || !t.getStructure().isTypeHouse()) {
                            performer.getCommunicator().sendNormalServerMessage("You would get no sleep outside tonight.");
                            return true;
                        }
                        if (!t.getStructure().isFinished()) {
                            performer.getCommunicator().sendNormalServerMessage("The house is too windy to provide protection.");
                            return true;
                        }
                Shifted the code regards to renting out of the block started with "if (t != null) {".
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);

                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "ac,b2004b,b201ed,bb00f5,59,13101e,b700f9,2c,b6015a,b604ad,131020,b600fd,2c,b6015d,b604ad,131020,b600fd,2c,b600d3,b61022,131025,b600fd,b60103,b60399,bb1027",
                        "b2004b,b201ed,bb00f5,59,13101e,b700f9,2c,b6015a,b604ad,131020,b600fd,2c,b6015d,b604ad,131020,b600fd,2c,b600d3,b61022,131025,b600fd,b60103,b60399",
                        "000000,000000,000000,00,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,000000,000000",
                        getAskSleepIterator(), "askSleep");
                //<editor-fold desc="Replaced code">
                /*
                Removed-
                    else {
                        MethodsItems.logger.log(Level.WARNING, "Why is tile for bed at " + target.getTileX() + "," + target.getTileY() + "," + target.isOnSurface() + " null?");
                    }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                getAskSleepAttribute().computeMaxStack();
                getAskSleepMInfo().rebuildStackMapIf6(pool, cfMethodsItems);

                setSleep(cfMethodsItems, "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z", "sleep");
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "ac,2c,b6010e,2c,b60190,94,99000f,2b,b600f1,131038,b60106,04,ac,2c",
                        "2c,b6010e,2c,b60190,94,99000f,2b,b600f1,131038,b60106,04,ac",
                        "00,000000,00,000000,00,000000,00,000000,000000,000000,00,00",
                        getSleepIterator(), "sleep");
                //<editor-fold desc="Replaced code">
                /*
                Removed-
                    if (target.getTopParent() != target.getWurmId()) {
                        performer.getCommunicator().sendNormalServerMessage("The bed needs to be on the ground.");
                        return true;
                    }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "3a06,1906,c60083,2b,b6007d,9d0076,1906,b600e2,c6000e,1906,b600e2,b6032a,9a000f,2b,b600f1,131008,b60106,04,ac,1906,b600e2,b60276,9a000f,2b,b600f1,13100a,b60106,04,ac,2c",
                        "1906,c60083,2b,b6007d,9d0076,1906,b600e2,c6000e,1906,b600e2,b6032a,9a000f,2b,b600f1,131008,b60106,04,ac,1906,b600e2,b60276,9a000f,2b,b600f1,13100a,b60106,04,ac",
                        "0000,000000,2b,b6007d,9d0076,0000,000000,000000,0000,000000,000000,000000,00,000000,000000,000000,00,00,0000,000000,000000,000000,00,000000,000000,000000,00,00",
                        getSleepIterator(), "sleep");
                //<editor-fold desc="Replace code">
                /*
                Removed-
                    if (t.getStructure() == null || !t.getStructure().isTypeHouse()) {
                        performer.getCommunicator().sendNormalServerMessage("You would get no sleep outside tonight.");
                        return true;
                    }
                    if (!t.getStructure().isFinished()) {
                        performer.getCommunicator().sendNormalServerMessage("The house is too windy to provide protection.");
                        return true;
                    }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "a70040,b2004b,b201ed,bb00f5,59,13101e,b700f9,2c,b6015a,b604ad,131020,b600fd,2c,b6015d,b604ad,131020,b600fd,2c,b600d3,b61022,131025,b600fd,b60103,b60399,25",
                        "b2004b,b201ed,bb00f5,59,13101e,b700f9,2c,b6015a,b604ad,131020,b600fd,2c,b6015d,b604ad,131020,b600fd,2c,b600d3,b61022,131025,b600fd,b60103,b60399",
                        "000000,000000,000000,00,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,000000,000000",
                        getSleepIterator(), "sleep");
                //<editor-fold desc="Replace code">
                /*
                -Removed
                    else {
                        MethodsItems.logger.log(Level.WARNING, "Why is tile for bed at " + target.getTileX() + "," + target.getTileY() + "," + target.isOnSurface() + " null?");
                    }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                setMayUseBed(cfItem, "(Lcom/wurmonline/server/creatures/Creature;)Z", "mayUseBed");
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "9a0032,2a,b604ac,2a,b60292,b80957,4d,2c,c6000a,2c,b60611,a70004,01,4e,2d,c60013,2d,b616ca,99000c,2d,b616cd,990005,04,ac,03,ac",
                        "2a,b604ac,2a,b60292,b80957,4d,2c,c6000a,2c,b60611,a70004,01,4e,2d,c60013,2d,b616ca,99000c,2d,b616cd,990005,04,ac,03",
                        "00,000000,00,000000,000000,00,00,000000,00,000000,000000,00,00,00,000000,00,000000,000000,00,000000,000000,00,00,04",
                        getMayUseBedIterator(), "mayUseBed");
                //<editor-fold desc="Replace code">
                /*
                -Removed
                    final VolaTile vt = Zones.getTileOrNull(this.getTilePos(), this.isOnSurface());
                    final Structure structure = (vt != null) ? vt.getStructure() : null;
                    return structure != null && structure.isTypeHouse() && structure.isFinished();

                -Always return true for this block "(!ItemSettings.exists(this.getWurmId())"
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);

                getMayUseBedAttribute().computeMaxStack();
                getMayUseBedMInfo().rebuildStackMapIf6(pool, cfItem);
                //ctcItem.rebuildClassFile();

            }
            if (useMagicChestInCart) {
                setMoveToItem(cfItem, "(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "370a,1908,b601ac,110298,9f000b,1908",
                        "1908,b601ac,110298,9f000b",
                        "0000,000000,000000,000000",
                        getMoveToItemIterator(), "moveToItem");
                //<editor-fold desc="Replace code">
                /*
                Removed-
                    target.getTemplateId() == 664 ||
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
            }
            if (useForgeInCart) {
                setStartFire(cfMethodsItems, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;F)Z", "startFire");
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "2c,b60537,b6053b,99002d,2c,b6010e,2c,b60190,94,990021,130540,04,bd0003,59,03,2c,b600fc,53,b80542,3a09,2a,b600f1,1909,b60106,04,ac,25",
                        "b60537,b6053b,99002d,2c,b6010e,2c,b60190,94,990021,130540,04,bd0003,59,03,2c,b600fc,53,b80542,3a09,2a,b600f1,1909,b60106,04,ac",
                        "000000,000000,000000,00,000000,00,000000,00,000000,000000,00,000000,00,00,00,000000,00,000000,0000,00,000000,0000,000000,00,00",
                        getStartFireIterator(), "startFire");
                //<editor-fold desc="Replace code">
                /*
                Removed-
                    if (target.getTemplate().isTransportable() && target.getTopParent() != target.getWurmId()) {
                        final String message = String.format("The %s must be on the ground before you can light it.", target.getName());
                        performer.getCommunicator().sendNormalServerMessage(message);
                        return true;
                    }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                getStartFireAttribute().computeMaxStack();
                getStartFireMInfo().rebuildStackMapIf6(pool, cfMethodsItems);
                setSetFire(cfMethodsItems, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z", "setFire");
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "2b,b60537,b6053b,99002b,2b,b6010e,2b,b60190,94,99001f,130540,04,bd0003,59,03,2b,b600fc,53,b80542,4d,2a,b600f1,2c,b60106,04,ac,2b",
                        "2b,b60537,b6053b,99002b,2b,b6010e,2b,b60190,94,99001f,130540,04,bd0003,59,03,2b,b600fc,53,b80542,4d,2a,b600f1,2c,b60106,04,ac",
                        "00,000000,000000,000000,00,000000,00,000000,00,000000,000000,00,000000,00,00,00,000000,00,000000,00,00,000000,00,000000,00,00",
                        getSetFireIterator(), "setFire");
                //<editor-fold desc="Replace code">
                /*
                Removed-
                    if (target.getTemplate().isTransportable() && target.getTopParent() != target.getWurmId()) {
                        final String message = String.format("The %s must be on the ground before you can light it.", target.getName());
                        performer.getCommunicator().sendNormalServerMessage(message);
                        return true;
                    }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                getSetFireAttribute().computeMaxStack();
                getSetFireMInfo().rebuildStackMapIf6(pool, cfMethodsItems);

                CtMethod ctmTargetIsOnFireCheck = ctcCargoTransportationMethods.getDeclaredMethod("targetIsOnFireCheck");
                ctmTargetIsOnFireCheck.setBody("{return false;}");

                //Remove the fire effect from zone on load. Add the fire effect to the zone on unload.
                //The fire effect associated with forge "Item" will be removed when its temperature is cool enough.
                CtMethod ctmLoadCargo = ctcCargoTransportationMethods.getMethod("loadCargo",
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
                ctmLoadCargo.instrument(
                        new ExprEditor() {
                            public void edit(MethodCall m) throws CannotCompileException {
                                // After the call to "zone.removeItem(target);" add code to remove fire effect.
                                if (m.getClassName().equals("com.wurmonline.server.zones.Zone") && m.getMethodName().equals("removeItem")) {
                                    m.replace(
                                            "{$proceed($$);" +
                                            "com.wurmonline.server.effects.Effect[] targetEffects = $1.getEffects();" +
                                            "for (int i = 0; i < targetEffects.length; i++) {" +
                                            "$0.removeEffect(targetEffects[i]);}}");
                                }
                            }
                        }
                );
                CtMethod ctmUnloadCargo = ctcCargoTransportationMethods.getMethod("unloadCargo",
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
                ctmUnloadCargo.instrument(
                        new ExprEditor() {
                            public void edit(MethodCall m) throws CannotCompileException {
                                // After the call to "zone.addItem(target);" add code to create fire effect.
                                // The Item.effect for fire associated with the forge was never removed.
                                if (m.getClassName().equals("com.wurmonline.server.zones.Zone") && m.getMethodName().equals("addItem")) {
                                    m.replace(
                                            "{$proceed($$);" +
                                            "if (target.isOnFire()) {" +
                                            "com.wurmonline.server.effects.Effect[] targetEffects = $1.getEffects();" +
                                            "for (int i = 0; i < targetEffects.length; i++) {" +
                                            "targetEffects[i].setPosX($1.getPosX());" +
                                            "targetEffects[i].setPosY($1.getPosY());" +
                                            "targetEffects[i].setPosZ($1.getPosZ());" +
                                            "$0.addEffect(targetEffects[i]);" +
                                            "}}}");
                                }
                            }
                        }
                );
            }
            if (craftWithinCart) {
                setAddCreationWindowOption(cfItemBehaviour,
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Ljava/util/List;)V", "addCreationWindowOption");

                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "2b,b602e8,990043,2b",
                        "2b,b602e8,990043",
                        "00,000000,000000", getAddCreationWindowOptionIterator(), "addCreationWindowOption");
                //<editor-fold desc="Replace code">
                /*
                Removed-
                    target.isUseOnGroundOnly() &&
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                replaceByteResult = jaseBT.byteCodeFindReplace(
                        "9a0020,2b,b600cf,2b,b600df,94,9a0029,2c",
                        "2b,b600cf,2b,b600df,94,9a0029",
                        "00,000000,00,000000,00,000000", getAddCreationWindowOptionIterator(), "addCreationWindowOption");
                //<editor-fold desc="Replace code">
                /*
                Removed-
                    if (target.getTopParent() == target.getWurmId()) {
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                getAddCreationWindowOptionAttribute().computeMaxStack();
                getAddCreationWindowOptionMInfo().rebuildStackMapIf6(pool, cfItemBehaviour);

            }
        } catch (NotFoundException | CannotCompileException | BadBytecode e) {
            e.printStackTrace();
        }
    }

    /*
    Multiple CodeIterators pointing to the same CodeAttribute can cause problems.
    Inserting gaps will always break other iterators pointing to the same attribute.
    CodeAttribute and MethodInfo also cause problems when multiple objects exist representing the same Wurm method.
    Use one class wide object for each CodeAttribute, CodeIterator, MethodInfo for all Wurm methods.
    The following section are get and set methods to achieve this.
    */
    public void setAddBedOptions(ClassFile cf, String desc, String name) {
        if (this.addBedOptionsMInfo == null || this.addBedOptionsIterator == null || this.addBedOptionsAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.addBedOptionsMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.addBedOptionsMInfo == null){
                throw new NullPointerException();
            }
            this.addBedOptionsAttribute = this.addBedOptionsMInfo.getCodeAttribute();
            this.addBedOptionsIterator = this.addBedOptionsAttribute.iterator();
        }
    }

    public CodeIterator getAddBedOptionsIterator(){return this.addBedOptionsIterator;}

    public CodeAttribute getAddBedOptionsAttribute(){return this.addBedOptionsAttribute;}

    public MethodInfo getAddBedOptionsMInfo(){return this.addBedOptionsMInfo;}

    public void setMoveToItem(ClassFile cf, String desc, String name) {
        if (this.moveToItemMInfo == null || this.moveToItemIterator == null || this.moveToItemAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.moveToItemMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.moveToItemMInfo == null){
                throw new NullPointerException();
            }
            this.moveToItemAttribute = this.moveToItemMInfo.getCodeAttribute();
            this.moveToItemIterator = this.moveToItemAttribute.iterator();
        }
    }

    public CodeIterator getMoveToItemIterator() {return this.moveToItemIterator;}

    public CodeAttribute getMoveToItemAttribute() {return this.moveToItemAttribute;}

    public MethodInfo getMoveToItemMInfo() {return this.moveToItemMInfo;}

    public void setLoadCargo(ClassFile cf, String desc, String name) {
        if (this.loadCargoMInfo == null || this.loadCargoIterator == null || this.loadCargoAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.loadCargoMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.loadCargoMInfo == null){
                throw new NullPointerException();
            }
            this.loadCargoAttribute = this.loadCargoMInfo.getCodeAttribute();
            this.loadCargoIterator = this.loadCargoAttribute.iterator();
        }
    }

    public CodeIterator getLoadCargoIterator() {return this.loadCargoIterator;}

    public CodeAttribute getLoadCargoAttribute() {return this.loadCargoAttribute;}

    public MethodInfo getLoadCargoMInfo() {return this.loadCargoMInfo;}

    public void setUnloadCargo(ClassFile cf, String desc, String name) {
        if (this.unloadCargoMInfo == null || this.unloadCargoIterator == null || this.unloadCargoAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.unloadCargoMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.unloadCargoMInfo == null){
                throw new NullPointerException();
            }
            this.unloadCargoAttribute = this.unloadCargoMInfo.getCodeAttribute();
            this.unloadCargoIterator = this.unloadCargoAttribute.iterator();
        }
    }

    public CodeIterator getUnloadCargoIterator() {return this.unloadCargoIterator;}

    public CodeAttribute getUnloadCargoAttribute() {return this.unloadCargoAttribute;}

    public MethodInfo getUnloadCargoMInfo() {return  this.unloadCargoMInfo;}

    public void setPerformerIsNotOnATransportVehicle(ClassFile cf, String desc, String name) {
        if (this.performerIsNotOnATransportVehicleMInfo == null || this.performerIsNotOnATransportVehicleIterator == null ||
                this.performerIsNotOnATransportVehicleAttribute == null) {            for (List a : new List[]{cf.getMethods()}){
            for(Object b : a){
                MethodInfo MInfo = (MethodInfo) b;
                if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                    this.performerIsNotOnATransportVehicleMInfo = MInfo;
                    break;
                }
            }
        }
            if (this.performerIsNotOnATransportVehicleMInfo == null){
                throw new NullPointerException();
            }
            this.performerIsNotOnATransportVehicleAttribute = this.performerIsNotOnATransportVehicleMInfo.getCodeAttribute();
            this.performerIsNotOnATransportVehicleIterator = this.performerIsNotOnATransportVehicleAttribute.iterator();
        }
    }

    public CodeIterator getPerformerIsNotOnATransportVehicleIterator() {return this.performerIsNotOnATransportVehicleIterator;}

    public CodeAttribute getPerformerIsNotOnATransportVehicleAttribute() {return this.performerIsNotOnATransportVehicleAttribute;}

    public MethodInfo getPerformerIsNotOnATransportVehicleMInfo() {return this.performerIsNotOnATransportVehicleMInfo;}

    public void setGetLoadUnloadActions(ClassFile cf, String desc, String name) {
        if (this.getLoadUnloadActionsMInfo == null || this.getLoadUnloadActionsIterator == null ||
                this.getLoadUnloadActionsAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
            for(Object b : a){
                MethodInfo MInfo = (MethodInfo) b;
                if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                    this.getLoadUnloadActionsMInfo = MInfo;
                    break;
                }
            }
        }
            if (this.getLoadUnloadActionsMInfo == null){
                throw new NullPointerException();
            }
            this.getLoadUnloadActionsAttribute = this.getLoadUnloadActionsMInfo.getCodeAttribute();
            this.getLoadUnloadActionsIterator = this.getLoadUnloadActionsAttribute.iterator();
        }
    }

    public CodeIterator getGetLoadUnloadActionsIterator() {return this.getLoadUnloadActionsIterator;}

    public CodeAttribute getGetLoadUnloadActionsAttribute() {return this.getLoadUnloadActionsAttribute;}

    public MethodInfo getGetLoadUnloadActionsMInfo() {return this.getLoadUnloadActionsMInfo;}

    public void setAskSleep(ClassFile cf, String desc, String name) {
        if (this.askSleepMInfo == null || this.askSleepIterator == null || this.askSleepAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.askSleepMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.askSleepMInfo == null){
                throw new NullPointerException();
            }
            this.askSleepAttribute = this.askSleepMInfo.getCodeAttribute();
            this.askSleepIterator = this.askSleepAttribute.iterator();
        }
    }

    public CodeIterator getAskSleepIterator() {return this.askSleepIterator;}

    public CodeAttribute getAskSleepAttribute() {return this.askSleepAttribute;}

    public MethodInfo getAskSleepMInfo() {return this.askSleepMInfo;}

    public void setSleep(ClassFile cf, String desc, String name) {
        if (this.sleepMInfo == null || this.sleepIterator == null || this.sleepAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.sleepMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.sleepMInfo == null){
                throw new NullPointerException();
            }
            this.sleepAttribute = this.sleepMInfo.getCodeAttribute();
            this.sleepIterator = this.sleepAttribute.iterator();
        }
    }

    public CodeIterator getSleepIterator() {return this.sleepIterator;}

    public CodeAttribute getSleepAttribute() {return this.sleepAttribute;}

    public MethodInfo getSleepMInfo() {return this.sleepMInfo;}

    public void setMayUseBed(ClassFile cf, String desc, String name){
        if (this.mayUseBedMInfo == null || this.mayUseBedIterator == null || this.mayUseBedAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.mayUseBedMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.mayUseBedMInfo == null){
                throw new NullPointerException();
            }
            this.mayUseBedAttribute = this.mayUseBedMInfo.getCodeAttribute();
            this.mayUseBedIterator = this.mayUseBedAttribute.iterator();
        }
    }

    public CodeIterator getMayUseBedIterator() {return this.mayUseBedIterator;}

    public CodeAttribute getMayUseBedAttribute() {return this.mayUseBedAttribute;}

    public MethodInfo getMayUseBedMInfo() { return this.mayUseBedMInfo;}

    public void setStartFire(ClassFile cf, String desc, String name){
        if (this.startFireMInfo == null || this.startFireIterator == null || this.startFireAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.startFireMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.startFireMInfo == null){
                throw new NullPointerException();
            }
            this.startFireAttribute = this.startFireMInfo.getCodeAttribute();
            this.startFireIterator = this.startFireAttribute.iterator();
        }
    }

    public CodeIterator getStartFireIterator() {return this.startFireIterator;}

    public CodeAttribute getStartFireAttribute() {return this.startFireAttribute;}

    public MethodInfo getStartFireMInfo() {return this.startFireMInfo;}

    public void setSetFire(ClassFile cf, String desc, String name){
        if (this.setFireMInfo == null || this.setFireIterator == null || this.setFireAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.setFireMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.setFireMInfo == null){
                throw new NullPointerException();
            }
            this.setFireAttribute = this.setFireMInfo.getCodeAttribute();
            this.setFireIterator = this.setFireAttribute.iterator();
        }
    }

    public CodeIterator getSetFireIterator() {return this.setFireIterator;}

    public CodeAttribute getSetFireAttribute() {return this.setFireAttribute;}

    public MethodInfo getSetFireMInfo() {return this.setFireMInfo;}

    public void setAddCreationWindowOption(ClassFile cf, String desc, String name){
        if (this.addCreationWindowOptionMInfo == null || this.addCreationWindowOptionIterator == null || this.addCreationWindowOptionAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.addCreationWindowOptionMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.addCreationWindowOptionMInfo == null){
                throw new NullPointerException();
            }
            this.addCreationWindowOptionAttribute = this.addCreationWindowOptionMInfo.getCodeAttribute();
            this.addCreationWindowOptionIterator = this.addCreationWindowOptionAttribute.iterator();
        }
    }

    public CodeIterator getAddCreationWindowOptionIterator() {return this.addCreationWindowOptionIterator;}

    public CodeAttribute getAddCreationWindowOptionAttribute() {return this.addCreationWindowOptionAttribute;}

    public MethodInfo getAddCreationWindowOptionMInfo() {return this.addCreationWindowOptionMInfo;}

}

