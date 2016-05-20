package com.Joedobo27.WUmod;

import com.sun.istack.internal.Nullable;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.effects.Effect;
import com.wurmonline.server.effects.EffectFactory;
import com.wurmonline.server.items.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


@SuppressWarnings({"unused", "WeakerAccess", "FieldCanBeLocal"})
public class LoadingUnchainedMod implements WurmServerMod, Initable, Configurable, ServerStartedListener {

    //<editor-fold desc="Configure controls.">
    private static boolean loadEmptyOrNotBin = true;
    private static boolean moveItemsIntoBinWithinContainer = false;
    private static boolean useCustomProximity = false;
    private static int loadProximityRequired = 4;
    private static boolean useCustomLoadTime = false;
    private static int loadDurationTime = 100;
    public static boolean loadIntoDragged = false;
    public static boolean boatInCart = false;
    private static boolean useBedInCart = false;
    private static boolean useMagicChestInCart = false;
    private static boolean useForgeInCart = false;
    private static boolean craftWithinCart = false;
    private static boolean loadAltar = false;
    private static boolean useAltarInCart = false;
    private static boolean loadOther = false;
    private static double minimumStrength = 23.0;
    private static boolean loadItemsNotOnGround = false;
    //</editor-fold>

    //<editor-fold desc="Bytecode objects">
    private static ClassPool pool;
    private static CtClass ctcSelf;
    private static CtClass ctcCargoTransportationMethods;
    private static ClassFile cfCargoTransportationMethods;
    private static ConstPool cpCargoTransportationMethods;
    private static CtClass ctcItem;
    private static ClassFile cfItem;
    private static ConstPool cpItem;
    private static CtClass ctcMethodsItems;
    private static ClassFile cfMethodsItems;
    private static ConstPool cpMethodsItems;
    private static CtClass ctcItemBehaviour;
    private static ClassFile cfItemBehaviour;
    private static ConstPool cpItemBehaviour;
    private static ClassFile cfMethodsReligion;
    private static ConstPool cpMethodsReligion;
    private static ClassFile cfDomainItemBehaviour;
    private static ConstPool cpDomainItemBehaviour;
    private static CtClass ctcActions;
    //</editor-fold>

    //<editor-fold desc="Javassist objects">
    private static CodeAttribute moveToItemAttribute;
    private static CodeIterator moveToItemIterator;
    private static MethodInfo moveToItemMInfo;

    private static CodeAttribute loadCargoAttribute;
    private static CodeIterator loadCargoIterator;
    private static MethodInfo loadCargoMInfo;

    private static CodeAttribute unloadCargoAttribute;
    private static CodeIterator unloadCargoIterator;
    private static MethodInfo unloadCargoMInfo;

    private static CodeAttribute performerIsNotOnATransportVehicleAttribute;
    private static CodeIterator performerIsNotOnATransportVehicleIterator;
    private static MethodInfo performerIsNotOnATransportVehicleMInfo;

    private static CodeAttribute getLoadUnloadActionsAttribute;
    private static CodeIterator getLoadUnloadActionsIterator;
    private static MethodInfo getLoadUnloadActionsMInfo;

    private static CodeAttribute addBedOptionsAttribute;
    private static CodeIterator addBedOptionsIterator;
    private static MethodInfo addBedOptionsMInfo;

    private static CodeAttribute askSleepAttribute;
    private static CodeIterator askSleepIterator;
    private static MethodInfo askSleepMInfo;

    private static CodeAttribute sleepAttribute;
    private static CodeIterator sleepIterator;
    private static MethodInfo sleepMInfo;

    private static CodeAttribute mayUseBedAttribute;
    private static CodeIterator mayUseBedIterator;
    private static MethodInfo mayUseBedMInfo;

    private static CodeAttribute startFireAttribute;
    private static CodeIterator startFireIterator;
    private static MethodInfo startFireMInfo;

    private static CodeAttribute setFireAttribute;
    private static CodeIterator setFireIterator;
    private static MethodInfo setFireMInfo;

    private static CodeAttribute addCreationWindowOptionAttribute;
    private static CodeIterator addCreationWindowOptionIterator;
    private static MethodInfo addCreationWindowOptionMInfo;

    private static CodeAttribute prayAttribute;
    private static CodeIterator prayIterator;
    private static MethodInfo prayMInfo;

    private static CodeAttribute sacrificeAttribute;
    private static CodeIterator sacrificeIterator;
    private static MethodInfo sacrificeMInfo;

    private static CodeAttribute actionDBAttribute;
    private static CodeIterator actionDBIterator;
    private static MethodInfo actionDBMInfo;

    private static CodeAttribute actionIBAttribute;
    private static CodeIterator actionIBIterator;
    private static MethodInfo actionIBMInfo;
    //</editor-fold>

    private static LoadingUnchainedMod instance;
    private static final long NULL_LONG = (long)-10;
    private static final Logger logger;
    static {
        logger = Logger.getLogger(LoadingUnchainedMod.class.getName());
        logger.setUseParentHandlers(false);
        for (Handler a : logger.getHandlers()) {
            logger.removeHandler(a);
        }
        logger.addHandler(aaaJoeCommon.joeFileHandler);
        logger.setLevel(Level.ALL);
    }

    public static LoadingUnchainedMod getInstance() {
        if (LoadingUnchainedMod.instance == null) {
            LoadingUnchainedMod.instance = new LoadingUnchainedMod();
        }
        return LoadingUnchainedMod.instance;
    }

    public void configure(Properties properties) {
        loadEmptyOrNotBin = Boolean.valueOf(properties.getProperty("loadEmptyOrNotBin", Boolean.toString(loadEmptyOrNotBin)));
        moveItemsIntoBinWithinContainer = Boolean.valueOf(properties.getProperty("moveItemsIntoBinWithinContainer",
                Boolean.toString(moveItemsIntoBinWithinContainer)));
        useCustomProximity = Boolean.valueOf(properties.getProperty("useCustomProximity", Boolean.toString(useCustomProximity)));
        loadProximityRequired = Integer.valueOf(properties.getProperty("loadProximityRequired", Integer.toString(loadProximityRequired)));
        loadProximityRequired = Math.max(loadProximityRequired, 1);
        loadProximityRequired = Math.min(loadProximityRequired, 160);
        useCustomLoadTime = Boolean.valueOf(properties.getProperty("useCustomLoadTime", Boolean.toString(useCustomLoadTime)));
        loadDurationTime = Integer.valueOf(properties.getProperty("loadDurationTime", Integer.toString(loadDurationTime)));
        loadIntoDragged = Boolean.valueOf(properties.getProperty("loadIntoDragged", Boolean.toString(loadIntoDragged)));
        boatInCart = Boolean.valueOf(properties.getProperty("boatInCart", Boolean.toString(boatInCart)));
        useBedInCart = Boolean.valueOf(properties.getProperty("useBedInCart", Boolean.toString(useBedInCart)));
        useMagicChestInCart = Boolean.valueOf(properties.getProperty("useMagicChestInCart", Boolean.toString(useMagicChestInCart)));
        useForgeInCart = Boolean.valueOf(properties.getProperty("useForgeInCart", Boolean.toString(useForgeInCart)));
        craftWithinCart = Boolean.valueOf(properties.getProperty("craftWithinCart", Boolean.toString(craftWithinCart)));
        loadAltar = Boolean.valueOf(properties.getProperty("loadAltar", Boolean.toString(loadAltar)));
        useAltarInCart = Boolean.valueOf(properties.getProperty("useAltarInCart", Boolean.toString(useAltarInCart)));
        loadOther = Boolean.valueOf(properties.getProperty("loadOther", Boolean.toString(loadOther)));
        minimumStrength = Double.valueOf(properties.getProperty("minimumStrength", Double.toString(minimumStrength)));
        loadItemsNotOnGround = Boolean.parseBoolean(properties.getProperty("loadItemsNotOnGround" , Boolean.toString(loadItemsNotOnGround)));

    }

    @Override
    public void onServerStarted() {
        try {
            int bedCnt = useBedReflection();
            int boatInCartCnt = boatInCartReflection();
            int altarCnt = loadAltarReflection();
            int otherCnt = loadOtherReflection();
            int craftWithinCartCnt = craftWithinCartReflection();

            logger.log(Level.INFO, "useBedInCart: " + bedCnt);
            logger.log(Level.INFO, "boatInCart: " + boatInCartCnt);
            logger.log(Level.INFO, "loadAltar: " + altarCnt);
            logger.log(Level.INFO, "loadOther: " + otherCnt);
            logger.log(Level.INFO, "craftWithinCart: " + craftWithinCartCnt);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void init() {
        try {
            pool = HookManager.getInstance().getClassPool();
            setJSSelf();
            setJSCargoTransportationMethods();
            setJSItem();
            setJSMethodsItems();
            setJSItemBehaviour();
            setJSMethodsReligion();
            setJSDomainItemBehaviour();
            setJSActions();

            loadFromWithinContainersBytecode();
            loadCargoBytecode();
            loadEmptyOrNotBinBytecode();
            moveItemsIntoBinWithinContainerBytecode();
            useCustomLoadTimeBytecode();
            useCustomProximityBytecode();
            loadIntoDraggedBytecode();
            menuAddLoadUnloadBytecode();
            useBedInCartBytecode();
            useMagicChestInCartBytecode();
            useForgeInCartBytecode();
            craftWithinCartBytecode();
            useAltarInCartBytecode();
        } catch (BadBytecode | NotFoundException | CannotCompileException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is a reference method used by ctmSetFire to copy and replace the same method in WU.
     * @param performer is type Creature and is the entity doing the action.
     * @param target is type Item and is the item where fire is to be set.
     * @return success indicator. The modded equivalency of this is that it always succeeds.
     */
    static boolean setFire(final Creature performer, final Item target) {
        if (target.getTemplateId() == 178) {
            target.setTemperature((short)6000);
        }
        else {
            target.setTemperature((short)10000);
        }
        if (target.getTopParent() == target.getWurmId()) {
            final Effect effect = EffectFactory.getInstance().createFire(target.getWurmId(), target.getPosX(), target.getPosY(), target.getPosZ(), performer.isOnSurface());
            target.addEffect(effect);
        }
        return true;
    }

    public static Item embarkedVehicleToItemSafe(Creature pilot) {
        try {
            if (pilot.getVehicle() != -10L)
                return Items.getItem(pilot.getVehicle());
        } catch (NoSuchItemException ignored) {}
        return null;
    }

    public static Item vehicleToItemSafe(long wurmId){
        Item i = null;
        try {
            i =  Items.getItem(wurmId) != null ? Items.getItem(wurmId) : null;
        } catch (NoSuchItemException ignored) {}
        return i;
    }

    public static long getVehicleFromDragEmbark(Creature player){
        long vehicleEmbark;
        long vehicleDragged;
        long toReturn = -10L;

        vehicleEmbark = player.getVehicle();
        if (loadIntoDragged) {
            Item vehicleDraggingItem = player.getDraggedItem();
            vehicleDragged = vehicleDraggingItem != null ? vehicleDraggingItem.getWurmId() : -10L;
            if (vehicleDragged == -10L && vehicleEmbark == -10L) {
                toReturn = -10L;
            } else if (vehicleDragged == -10L) {
                toReturn = vehicleEmbark;
            } else {
                toReturn = vehicleDragged;
            }
        } else {
            if (vehicleEmbark == -10L) {
                toReturn = -10L;
            } else {
                toReturn = vehicleEmbark;
            }
        }
        return toReturn;
    }

    public static boolean loadIntoContainerInvalid(Vehicle vehicle) {
        return Objects.equals(null, vehicle) || vehicle.creature || vehicle.isChair();
    }

    public static List<ActionEntry> getLoadUnloadActions(final Creature player, final Item target) {
        List<ActionEntry> toReturn = new LinkedList<>();
        Vehicle vehicleEmbark;
        Item vehicleItemEmbark;
        Vehicle vehicleDragged;
        Item vehicleItemDragged;
        Vehicle vehicle;
        Item vehicleItem;

        // Target qualifiers.
        if (!target.getTemplate().isTransportable() && !target.isBoat() && !target.isUnfinished()) {
            return toReturn;}
        //Vehicles use mode.
        long vehicleLong = getVehicleFromDragEmbark(player);
        vehicle = vehicleLong != -10L ? Vehicles.getVehicleForId(vehicleLong) : null;
        vehicleItem = vehicle != null ? vehicleToItemSafe(vehicle.wurmid) : null;
        // Unload
        if (target.getTopParent() == -10L || target.getWurmId() == -10L) {return toReturn;}
        if (target.getTopParent() != target.getWurmId()) {
            vehicle = Vehicles.getVehicleForId(target.getTopParent());
            if (vehicle != null) {
                Item targetParentVehicleItem = vehicleToItemSafe(vehicle.wurmid);
                if (!vehicle.creature && (MethodsItems.mayUseInventoryOfVehicle(player, targetParentVehicleItem) ||
                        targetParentVehicleItem.getLockId() == -10L || Items.isItemDragged(targetParentVehicleItem))) {
                    toReturn.add(Actions.actionEntrys[606]);
                }
            }
        }
        // load
        if (loadIntoContainerInvalid(vehicle) || vehicleItem == null){return toReturn;}
        ItemTemplate template = target.isUnfinished() ? target.getRealTemplate() : null;
        if (!boatInCart) {
            if (target.isBoat() && vehicleItem.getTemplateId() != 853) {return toReturn;}
            if (target.isUnfinished()) {
                if (template == null || !template.isTransportable() || (template.isBoat() && vehicleItem.getTemplateId() != 853)) {
                    return toReturn;
                }
            }
        } else {
            if (target.isUnfinished() && (template == null || !template.isTransportable())) {
                return toReturn;
            }
        }
        if (MethodsItems.mayUseInventoryOfVehicle(player, vehicleItem) || vehicleItem.getLockId() == -10L ||
        Items.isItemDragged(vehicleItem)) {
        toReturn.add(Actions.actionEntrys[605]);}
         return toReturn;
    }

     //<editor-fold desc="Javassist and bytecode altering section.">
    private void setJSSelf() throws NotFoundException {
        ctcSelf = pool.get(this.getClass().getName());
    }

    private void setJSCargoTransportationMethods() throws NotFoundException {
        ctcCargoTransportationMethods = pool.get("com.wurmonline.server.behaviours.CargoTransportationMethods");
        cfCargoTransportationMethods = ctcCargoTransportationMethods.getClassFile();
        cpCargoTransportationMethods = cfCargoTransportationMethods.getConstPool();
    }

    private void setJSItem() throws NotFoundException {
        ctcItem = pool.get("com.wurmonline.server.items.Item");
        cfItem = ctcItem.getClassFile();
        cpItem = cfItem.getConstPool();
    }

    private void setJSMethodsItems() throws NotFoundException {
        ctcMethodsItems = pool.get("com.wurmonline.server.behaviours.MethodsItems");
        cfMethodsItems = ctcMethodsItems.getClassFile();
        cpMethodsItems = cfMethodsItems.getConstPool();
    }

    private void setJSItemBehaviour() throws NotFoundException {
        ctcItemBehaviour = pool.get("com.wurmonline.server.behaviours.ItemBehaviour");
        cfItemBehaviour = ctcItemBehaviour.getClassFile();
        cpItemBehaviour = cfItemBehaviour.getConstPool();
    }

    private void setJSMethodsReligion() throws NotFoundException {
        CtClass ctcMethodsReligion = pool.get("com.wurmonline.server.behaviours.MethodsReligion");
        cfMethodsReligion = ctcMethodsReligion.getClassFile();
        cpMethodsReligion = cfMethodsReligion.getConstPool();
    }

    private void setJSDomainItemBehaviour() throws NotFoundException {
        CtClass ctcDomainItemBehaviour = pool.get("com.wurmonline.server.behaviours.DomainItemBehaviour");
        cfDomainItemBehaviour = ctcDomainItemBehaviour.getClassFile();
        cpDomainItemBehaviour = cfDomainItemBehaviour.getConstPool();
    }

    private void setJSActions() throws NotFoundException {
        ctcActions = pool.get("com.wurmonline.server.behaviours.Actions");
    }

    private void loadFromWithinContainersBytecode() throws NotFoundException, CannotCompileException {
        if (!loadItemsNotOnGround)
            return;
        CtMethod cmTargetIsNotOnTheGround = ctcCargoTransportationMethods.getMethod("targetIsNotOnTheGround",
                "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Z)Z");
        cmTargetIsNotOnTheGround.setBody("return false;");

        String s = "public com.wurmonline.server.items.Item removeItemA(long _id, boolean setPosition, boolean ignoreWatchers, boolean skipPileRemoval)";
        s += "{com.wurmonline.server.items.Item a = this.removeItem(_id, setPosition, ignoreWatchers, skipPileRemoval);return a;}";
        s += "catch (com.wurmonline.server.NoSuchItemException ignore){}return null;";
        CtMethod m = CtNewMethod.make(s, ctcItem);
        ctcItem.addMethod(m);
    }

    private void loadCargoBytecode() throws NotFoundException, CannotCompileException {
        if (!loadItemsNotOnGround && !useForgeInCart)
            return;
        String s;
        CtMethod ctmLoadCargo = ctcCargoTransportationMethods.getMethod("loadCargo",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
        if (loadItemsNotOnGround && useForgeInCart) {
            s = "if (target.getWurmId() != target.getTopParent()){";
            s += "try{com.wurmonline.server.items.Item carrierItem = com.wurmonline.server.Items.getItem(target.getTopParent());";
            s += "carrierItem.removeItemA(target.getWurmId(), false, false, false);}catch(com.wurmonline.server.NoSuchItemException ignore){};}";
            // insert this new method in load code...parentItem.removeItemA(targetItemLong, false, false, false);
            s += "if ($2.isOnFire()) {";
            s += "com.wurmonline.server.zones.Zone zone1 = ";
            s += "com.wurmonline.server.zones.Zones.getZone($1.getTileX(), $1.getTileY(), $1.isOnSurface());";
            s += "com.wurmonline.server.effects.Effect[] targetEffects = $2.getEffects();";
            s += "for (int i = 0; i < targetEffects.length; i++) {";
            s += "if (targetEffects[i].getType() == (short)0) {";
            s += "zone1.removeEffect(targetEffects[i]);}}}";
            // Alter loadCargo() of cargoTransportationMethods.class to destory fire effect on furnace load.
            // Insert code right before "if (targetIsNotOnTheGround(target, performer, true)) ".
            ctmLoadCargo.insertAt(272, "{" + s + "}");
        }
        if (!loadItemsNotOnGround && useForgeInCart) {
            s = "if ($2.isOnFire()) {";
            s += "com.wurmonline.server.zones.Zone zone1 = ";
            s += "com.wurmonline.server.zones.Zones.getZone($1.getTileX(), $1.getTileY(), $1.isOnSurface());";
            s += "com.wurmonline.server.effects.Effect[] targetEffects = $2.getEffects();";
            s += "for (int i = 0; i < targetEffects.length; i++) {";
            s += "if (targetEffects[i].getType() == (short)0) {";
            s += "zone1.removeEffect(targetEffects[i]);}}}";
            // Alter loadCargo() of cargoTransportationMethods.class to destory fire effect on furnace load.
            // Insert code right before "if (targetIsNotOnTheGround(target, performer, true)) ".
            ctmLoadCargo.insertAt(272, "{" + s + "}");
        }
        if (loadItemsNotOnGround && !useForgeInCart) {
            s = "if (target.getWurmId() != target.getTopParent()){";
            s += "try{com.wurmonline.server.items.Item carrierItem = com.wurmonline.server.Items.getItem(target.getTopParent());";
            s += "carrierItem.removeItemA(target.getWurmId(), false, false, false);}catch(com.wurmonline.server.NoSuchItemException ignore){};}";
            // insert this new method in load code...parentItem.removeItemA(targetItemLong, false, false, false);
            // Insert code right before "if (targetIsNotOnTheGround(target, performer, true)) ".
            ctmLoadCargo.insertAt(272, "{" + s + "}");
        }

    }

    private void loadEmptyOrNotBinBytecode() throws NotFoundException, CannotCompileException {
        if (!loadEmptyOrNotBin)
            return;
        // Alter targetIsNotEmptyContainerCheck() of cargoTransportationMethods.class to always return false.
        CtMethod ctmTargetIsNotEmptyContainerCheck = ctcCargoTransportationMethods.getMethod("targetIsNotEmptyContainerCheck",
                "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z");
        ctmTargetIsNotEmptyContainerCheck.setBody("return false;");
    }

    private void moveItemsIntoBinWithinContainerBytecode() throws BadBytecode {
        if (!moveItemsIntoBinWithinContainer)
            return;
        JDBByteCode jbt;
        JDBByteCode replace;
        String replaceByteResult;

        //<editor-fold desc="moveToItem() of Item.class changes">
            /*
            Lines 3990 to 4046 in moveToItem of Javap.exe dump.
            Removed-
                if (target.getTopParent() != target.getWurmId() && !target.isCrate()) {
                    if (mover != null) {
                        final String message = StringUtil.format("The %s needs to be on the ground.", target.getName());
                        mover.getCommunicator().sendNormalServerMessage(message);
                    }
                    return false;
                }
            */
        //</editor-fold>
        setMoveToItem(cfItem, "(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.IFNULL, Opcode.LDC_W,
                Opcode.ICONST_1, Opcode.ANEWARRAY, Opcode.DUP, Opcode.ICONST_0, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.AASTORE,
                Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.ICONST_0, Opcode.IRETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("08",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method getTopParent:()J"),
                "08",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method getWurmId:()J"),
                "", "002e", "08",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method isCrate:()Z"),
                "0026", "", "0020",
                JDBByteCode.findConstantPoolReference(cpItem, "// String The %s needs to be on the ground."),
                "",
                JDBByteCode.findConstantPoolReference(cpItem, "// class java/lang/Object"),
                "", "", "08",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method getName:()Ljava/lang/String;"),
                "",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method com/wurmonline/server/utils/StringUtil.format:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
                "13", "",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                "13",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "", "")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "0000,000000,0000,000000,00,000000,0000,000000,000000,00,000000,000000,00,000000,00,00,0000,000000,00,000000,0000,00,000000,0000,000000,00,00",
                moveToItemIterator, "moveToItem");
        logger.log(Level.INFO, replaceByteResult);
    }

    private void useCustomProximityBytecode() throws  NotFoundException, CannotCompileException {
        if (!useCustomProximity)
            return;
        // alter getLoadActionDistance() of  cargoTransportationMethods.class to return properties value in loadProximityRequired.
        CtMethod ctmGetLoadActionDistance = ctcCargoTransportationMethods.getMethod("getLoadActionDistance",
                "(Lcom/wurmonline/server/items/Item;)I");
        ctmGetLoadActionDistance.setBody("return " + loadProximityRequired + ";");
        logger.log(Level.INFO, "Custom loading proximity requirement set.");
    }

    private void useCustomLoadTimeBytecode()throws NotFoundException, CannotCompileException {
        if (!useCustomLoadTime)
            return;
        // alter getLoadUnloadActionTime() of Actions.class to always return the properties value in loadDurationTime.
        CtMethod cmGetLoadUnloadActionTime = ctcActions.getMethod("getLoadUnloadActionTime", "(Lcom/wurmonline/server/creatures/Creature;)I");
        cmGetLoadUnloadActionTime.setBody("return " + loadDurationTime + ";");
        logger.log(Level.INFO, "Custom load time set.");
    }

    private void loadIntoDraggedBytecode() throws BadBytecode, NotFoundException, CannotCompileException, FileNotFoundException {
        if (!loadIntoDragged)
            return;
        JDBByteCode find;
        JDBByteCode replace;
        String replaceByteResult;

        // performer.getVehicle() is a problem. It's used in more then just load cargo applications so it can't be overwritten.
        // Make a new method: getVehicleDragOrEmbark() in Player.class and have it by default return this.getVehicle()
        // ( wrap the getVehicle() and have the new method return the same as unwrapped by default). Next, use bytecode
        // to replace "performer.getVehicle()" with "performer.getVehicleDragOrEmbark()" for loading situations in ItemBehaviour.class.
        // Finally, use ExprEditor to hook into getVehicleDragOrEmbark() and redirect the return value derived from a hooked
        // method in mod.
        CtMethod m = CtNewMethod.make(
                "public long getVehicleDragOrEmbark(){return $0.getVehicle();}",
                pool.getCtClass("com.wurmonline.server.players.Player"));
        pool.getCtClass("com.wurmonline.server.players.Player").addMethod(m);

        //<editor-fold desc="changed action(Action,Creation,Item,Short,Float) in ItemBehaviour.class">
        /*
            -starting on line 3387 to 3402

            -was
            if (performer.getVehicle() == -10L) {
                    break Label_14818;
                }
                try {
                    final Item vehicle2 = Items.getItem(performer.getVehicle());
                    if (vehicle2.getTemplateId() != 853) {
            -becomes
            replace all performer.getVehicle() with performer.getVehicleDragOrEmbark().
        */
        //</editor-fold>
        setActionIB(cfItemBehaviour,
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;SF)Z",
                "action");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.IRETURN, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W,
                Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/creatures/Creature.getVehicle:()J"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// long -10l"),
                "", "2C9F","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/creatures/Creature.getVehicle:()J"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/Items.getItem:(J)Lcom/wurmonline/server/items/Item;"))));
        find.setOpcodeOperand();
        replace = new JDBByteCode();
        replace.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.IRETURN, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W,
                Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC)));
        replace.setOperandStructure(new ArrayList<>(Arrays.asList("","",
                JDBByteCode.addConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/players/Player.getVehicleDragOrEmbark:()J"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// long -10l"),
                "", "2C9F","",
                JDBByteCode.addConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/players/Player.getVehicleDragOrEmbark:()J"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/Items.getItem:(J)Lcom/wurmonline/server/items/Item;"))));
        replace.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(), replace.getOpcodeOperand(), actionIBIterator,
                "action_ItemBehaviour");
        logger.log(Level.INFO, replaceByteResult);


        CtMethod ctmActionIB = ctcItemBehaviour.getMethod("action",
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;SF)Z");
        ctmActionIB.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals(methodCall.getMethodName(), "getVehicleDragOrEmbark")){
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getVehicleFromDragEmbark(performer);");
                }
            }
        });

        CtMethod ctmLoadCargo = ctcCargoTransportationMethods.getMethod("loadCargo", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
        ctmLoadCargo.instrument( new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals(methodCall.getMethodName(), "getVehicle")) {
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getVehicleFromDragEmbark(performer);");
                }
                else if (Objects.equals(methodCall.getMethodName(), "performerIsNotOnATransportVehicle")) {
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.loadIntoContainerInvalid(vehicle);");
                }
            }
        });

        CtMethod ctmLoadShip = ctcCargoTransportationMethods.getMethod("loadShip", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
        ctmLoadShip.instrument( new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals(methodCall.getMethodName(), "getVehicle")) {
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getVehicleFromDragEmbark(performer);");
                }
                else if (Objects.equals(methodCall.getMethodName(), "performerIsNotOnATransportVehicle")) {
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.loadIntoContainerInvalid(vehicle);");
                }
            }
        });
        // performerIsNotOnAVehicle() of cargoTransportationMethods always return false.
        CtMethod ctmPerformerIsNotOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotOnAVehicle",
                "(Lcom/wurmonline/server/creatures/Creature;)Z");
        ctmPerformerIsNotOnAVehicle.setBody("return false;");

        // performerIsNotSeatedOnAVehicle() of cargoTransportationMethods always return false.
        CtMethod ctmPerformerIsNotSeatedOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotSeatedOnAVehicle",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Seat;)Z");
        ctmPerformerIsNotSeatedOnAVehicle.setBody("return false;");
    }

    public static void printMethodCall(Object[] args, @Nullable Object zero, @Nullable boolean rtn, @Nullable Object a, @Nullable Object b, @Nullable Object c){
        for (Object arg : args) {
            logger.log(Level.FINE, "print args: " + arg.toString());
        }
        logger.log(Level.FINE, zero == null ? "$0 is null" : "$0 is: " + zero.toString());
        logger.log(Level.FINE, "return is: " + Boolean.toString(rtn));
        logger.log(Level.FINE, a == null ? "performer is null" : "vehicle: " + a.toString());
        logger.log(Level.FINE, b == null ? "vehicle is null" : "target: " + b.toString());
        logger.log(Level.FINE, c == null ? "target is null" : "target: " + c.toString());
    }

    @SuppressWarnings("ConstantConditions")
    private void menuAddLoadUnloadBytecode() throws NotFoundException, CannotCompileException {
        if (!boatInCart && !loadIntoDragged)
            return;
        pool.get("com.wurmonline.server.behaviours.ItemBehaviour").instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (Objects.equals(ctcCargoTransportationMethods.getName(), m.getClassName()) && Objects.equals("getLoadUnloadActions", m.getMethodName())
                && Objects.equals("(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Ljava/util/List;", m.getSignature())) {
                    m.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getLoadUnloadActions($1, $2);");
                    logger.log(Level.INFO, "replaced getLoadUnloadActions()");
                }
            }
        });
    }

    private void useBedInCartBytecode() throws BadBytecode, FileNotFoundException {
        if (!useBedInCart)
            return;
        JDBByteCode find;
        JDBByteCode replace;
        String replaceByteResult;

        //<editor-fold desc="addBedOptions() of ItemBehaviour.class changes.">
            /*
            Working with lines 17-35 in addBedOptions of the javap.exe dump.

            Added "&& target.getTopParent() == target.getWurmId()" to the logic statement:
            "if (t != null && t.getStructure() != null && t.getStructure().isTypeHouse()".
            If the bed is on ground and in a house use renting.
            */
        //</editor-fold>
        setAddBedOptions(cfItemBehaviour,
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Ljava/util/List;)V", "addBedOptions");
        addBedOptionsIterator.insertGap(41, 12);
        addBedOptionsIterator.insertExGap(376, 18);
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("04", "0177","04",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "016f","04",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "0164", "", "", "", "", "", "", "", "", "", "", "", "")));
        find.setOpcodeOperand();

        replace = new JDBByteCode();
        replace.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LCMP, Opcode.IFNE)));
        replace.setOperandStructure(new ArrayList<>(Arrays.asList("04", "0165","04",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "015d","04",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "0152","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// Method com/wurmonline/server/items/Item.getTopParent:()J"),
                "",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// Method com/wurmonline/server/items/Item.getWurmId:()J"),
                "","0146")));
        replace.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace( find.getOpcodeOperand(), find.getOpcodeOperand(),
                replace.getOpcodeOperand(), addBedOptionsIterator, "addBedOptions");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="addBedOptions() of ItemBehaviour.class changes.">
            /*
            Working with lines 358-361 in addBedOptions of the javap.exe dump.
            Added-
                } else {
                    toReturn.add(Actions.actionEntrys[325]);
                }
            This else block is a complement to the if block:
            "if (t != null && t.getStructure() != null && t.getStructure().isTypeHouse()"
            When the bed is not on the ground and not in a house it can't be controlled so use free sleep.
            */
        //</editor-fold>
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.INVOKEVIRTUAL, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.RETURN)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList(
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// Method java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/Throwable;)V"),
                "","","","","","","","","","","","","","","","","","","")));
        find.setOpcodeOperand();
        replace = new JDBByteCode();
        replace.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GOTO, Opcode.ALOAD_3, Opcode.GETSTATIC, Opcode.SIPUSH,
                Opcode.AALOAD, Opcode.INVOKEINTERFACE, Opcode.NOP, Opcode.POP)));
        replace.setOperandStructure(new ArrayList<>(Arrays.asList("0012","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// Field com/wurmonline/server/behaviours/Actions.actionEntrys:[Lcom/wurmonline/server/behaviours/ActionEntry;"),
                "0145","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,"// InterfaceMethod java/util/List.add:(Ljava/lang/Object;)Z") + "0200",
                "","")));
        replace.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), "00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00",
                replace.getOpcodeOperand(), addBedOptionsIterator, "addBedOptions");
        logger.log(Level.INFO, replaceByteResult);
        //getAddBedOptionsAttribute().computeMaxStack();
        addBedOptionsMInfo.rebuildStackMapIf6(pool, cfItemBehaviour);

        //<editor-fold desc="askSleep() of MethodsItems.class changes">
            /*
            Working with lines 108 - 180 in askSleep of javap.exe dump.
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
            */
        //</editor-fold>
        setAskSleep(cfMethodsItems, "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                "askSleep");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD,
                Opcode.INVOKEVIRTUAL, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1,
                Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/items/Item.getTileX:()I"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/items/Item.getTileY:()I"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/items/Item.isOnSurface:()Z"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/zones/Zones.getTileOrNull:(IIZ)Lcom/wurmonline/server/zones/VolaTile;"),
                "05","05","00bf","05",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "000e","05",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// String You would get no sleep outside tonight."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","","05",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/structures/Structure.isFinished:()Z"),
                "000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// String The house is too windy to provide protection."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "00,000000,00,000000,00,000000,000000,0000,0000,000000,0000,000000,000000,0000,000000,000000,000000,00,000000,000000,000000,00,00,0000,000000,000000,000000,00,000000,000000,000000,00,00",
                askSleepIterator, "askSleep");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="askSleep() of MethodsItems.class changes">
            /*
            Removing indexes 318 - 379 in askSleep of javap.exe dump.
            Removed-
                else {
                    MethodsItems.logger.log(Level.WARNING, "Why is tile for bed at " + target.getTileX() + "," + target.getTileY() + "," + target.isOnSurface() + " null?");
                }
            */
        //</editor-fold>
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GETSTATIC, Opcode.GETSTATIC, Opcode.NEW, Opcode.DUP, Opcode.INVOKESPECIAL, Opcode.LDC_W,
                Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2,
                Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList(
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Field logger:Ljava/util/logging/Logger;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Field java/util/logging/Level.WARNING:Ljava/util/logging/Level;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// class java/lang/StringBuilder"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.\"<init>\":()V"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String Why is tile for bed at "),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getTileX:()I"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String ,"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getTileY:()I"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String ,"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.isOnSurface:()Z"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Z)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String  null?"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.toString:()Ljava/lang/String;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;)V")
        )));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "000000,000000,000000,00,000000,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,000000,000000",
                askSleepIterator, "askSleep");
        logger.log(Level.INFO, replaceByteResult);
        //getAskSleepAttribute().computeMaxStack();
        askSleepMInfo.rebuildStackMapIf6(pool, cfMethodsItems);

        //<editor-fold desc="sleep() of MethodsItems.class changes.">
            /*
            Removing indexes 48 - 71 in sleep of javap.exe dump.
            Removed-
                if (target.getTopParent() != target.getWurmId()) {
                    performer.getCommunicator().sendNormalServerMessage("The bed needs to be on the ground.");
                    return true;
                }
            */
        //</editor-fold>
        setSleep(cfMethodsItems, "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                "sleep");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getTopParent:()J"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getWurmId:()J"),
                "","000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String The bed needs to be on the ground."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "00,000000,00,000000,00,000000,00,000000,000000,000000,00,00",
                sleepIterator, "sleep");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="sleep() of MethodsItems.class changes.">
            /*
            Removing indexes 173 - 226 in sleep of javap.exe dump.
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
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                Opcode.IRETURN, Opcode.ALOAD,Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("06",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "000e","06",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String You would get no sleep outside tonight."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","","06",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/structures/Structure.isFinished:()Z"),
                "000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String The house is too windy to provide protection."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "0000,000000,000000,0000,000000,000000,000000,00,000000,000000,000000,00,00,0000,000000,000000,000000,00,000000,000000,000000,00,00",
                sleepIterator, "sleep");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="sleep() of MethodsItems.class changes.">
            /*
            Removing indexes 302 - 363 in sleep of javap.exe dump.
            -Removed
                else {
                    MethodsItems.logger.log(Level.WARNING, "Why is tile for bed at " + target.getTileX() + "," + target.getTileY() + "," + target.isOnSurface() + " null?");
                }
            */
        //</editor-fold>
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GETSTATIC, Opcode.GETSTATIC, Opcode.NEW, Opcode.DUP, Opcode.INVOKESPECIAL, Opcode.LDC_W,
                Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2,
                Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList(
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Field logger:Ljava/util/logging/Logger;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Field java/util/logging/Level.WARNING:Ljava/util/logging/Level;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// class java/lang/StringBuilder"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.\"<init>\":()V"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String Why is tile for bed at "),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getTileX:()I"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String ,"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getTileY:()I"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String ,"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.isOnSurface:()Z"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Z)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// String  null?"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/lang/StringBuilder.toString:()Ljava/lang/String;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;)V")
        )));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "000000,000000,000000,00,000000,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,000000,000000",
                sleepIterator, "sleep");
        logger.log(Level.INFO, replaceByteResult);
        //getSleepAttribute().computeMaxStack();
        sleepMInfo.rebuildStackMapIf6(pool,cfMethodsItems);

        //<editor-fold desc="mayUseBed() of Item.class changes.">
            /*
            Removing indexes 10 - 55 in mayUseBed of javap.exe dump.
            -Removed
                final VolaTile vt = Zones.getTileOrNull(this.getTilePos(), this.isOnSurface());
                final Structure structure = (vt != null) ? vt.getStructure() : null;
                return structure != null && structure.isTypeHouse() && structure.isFinished();

            -Always return true for this block "(!ItemSettings.exists(this.getWurmId())"
            */
        //</editor-fold>
        setMayUseBed(cfItem, "(Lcom/wurmonline/server/creatures/Creature;)Z", "mayUseBed");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC,
                Opcode.ASTORE_2, Opcode.ALOAD_2, Opcode.IFNULL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ACONST_NULL, Opcode.ASTORE_3,
                Opcode.ALOAD_3, Opcode.IFNULL, Opcode.ALOAD_3, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD_3, Opcode.INVOKEVIRTUAL, Opcode.IFEQ,
                Opcode.ICONST_1, Opcode.GOTO, Opcode.ICONST_0)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method getTilePos:()Lcom/wurmonline/math/TilePos;"),
                "",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method isOnSurface:()Z"),
                JDBByteCode.findConstantPoolReference(cpItem, "// Method com/wurmonline/server/zones/Zones.getTileOrNull:(Lcom/wurmonline/math/TilePos;Z)Lcom/wurmonline/server/zones/VolaTile;"),
                "","","000a","",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "0004","","","","0015","",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "000e","",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method com/wurmonline/server/structures/Structure.isFinished:()Z"),
                "0007","","0004","")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "00,000000,00,000000,000000,00,00,000000,00,000000,000000,00,00,00,000000,00,000000,000000,00,000000,000000,00,000000,04",
                mayUseBedIterator, "mayUseBed");
        logger.log(Level.INFO, replaceByteResult);
        //getMayUseBedAttribute().computeMaxStack();
        mayUseBedMInfo.rebuildStackMapIf6(pool, cfItem);
    }

    private void useMagicChestInCartBytecode() throws BadBytecode {
        if (!useMagicChestInCart)
            return;
        JDBByteCode find;
        JDBByteCode replace;
        String replaceByteResult;

        //<editor-fold desc="moveToItem() of Item.class changes.">
            /*
            Removing indexes 58 - 69 in moveToItem of javap.exe dump.
            Removed-
                target.getTemplateId() == 664 ||
            */
        //</editor-fold>
        setMoveToItem(cfItem, "(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.SIPUSH, Opcode.IF_ICMPEQ)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("08",
                JDBByteCode.findConstantPoolReference(cpItem, "// Method getTemplateId:()I"),
                "0298","000b")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "0000,000000,000000,000000",
                moveToItemIterator, "moveToItem");
        logger.log(Level.INFO, replaceByteResult);
    }

    private void useForgeInCartBytecode() throws BadBytecode, NotFoundException, CannotCompileException {
        if (!useForgeInCart)
            return;
        JDBByteCode find;
        JDBByteCode replace;
        String replaceByteResult;


        //<editor-fold desc="startFire() in MethodsItems.class">
            /*
            Removing indexes 113 - 165 in startFire of javap.exe dump.
            Removed-
                if (target.getTemplate().isTransportable() && target.getTopParent() != target.getWurmId()) {
                    final String message = StringUtil.format("The %s must be on the ground before you can light it.", target.getName());
                    performer.getCommunicator().sendNormalServerMessage(message, (byte)3);
                    return true;
                }
            */
        //</editor-fold>
        setStartFire(cfMethodsItems, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;F)Z", "startFire");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LCMP, Opcode.IFEQ, Opcode.LDC_W,
                Opcode.ICONST_1, Opcode.ANEWARRAY, Opcode.DUP, Opcode.ICONST_0, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.AASTORE,
                Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.ICONST_3, Opcode.INVOKEVIRTUAL,
                Opcode.ICONST_1, Opcode.IRETURN)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getTemplate:()Lcom/wurmonline/server/items/ItemTemplate;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/ItemTemplate.isTransportable:()Z"),
                "002e","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getTopParent:()J"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getWurmId:()J"),
                "","0022",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,"// String The %s must be on the ground before you can light it."),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// class java/lang/Object"),
                "","","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/items/Item.getName:()Ljava/lang/String;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/utils/StringUtil.format:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
                "09","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,  "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                "09","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;B)V"),
                "","")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "00,000000,000000,000000,00,000000,00,000000,00,000000,000000,00,000000,00,00,00,000000,00,000000,0000,00,000000,0000,00,000000,00,00",
                startFireIterator, "startFire");
        logger.log(Level.INFO, replaceByteResult);
        //getStartFireAttribute().computeMaxStack();
        startFireMInfo.rebuildStackMapIf6(pool, cfMethodsItems);

        //<editor-fold desc="setFire() changes">
            /*
            Get the setFire() method in this mod's class, copy it, and paste it over the setFire() in MethodsItems.class.
             */
        //</editor-fold>
        setSetFire(cfMethodsItems, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z", "setFire");
        CtMethod ctmSetFire1 = ctcSelf.getMethod("setFire", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z");
        CtMethod ctmSetFire = ctcMethodsItems.getMethod("setFire", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z");
        ctmSetFire.setBody(ctmSetFire1, null);

        // Change targetIsOnFireCheck() so it always returns false.
        CtMethod ctmTargetIsOnFireCheck = ctcCargoTransportationMethods.getDeclaredMethod("targetIsOnFireCheck");
        ctmTargetIsOnFireCheck.setBody("return false;");

        // Alter unloadCargo() of cargoTransportationMethods.class to create a fire effect on furnace unload.
        CtMethod ctmUnloadCargo = ctcCargoTransportationMethods.getMethod("unloadCargo",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
        String s = "if ($2.isOnFire()) {";
        s = s.concat("com.wurmonline.server.zones.Zone zone1 = com.wurmonline.server.zones.Zones#getZone($1.getTileX(), $1.getTileY(), $1.isOnSurface());");
        s = s.concat("com.wurmonline.server.effects.Effect[] targetEffects = $2.getEffects();");
        s = s.concat("boolean fire = false;");
        s = s.concat("if (targetEffects.length > 0) {");
        s = s.concat("for (int i = 0; i < targetEffects.length; i++) {");
        s = s.concat("if (targetEffects[i].getType() == (short)0){");
        s = s.concat("fire = true;}}");
        s = s.concat("if (fire) {");
        s = s.concat("for (int i = 0; i < targetEffects.length; i++) {");
        s = s.concat("targetEffects[i].setPosX($1.getPosX());");
        s = s.concat("targetEffects[i].setPosY($1.getPosY());");
        s = s.concat("targetEffects[i].setPosZ($2.getPosZ());");
        s = s.concat("zone1.addEffect(targetEffects[i]);}}");
        s = s.concat("else { com.wurmonline.server.effects.Effect effect = ");
        s = s.concat("com.wurmonline.server.effects.EffectFactory#getInstance().createFire($2.getWurmId(), $2.getPosX(), $2.getPosY(), $2.getPosZ(), $1.isOnSurface());");
        s = s.concat("$2.addEffect(effect);}}");
        s = s.concat("else { com.wurmonline.server.effects.Effect effect = ");
        s = s.concat("com.wurmonline.server.effects.EffectFactory#getInstance().createFire($2.getWurmId(), $2.getPosX(), $2.getPosY(), $2.getPosZ(), $1.isOnSurface());");
        s = s.concat("$2.addEffect(effect);}}");
        //Insert after the "%s finishes unloading..." and just before the "return true;".
        ctmUnloadCargo.insertAt(1312, "{ " + s + " }");
    }

    private void craftWithinCartBytecode() throws BadBytecode {
        if (!craftWithinCart)
            return;
        JDBByteCode find;
        JDBByteCode replace;
        String replaceByteResult;

        //<editor-fold desc="Replace code">
            /*
            Removing indexes 0 - 4 in addCreationWindowOption of javap.exe dump.
            Removed-
                target.isUseOnGroundOnly() &&
            */
        //</editor-fold>
        setAddCreationWindowOption(cfItemBehaviour,
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Ljava/util/List;)V", "addCreationWindowOption");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.IFEQ)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/items/Item.isUseOnGroundOnly:()Z"),
                "0043")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "00,000000,000000", addCreationWindowOptionIterator, "addCreationWindowOption");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="Replace code">
            /*
            Removing indexes 42 - 51 in addCreationWindowOption of javap.exe dump.
            Removed-
                if (target.getTopParent() == target.getWurmId()) {
            */
        //</editor-fold>
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                Opcode.LCMP, Opcode.IFNE)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/items/Item.getTopParent:()J"),
                "",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/items/Item.getWurmId:()J"),
                "","0029")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "00,000000,00,000000,00,000000", addCreationWindowOptionIterator, "addCreationWindowOption");
        logger.log(Level.INFO, replaceByteResult);
        //getAddCreationWindowOptionAttribute().computeMaxStack();
        addCreationWindowOptionMInfo.rebuildStackMapIf6(pool, cfItemBehaviour);
    }

    private void useAltarInCartBytecode() throws BadBytecode {
        if (!useAltarInCart)
            return;
        JDBByteCode find;
        JDBByteCode replace;
        String replaceByteResult;

        //<editor-fold desc="pray() of MethodsReligion.Class changes.">
            /*
            // lines 158-158 in pray of MethodsReligion removed.
            if (altar.getParentId() != -10L) {
                performer.getCommunicator().sendNormalServerMessage("The altar needs to be on the ground to be used.");
                return true;
            }
            */
        //</editor-fold>
        setPray(cfMethodsReligion,
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                "pray");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                Opcode.IRETURN)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// Method com/wurmonline/server/items/Item.getParentId:()J"),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// long -10l"),
                "","000e","",
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// String The altar needs to be on the ground to be used.").substring(2),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "00,000000,000000,00,000000,00,000000,0000,000000,00,00",prayIterator,"pray");
        logger.log(Level.INFO, replaceByteResult);
        //<editor-fold desc="sacrifice() of MethodsReligion.class changes.">
            /*
            // lines 167-184 in sacrifice of MethodsReligion removed.
            if (altar.getParentId() != -10L) {
                performer.getCommunicator().sendNormalServerMessage("The altar needs to be on the ground to be used.");
                return true;
            }
            */
        //</editor-fold>
        setSacrifice(cfMethodsReligion,
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z",
                "sacrifice");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                Opcode.IRETURN)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// Method com/wurmonline/server/items/Item.getParentId:()J"),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// long -10l"),
                "","000e","",
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// String The altar needs to be on the ground to be used.").substring(2),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "00,000000,000000,00,000000,00,000000,0000,000000,00,00",sacrificeIterator,"sacrifice");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="action() of DomainItemBehaviour.class changes">
            /*
            Preach
            lines 129-165 in action of DomainItemBehaviour. (Action, Creature, Item, Item, short, float)
            removed-
                if (target.getParentId() != -10L) {
                    performer.getCommunicator().sendNormalServerMessage("The altar needs to be on the ground to be used.");
                } else {
            left in place an removed the else logic for-
                done = MethodsReligion.holdSermon(performer, target, source, act, counter);
            */
        //</editor-fold>
        setActionDB(cfDomainItemBehaviour,
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;SF)Z",
                "action");
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ALOAD_2,
                Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_1, Opcode.FLOAD, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("04",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/items/Item.getParentId:()J"),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// long -10l"),
                "","000f","",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// String The altar needs to be on the ground to be used.").substring(2),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "0098","","04","","","06",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/behaviours/MethodsReligion.holdSermon:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z"),
                "07","0089")));
        find.setOpcodeOperand();
        replace = new JDBByteCode();
        replace.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.ALOAD_2,Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_1, Opcode.FLOAD,
                Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
        replace.setOperandStructure(new ArrayList<>(Arrays.asList("","","","","","","","","","","","","","","","","","","","","","","","",
                "","04","","","06",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/behaviours/MethodsReligion.holdSermon:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z"),
                "07","0089")));
        replace.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(), replace.getOpcodeOperand(),
                actionDBIterator, "action");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="action() of DomainItemBehaviour.class changes">
            /*
            recharge
            lines 183-214 in action of DomainItemBehaviour. (Action, Creature, Item, Item, short, float)
            removed-
                if (target.getParentId() != -10L) {
                    performer.getCommunicator().sendNormalServerMessage("The altar needs to be on the ground to be used.");
                } else {
            left in place an removed the else logic for-
                done = MethodsReligion.sendRechargeQuestion(performer, source);
            */
        //</editor-fold>
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ALOAD_2,
                Opcode.ALOAD_3, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("04",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/items/Item.getParentId:()J"),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// long -10l"),
                "","000f","",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// String The altar needs to be on the ground to be used.").substring(2),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "0062","","",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/behaviours/MethodsReligion.sendRechargeQuestion:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z"),
                "07","0058")));
        find.setOpcodeOperand();
        replace = new JDBByteCode();
        replace.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.ALOAD_2,
                Opcode.ALOAD_3, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
        replace.setOperandStructure(new ArrayList<>(Arrays.asList("","","","","","","","","","","","","","","","","","","","","","","","",
                "","",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour, "// Method com/wurmonline/server/behaviours/MethodsReligion.sendRechargeQuestion:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z"),
                "07","0058")));
        replace.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(), replace.getOpcodeOperand(),
                actionDBIterator, "action");
        logger.log(Level.INFO, replaceByteResult);
    }
    //</editor-fold>

    //<editor-fold desc="Reflection methods section.">
    private int useBedReflection() throws IllegalAccessException, NoSuchFieldException {
        int bedCnt = 0;
        if (!useBedInCart)
            return bedCnt;
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        for (ItemTemplate template : fieldTemplates.values()) {
            Integer templateId = template.getTemplateId();
            Field fieldUseOnGroundOnly = ReflectionUtil.getField(ItemTemplate.class, "useOnGroundOnly");
            if (templateId == ItemList.bedStandard || templateId == ItemList.canopyBed) {
                if (template.isUseOnGroundOnly()) {
                    ReflectionUtil.setPrivateField(template, fieldUseOnGroundOnly, Boolean.FALSE);
                    bedCnt++;
                }
            }
        }
        return bedCnt;
    }

    private int boatInCartReflection() throws IllegalAccessException, NoSuchFieldException {
        int boatInCartCnt = 0;
        if (!boatInCart)
            return boatInCartCnt;
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        for (ItemTemplate template : fieldTemplates.values()) {
            Integer templateId = template.getTemplateId();
            Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
            if (template.isFloating() || templateId == ItemList.shipCarrier) {
                ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                boatInCartCnt++;
            }
        }
        return boatInCartCnt;
    }

    private int loadAltarReflection() throws IllegalAccessException, NoSuchFieldException {
        int altarCnt = 0;
        if (!loadAltar)
            return altarCnt;
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
            ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        for (ItemTemplate template : fieldTemplates.values()) {
            Integer templateId = template.getTemplateId();
            Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
            if (templateId == ItemList.altarWood || templateId == ItemList.altarGold || templateId == ItemList.altarSilver ||
                    templateId == ItemList.altarStone) {
                ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                altarCnt++;
            }

        }
        return altarCnt;
    }

    private int loadOtherReflection() throws IllegalAccessException, NoSuchFieldException {
        int otherCnt = 0;
        if (!loadOther)
            return otherCnt;
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        for (ItemTemplate template : fieldTemplates.values()) {
            Integer templateId = template.getTemplateId();
            Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
            if (templateId == ItemList.trashBin) {
                ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                otherCnt++;
            }
        }
        return otherCnt;
    }

    private int craftWithinCartReflection() throws IllegalAccessException, NoSuchFieldException {
        int craftWithinCartCnt = 0;
        if (!craftWithinCart)
            return craftWithinCartCnt;
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        for (ItemTemplate template : fieldTemplates.values()) {
            Integer templateId = template.getTemplateId();
            Field fieldUseOnGroundOnly = ReflectionUtil.getField(ItemTemplate.class, "useOnGroundOnly");
            if (templateId == ItemList.loom || templateId == ItemList.spinningWheel) {
                if (template.isUseOnGroundOnly()) {
                    ReflectionUtil.setPrivateField(template, fieldUseOnGroundOnly, Boolean.FALSE);
                    craftWithinCartCnt++;
                }
            }
        }
        return craftWithinCartCnt;
    }
    //</editor-fold>

    /*
    Multiple CodeIterators pointing to the same CodeAttribute can cause problems.
    Inserting gaps will always break other iterators pointing to the same attribute.
    CodeAttribute and MethodInfo also cause problems when multiple objects exist representing the same Wurm method.
    Use one class wide object for each CodeAttribute, CodeIterator, MethodInfo for all Wurm methods.
    The following section sets this up.
    */
    //<editor-fold desc="Getter and Setter for CodeIterator, CodeAttribute, methodInfo.">
    private static void setActionIB(ClassFile cf, String desc, String name){
        if (actionIBMInfo == null || actionIBIterator == null || actionIBAttribute == null){
            for (List a : new List[]{cf.getMethods()}){
                for (Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        actionIBMInfo = MInfo;
                        break;
                    }
                }
            }
            if (actionIBMInfo == null){
                throw new NullPointerException();
            }
            actionIBAttribute = actionIBMInfo.getCodeAttribute();
            actionIBIterator = actionIBAttribute.iterator();
        }
    }

    private static void setActionDB(ClassFile cf, String desc, String name){
        if (actionDBMInfo == null || actionDBIterator == null || actionDBAttribute == null){
            for (List a : new List[]{cf.getMethods()}){
                for (Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        actionDBMInfo = MInfo;
                        break;
                    }
                }
            }
            if (actionDBMInfo == null){
                throw new NullPointerException();
            }
            actionDBAttribute = actionDBMInfo.getCodeAttribute();
            actionDBIterator = actionDBAttribute.iterator();
        }
    }

    private static void setSacrifice(ClassFile cf, String desc, String name){
        if (sacrificeMInfo == null || sacrificeIterator == null || sacrificeAttribute == null){
            for (List a : new List[]{cf.getMethods()}){
                for (Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        sacrificeMInfo = MInfo;
                        break;
                    }
                }
            }
            if (sacrificeMInfo == null){
                throw new NullPointerException();
            }
            sacrificeAttribute = sacrificeMInfo.getCodeAttribute();
            sacrificeIterator = sacrificeAttribute.iterator();
        }
    }

    private static void setPray(ClassFile cf, String desc, String name){
        if (prayMInfo == null || prayIterator == null || prayAttribute == null){
            for (List a : new List[]{cf.getMethods()}){
                for (Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        prayMInfo = MInfo;
                        break;
                    }
                }
            }
            if (prayMInfo == null){
                throw new NullPointerException();
            }
            prayAttribute = prayMInfo.getCodeAttribute();
            prayIterator = prayAttribute.iterator();
        }
    }

    private static void setAddBedOptions(ClassFile cf, String desc, String name) {
        if (addBedOptionsMInfo == null || addBedOptionsIterator == null || addBedOptionsAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        addBedOptionsMInfo = MInfo;
                        break;
                    }
                }
            }
            if (addBedOptionsMInfo == null){
                throw new NullPointerException();
            }
            addBedOptionsAttribute = addBedOptionsMInfo.getCodeAttribute();
            addBedOptionsIterator = addBedOptionsAttribute.iterator();
        }
    }

    private static void setMoveToItem(ClassFile cf, String desc, String name) {
        if (moveToItemMInfo == null || moveToItemIterator == null || moveToItemAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        moveToItemMInfo = MInfo;
                        break;
                    }
                }
            }
            if (moveToItemMInfo == null){
                throw new NullPointerException();
            }
            moveToItemAttribute = moveToItemMInfo.getCodeAttribute();
            moveToItemIterator = moveToItemAttribute.iterator();
        }
    }

    private static void setLoadCargo(ClassFile cf, String desc, String name) {
        if (loadCargoMInfo == null || loadCargoIterator == null || loadCargoAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        loadCargoMInfo = MInfo;
                        break;
                    }
                }
            }
            if (loadCargoMInfo == null){
                throw new NullPointerException();
            }
            loadCargoAttribute = loadCargoMInfo.getCodeAttribute();
            loadCargoIterator = loadCargoAttribute.iterator();
        }
    }

    private static void setUnloadCargo(ClassFile cf, String desc, String name) {
        if (unloadCargoMInfo == null || unloadCargoIterator == null || unloadCargoAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        unloadCargoMInfo = MInfo;
                        break;
                    }
                }
            }
            if (unloadCargoMInfo == null){
                throw new NullPointerException();
            }
            unloadCargoAttribute = unloadCargoMInfo.getCodeAttribute();
            unloadCargoIterator = unloadCargoAttribute.iterator();
        }
    }

    private static void setPerformerIsNotOnATransportVehicle(ClassFile cf, String desc, String name) {
        if (performerIsNotOnATransportVehicleMInfo == null || performerIsNotOnATransportVehicleIterator == null ||
                performerIsNotOnATransportVehicleAttribute == null) {            for (List a : new List[]{cf.getMethods()}){
            for(Object b : a){
                MethodInfo MInfo = (MethodInfo) b;
                if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                    performerIsNotOnATransportVehicleMInfo = MInfo;
                    break;
                }
            }
        }
            if (performerIsNotOnATransportVehicleMInfo == null){
                throw new NullPointerException();
            }
            performerIsNotOnATransportVehicleAttribute = performerIsNotOnATransportVehicleMInfo.getCodeAttribute();
            performerIsNotOnATransportVehicleIterator = performerIsNotOnATransportVehicleAttribute.iterator();
        }
    }

    private static void setGetLoadUnloadActions(ClassFile cf, String desc, String name) {
        if (getLoadUnloadActionsMInfo == null || getLoadUnloadActionsIterator == null ||
                getLoadUnloadActionsAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
            for(Object b : a){
                MethodInfo MInfo = (MethodInfo) b;
                if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                    getLoadUnloadActionsMInfo = MInfo;
                    break;
                }
            }
        }
            if (getLoadUnloadActionsMInfo == null){
                throw new NullPointerException();
            }
            getLoadUnloadActionsAttribute = getLoadUnloadActionsMInfo.getCodeAttribute();
            getLoadUnloadActionsIterator = getLoadUnloadActionsAttribute.iterator();
        }
    }

    private static void setAskSleep(ClassFile cf, String desc, String name) {
        if (askSleepMInfo == null || askSleepIterator == null || askSleepAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        askSleepMInfo = MInfo;
                        break;
                    }
                }
            }
            if (askSleepMInfo == null){
                throw new NullPointerException();
            }
            askSleepAttribute = askSleepMInfo.getCodeAttribute();
            askSleepIterator = askSleepAttribute.iterator();
        }
    }

    private static void setSleep(ClassFile cf, String desc, String name) {
        if (sleepMInfo == null || sleepIterator == null || sleepAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        sleepMInfo = MInfo;
                        break;
                    }
                }
            }
            if (sleepMInfo == null){
                throw new NullPointerException();
            }
            sleepAttribute = sleepMInfo.getCodeAttribute();
            sleepIterator = sleepAttribute.iterator();
        }
    }

    private static void setMayUseBed(ClassFile cf, String desc, String name){
        if (mayUseBedMInfo == null || mayUseBedIterator == null || mayUseBedAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        mayUseBedMInfo = MInfo;
                        break;
                    }
                }
            }
            if (mayUseBedMInfo == null){
                throw new NullPointerException();
            }
            mayUseBedAttribute = mayUseBedMInfo.getCodeAttribute();
            mayUseBedIterator = mayUseBedAttribute.iterator();
        }
    }

    private static void setStartFire(ClassFile cf, String desc, String name){
        if (startFireMInfo == null || startFireIterator == null || startFireAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        startFireMInfo = MInfo;
                        break;
                    }
                }
            }
            if (startFireMInfo == null){
                throw new NullPointerException();
            }
            startFireAttribute = startFireMInfo.getCodeAttribute();
            startFireIterator = startFireAttribute.iterator();
        }
    }

    private static void setSetFire(ClassFile cf, String desc, String name){
        if (setFireMInfo == null || setFireIterator == null || setFireAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        setFireMInfo = MInfo;
                        break;
                    }
                }
            }
            if (setFireMInfo == null){
                throw new NullPointerException();
            }
            setFireAttribute = setFireMInfo.getCodeAttribute();
            setFireIterator = setFireAttribute.iterator();
        }
    }

    private void setAddCreationWindowOption(ClassFile cf, String desc, String name){
        if (addCreationWindowOptionMInfo == null || addCreationWindowOptionIterator == null || addCreationWindowOptionAttribute == null) {
            for (List a : new List[]{cf.getMethods()}){
                for(Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        addCreationWindowOptionMInfo = MInfo;
                        break;
                    }
                }
            }
            if (addCreationWindowOptionMInfo == null){
                throw new NullPointerException();
            }
            addCreationWindowOptionAttribute = addCreationWindowOptionMInfo.getCodeAttribute();
            addCreationWindowOptionIterator = addCreationWindowOptionAttribute.iterator();
        }
    }
    //</editor-fold>
}

