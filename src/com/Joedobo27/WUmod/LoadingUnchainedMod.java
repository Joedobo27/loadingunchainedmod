package com.Joedobo27.WUmod;


import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Creature;
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


@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"})
public class LoadingUnchainedMod implements WurmServerMod, Initable, Configurable, ServerStartedListener {

    //<editor-fold desc="Configure controls.">
    private static boolean loadEmptyOrNotBin = true;
    private static boolean moveItemsIntoBinWithinContainer = false;
    private static int loadProximityRequired = -1;
    private static int loadDurationTime = -1;
    private static boolean loadIntoDragged = false;
    private static boolean boatInCart = false;
    private static boolean useMagicChestInCart = false;
    private static boolean loadAltar = false;
    private static boolean loadOther = false;
    private static double minimumStrength = 23.0;
    private static boolean loadItemsNotOnGround = false;
    private static boolean loadIntoTrashbin = false;
    //</editor-fold>

    //<editor-fold desc="Bytecode objects">
    private static ClassPool pool;
    private static CtClass ctcCargoTransportationMethods;
    private static ClassFile cfCargoTransportationMethods;
    private static CtClass ctcItem;
    private static ClassFile cfItem;
    private static ConstPool cpItem;
    private static CtClass ctcItemBehaviour;
    private static ClassFile cfItemBehaviour;
    private static ConstPool cpItemBehaviour;
    private static CtClass ctcActions;
    private static ClassFile cfActions;

    // Hashes of vanilla WU code to notify of changes.
    private static final int LOAD_CARGO_HASH = 2072289037; // As of WU 1.0.0.7
    private static final int EMPTY_CONTAINER_CHECK_HASH = -1953802983; /// As of WU 1.0.0.7
    private static final int LOADING_TIME_HASH = 1635252118; // As of WU 1.0.0.7
    private static final int LOAD_DISTANCE_HASH = 2000966859; // As of WU 1.0.0.7
    private static final int NOT_ON_VEHICLE_HASH = -313592970; // As of WU 1.0.0.7
    private static final int NOT_SEATED_HASH = 1996695579; // As of WU 1.0.0.7
    private static final int TARGET_NOT_GROUNDED_HASH = 556599339; // As of WU 1.0.0.7
    //</editor-fold>

    //<editor-fold desc="Javassist objects">
    private static CodeAttribute moveToItemAttribute;
    private static CodeIterator moveToItemIterator;
    private static MethodInfo moveToItemMInfo;

    private static CodeAttribute loadCargoAttribute;
    private static CodeIterator loadCargoIterator;
    private static MethodInfo loadCargoMInfo;

    private static CodeAttribute actionIBAttribute;
    private static CodeIterator actionIBIterator;
    private static MethodInfo actionIBMInfo;
    //</editor-fold>

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

    public void configure(Properties properties) {
        loadEmptyOrNotBin = Boolean.parseBoolean(properties.getProperty("loadEmptyOrNotBin", Boolean.toString(loadEmptyOrNotBin)));
        moveItemsIntoBinWithinContainer = Boolean.parseBoolean(properties.getProperty("moveItemsIntoBinWithinContainer",
                Boolean.toString(moveItemsIntoBinWithinContainer)));
        loadProximityRequired = Integer.parseInt(properties.getProperty("loadProximityRequired", Integer.toString(loadProximityRequired)));
        loadProximityRequired = Math.max(loadProximityRequired, 1);
        loadProximityRequired = Math.min(loadProximityRequired, 160);
        loadDurationTime = Integer.parseInt(properties.getProperty("loadDurationTime", Integer.toString(loadDurationTime)));
        loadIntoDragged = Boolean.parseBoolean(properties.getProperty("loadIntoDragged", Boolean.toString(loadIntoDragged)));
        boatInCart = Boolean.parseBoolean(properties.getProperty("boatInCart", Boolean.toString(boatInCart)));
        useMagicChestInCart = Boolean.parseBoolean(properties.getProperty("useMagicChestInCart", Boolean.toString(useMagicChestInCart)));
        loadAltar = Boolean.parseBoolean(properties.getProperty("loadAltar", Boolean.toString(loadAltar)));
        loadOther = Boolean.parseBoolean(properties.getProperty("loadOther", Boolean.toString(loadOther)));
        minimumStrength = Double.parseDouble(properties.getProperty("minimumStrength", Double.toString(minimumStrength)));
        loadItemsNotOnGround = Boolean.parseBoolean(properties.getProperty("loadItemsNotOnGround" , Boolean.toString(loadItemsNotOnGround)));
        loadIntoTrashbin = Boolean.parseBoolean(properties.getProperty("loadIntoTrashbin", Boolean.toString(loadIntoTrashbin)));
    }

    @Override
    public void init() {
        try {
            pool = HookManager.getInstance().getClassPool();
            setJSCargoTransportationMethods();
            setJSItem();
            setJSItemBehaviour();
            setJSActions();

            loadFromWithinContainersBytecode();
            loadEmptyOrNotBinBytecode();
            moveItemsIntoBinWithinContainerBytecode();
            useCustomLoadTimeBytecode();
            useCustomProximityBytecode();
            loadIntoDraggedBytecode();
            menuAddLoadUnloadBytecode();
            useMagicChestInCartBytecode();
            loadIntoTrashbinBytecode();
        } catch (BadBytecode | NotFoundException | CannotCompileException | FileNotFoundException e) {
            logger.log(Level.WARNING, e.toString(), e);
        }
    }

    @Override
    public void onServerStarted() {
        try {
            int boatInCartCnt = boatInCartReflection();
            int altarCnt = loadAltarReflection();
            int otherCnt = loadOtherReflection();
            loadIntoTrashbinReflection();

            logger.log(Level.INFO, "boatInCart: " + boatInCartCnt);
            logger.log(Level.INFO, "loadAltar: " + altarCnt);
            logger.log(Level.INFO, "loadOther: " + otherCnt);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.log(Level.WARNING, e.toString(), e);
        }

    }

    @SuppressWarnings("unused")
    public static Item embarkedVehicleToItemSafe(Creature pilot) {
        try {
            if (pilot.getVehicle() != -10L)
                return Items.getItem(pilot.getVehicle());
        } catch (NoSuchItemException ignored) {}
        return null;
    }

    public static Item wurmIdToItemSafe(long wurmId){
        Item i = null;
        try {
            i =  Items.getItem(wurmId) != null ? Items.getItem(wurmId) : null;
        } catch (NoSuchItemException ignored) {}
        return i;
    }

    /**
     * Hooking method
     *
     * @param player type WU Creature.
     * @return -10L is an equivalent to null or a general failure.
     */
    public static long getVehicleIdFromDragEmbark(Creature player){
        long vehicleEmbark;
        long vehicleDragged;
        long toReturn;

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

    /**
     * Hooking method that whose return value replaces what would be returned by WU's CargoTransportationMethods.class
     * getLoadUnloadActions().
     *
     * @param player type WU Creature.
     * @param target type WU Item.
     * @return List<ActionEntry> which is either the unload actionEntrys[606] or load actionEntrys[605].
     */
    @SuppressWarnings("unused")
    public static List<ActionEntry> getLoadUnloadActionsHook(final Creature player, final Item target) {
        List<ActionEntry> toReturn = new LinkedList<>();
        Vehicle vehicle;
        Item vehicleItem;

        // Target qualifiers.
        if (!target.getTemplate().isTransportable() && !target.isBoat() && !target.isUnfinished()) {
            return toReturn;}
        // Recursive vehicle nesting is buggy. Only load empty vehicles.
        if (target.isVehicle() && !target.isEmpty()) {
            return toReturn;}
        //Vehicles use mode.
        long vehicleLong = getVehicleIdFromDragEmbark(player);
        vehicle = vehicleLong != -10L ? Vehicles.getVehicleForId(vehicleLong) : null;
        vehicleItem = vehicle != null ? wurmIdToItemSafe(vehicle.wurmid) : null;
        // Unload
        if (target.getTopParent() == -10L || target.getWurmId() == -10L) {return toReturn;}
        if (target.getTopParent() != target.getWurmId()) {
            vehicle = Vehicles.getVehicleForId(target.getTopParent());
            if (vehicle != null) {
                Item targetParentVehicleItem = wurmIdToItemSafe(vehicle.wurmid);
                if (!vehicle.creature && (MethodsItems.mayUseInventoryOfVehicle(player, targetParentVehicleItem) ||
                        targetParentVehicleItem.getLockId() == -10L || Items.isItemDragged(targetParentVehicleItem))) {
                    toReturn.add(Actions.actionEntrys[606]);
                }
            }
        }
        // load
        if (loadIntoContainerInvalid(vehicle) || vehicleItem == null || target.getTopParent() == vehicleLong){return toReturn;}
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

    @SuppressWarnings("unused")
    public static int getTemplateIdHook(Item target){
        int templateId = target.getTemplateId();
        return templateId == ItemList.trashBin ? 186 : templateId;
    }

     //<editor-fold desc="Javassist and bytecode altering section.">
    private void setJSCargoTransportationMethods() throws NotFoundException {
        ctcCargoTransportationMethods = pool.get("com.wurmonline.server.behaviours.CargoTransportationMethods");
        cfCargoTransportationMethods = ctcCargoTransportationMethods.getClassFile();
    }

    private void setJSItem() throws NotFoundException {
        ctcItem = pool.get("com.wurmonline.server.items.Item");
        cfItem = ctcItem.getClassFile();
        cpItem = cfItem.getConstPool();
    }

    private void setJSItemBehaviour() throws NotFoundException {
        ctcItemBehaviour = pool.get("com.wurmonline.server.behaviours.ItemBehaviour");
        cfItemBehaviour = ctcItemBehaviour.getClassFile();
        cpItemBehaviour = cfItemBehaviour.getConstPool();
    }

    private void setJSActions() throws NotFoundException {
        ctcActions = pool.get("com.wurmonline.server.behaviours.Actions");
        cfActions = ctcActions.getClassFile();
    }

    private void loadFromWithinContainersBytecode() throws NotFoundException, CannotCompileException, BadBytecode {
        if (!loadItemsNotOnGround)
            return;
        setLoadCargo(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                "loadCargo");
        CodeIterator targetIsNotOnTheGroundIterator = cfCargoTransportationMethods.getMethod("targetIsNotOnTheGround").getCodeAttribute().iterator();
        if ( TARGET_NOT_GROUNDED_HASH == JDBByteCode.byteCodeHashCheck(targetIsNotOnTheGroundIterator)) {
            CtMethod cmTargetIsNotOnTheGround = ctcCargoTransportationMethods.getMethod("targetIsNotOnTheGround",
                    "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Z)Z");
            cmTargetIsNotOnTheGround.setBody("return false;");
        }else{
            logger.log(Level.WARNING, "Hash of targetIsNotOnTheGround() doesn't match vanilla WU. loadItemsNotOnGround failure");
        }

        // removeItem() in Item.class is private. None of the downstream methods that end up calling this are useful here.
        // Add a new method that is just a public wrapper of removeItem().
        String s = "public com.wurmonline.server.items.Item removeItemA(long _id, boolean setPosition, boolean ignoreWatchers, boolean skipPileRemoval)";
        s += "{com.wurmonline.server.items.Item a = this.removeItem(_id, setPosition, ignoreWatchers, skipPileRemoval);return a;}";
        s += "catch (com.wurmonline.server.NoSuchItemException ignore){}return null;";
        CtMethod m = CtNewMethod.make(s, ctcItem);
        ctcItem.addMethod(m);

        if (LOAD_CARGO_HASH == JDBByteCode.byteCodeHashCheck(loadCargoIterator)) {
            // Each container Item has an array of items that it contains. I'm not sure this array is getting updated for
            // recursive parent containers when something is nested. recursive step up each parent and call carrierItem.removeItemA.
            CtMethod ctmLoadCargo = ctcCargoTransportationMethods.getMethod("loadCargo",
                    "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
            s = "if (target.getWurmId() != target.getTopParent()){";
            s += "try{com.wurmonline.server.items.Item carrierItem = com.wurmonline.server.Items.getItem(target.getTopParent());";
            s += "carrierItem.removeItemA(target.getWurmId(), false, false, false);}catch(com.wurmonline.server.NoSuchItemException ignore){};}";
            // insert this new method in load code...parentItem.removeItemA(targetItemLong, false, false, false);
            // Insert code right before "if (targetIsNotOnTheGround(target, performer, true)) ".
            ctmLoadCargo.insertAt(272, "{" + s + "}");
        } else {
            logger.log(Level.WARNING, "Hash of loadCargo() doesn't match vanilla WU. loadItemsNotOnGround insert failure");
        }
        logger.log(Level.INFO, "loadItemsNotOnGround alterations complete.");
    }

    private void loadEmptyOrNotBinBytecode() throws NotFoundException, CannotCompileException, BadBytecode {
        if (!loadEmptyOrNotBin)
            return;
        CodeIterator targetIsNotEmptyContainerCheckIterator = cfCargoTransportationMethods.getMethod("targetIsNotEmptyContainerCheck").getCodeAttribute().iterator();
        if ( EMPTY_CONTAINER_CHECK_HASH == JDBByteCode.byteCodeHashCheck(targetIsNotEmptyContainerCheckIterator)) {
            // Alter targetIsNotEmptyContainerCheck() of cargoTransportationMethods.class to always return false.
            CtMethod ctmTargetIsNotEmptyContainerCheck = ctcCargoTransportationMethods.getMethod("targetIsNotEmptyContainerCheck",
                    "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z");
            ctmTargetIsNotEmptyContainerCheck.setBody("return false;");
        }else{
            logger.log(Level.WARNING, "Hash of targetIsNotEmptyContainerCheck() don't match vanilla WU. loadEmptyOrNotBin failure");
        }
        logger.log(Level.INFO, "loadEmptyOrNotBin alterations complete.");
    }

    private void moveItemsIntoBinWithinContainerBytecode() throws BadBytecode {
        if (!moveItemsIntoBinWithinContainer)
            return;
        JDBByteCode find;
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
        find = new JDBByteCode();
        find.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.IFNULL, Opcode.LDC_W,
                Opcode.ICONST_1, Opcode.ANEWARRAY, Opcode.DUP, Opcode.ICONST_0, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.AASTORE,
                Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.ICONST_0, Opcode.IRETURN)));
        find.setOperandStructure(new ArrayList<>(Arrays.asList("08",
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
        find.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(find.getOpcodeOperand(), find.getOpcodeOperand(),
                "0000,000000,0000,000000,00,000000,0000,000000,000000,00,000000,000000,00,000000,00,00,0000,000000,00,000000,0000,00,000000,0000,000000,00,00",
                moveToItemIterator, "moveToItem");
        logger.log(Level.INFO, replaceByteResult);
    }

    private void useCustomProximityBytecode() throws  NotFoundException, CannotCompileException, BadBytecode {
        if (loadProximityRequired < 0)
            return;
        CodeIterator getLoadActionDistanceIterator = cfCargoTransportationMethods.getMethod("getLoadActionDistance").getCodeAttribute().iterator();
        if (LOAD_DISTANCE_HASH == JDBByteCode.byteCodeHashCheck(getLoadActionDistanceIterator)) {
            // alter getLoadActionDistance() of  cargoTransportationMethods.class to return properties value in loadProximityRequired.
            CtMethod ctmGetLoadActionDistance = ctcCargoTransportationMethods.getMethod("getLoadActionDistance",
                    "(Lcom/wurmonline/server/items/Item;)I");
            ctmGetLoadActionDistance.setBody("return " + loadProximityRequired + ";");
            logger.log(Level.INFO, "Custom loading proximity requirement set to " + loadProximityRequired);
        }else{
            logger.log(Level.WARNING, "Hash of getLoadActionDistance() doesn't match vanilla WU. loadProximityRequired failure");
        }
    }

    private void useCustomLoadTimeBytecode()throws NotFoundException, CannotCompileException, BadBytecode {
        if (loadDurationTime < 0)
            return;
        CodeIterator getLoadUnloadActionTimeIterator = cfActions.getMethod("getLoadUnloadActionTime").getCodeAttribute().iterator();
        if (LOADING_TIME_HASH == JDBByteCode.byteCodeHashCheck(getLoadUnloadActionTimeIterator)) {
            // alter getLoadUnloadActionTime() of Actions.class to always return the properties value in loadDurationTime.
            CtMethod cmGetLoadUnloadActionTime = ctcActions.getMethod("getLoadUnloadActionTime", "(Lcom/wurmonline/server/creatures/Creature;)I");
            cmGetLoadUnloadActionTime.setBody("return " + loadDurationTime + ";");
            logger.log(Level.INFO, "Custom load time set to " + loadDurationTime / 10 + " seconds.");
        }else{
            logger.log(Level.WARNING, "Hash of getLoadUnloadActionTime() doesn't match vanilla WU. loadDurationTime failure");
        }
    }

    private void loadIntoDraggedBytecode() throws BadBytecode, NotFoundException, CannotCompileException, FileNotFoundException {
        if (!loadIntoDragged)
            return;
        JDBByteCode find;
        JDBByteCode replace;
        String replaceByteResult;

        // performer.getVehicle() is a problem. It's used in more then just load cargo applications so it can't be overwritten.
        // Make a new method: getVehicleIdDragOrEmbark() in Player.class and have it by default return this.getVehicle()
        // ( wrap the getVehicle() and have the new method return the same as unwrapped by default). Next, use bytecode
        // to replace "performer.getVehicle()" with "performer.getVehicleIdDragOrEmbark()" for loading situations in ItemBehaviour.class.
        // Finally, use ExprEditor to hook into getVehicleIdDragOrEmbark() and redirect the return value derived from a hooked
        // method in mod.
        CtMethod m = CtNewMethod.make(
                "public long getVehicleIdDragOrEmbark(){return $0.getVehicle();}",
                pool.getCtClass("com.wurmonline.server.players.Player"));
        pool.getCtClass("com.wurmonline.server.players.Player").addMethod(m);

        //<editor-fold desc="changed action(Action,Creation,Item,Short,Float) in ItemBehaviour.class">
        /*
            -starting on line 3384 to 3399

            -was
            if (performer.getVehicle() == -10L) {
                    break Label_14818;
                }
                try {
                    final Item vehicle2 = Items.getItem(performer.getVehicle());
                    if (vehicle2.getTemplateId() != 853) {
            -becomes
            replace all performer.getVehicle() with performer.getVehicleIdDragOrEmbark().
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
                "", "2D72","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/creatures/Creature.getVehicle:()J"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/Items.getItem:(J)Lcom/wurmonline/server/items/Item;"))));
        find.setOpcodeOperand();
        replace = new JDBByteCode();
        replace.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.IRETURN, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W,
                Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC)));
        replace.setOperandStructure(new ArrayList<>(Arrays.asList("","",
                JDBByteCode.addConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/players/Player.getVehicleIdDragOrEmbark:()J"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, "// long -10l"),
                "", "2D72","",
                JDBByteCode.addConstantPoolReference(cpItemBehaviour, "// Method com/wurmonline/server/players/Player.getVehicleIdDragOrEmbark:()J"),
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
                if (Objects.equals(methodCall.getMethodName(), "getVehicleIdDragOrEmbark")){
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getVehicleIdFromDragEmbark(performer);");
                }
            }
        });

        CtMethod ctmLoadCargo = ctcCargoTransportationMethods.getMethod("loadCargo",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
        ctmLoadCargo.instrument( new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals(methodCall.getMethodName(), "getVehicle")) {
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getVehicleIdFromDragEmbark(performer);");
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
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getVehicleIdFromDragEmbark(performer);");
                }
                else if (Objects.equals(methodCall.getMethodName(), "performerIsNotOnATransportVehicle")) {
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.loadIntoContainerInvalid(vehicle);");
                }
            }
        });

        CodeIterator performerIsNotOnAVehicleIterator = cfCargoTransportationMethods.getMethod("performerIsNotOnAVehicle").getCodeAttribute().iterator();
        if (NOT_ON_VEHICLE_HASH == JDBByteCode.byteCodeHashCheck(performerIsNotOnAVehicleIterator)) {
            // performerIsNotOnAVehicle() of cargoTransportationMethods always return false.
            CtMethod ctmPerformerIsNotOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotOnAVehicle",
                    "(Lcom/wurmonline/server/creatures/Creature;)Z");
            ctmPerformerIsNotOnAVehicle.setBody("return false;");
        }else{
            logger.log(Level.WARNING, "Hash of performerIsNotOnAVehicle() doesn't match vanilla WU. loadIntoDragged failure");
        }


        CodeIterator performerIsNotSeatedOnAVehicleIterator = cfCargoTransportationMethods.getMethod("performerIsNotSeatedOnAVehicle").getCodeAttribute().iterator();
        if ( NOT_SEATED_HASH == JDBByteCode.byteCodeHashCheck(performerIsNotSeatedOnAVehicleIterator)) {
            // performerIsNotSeatedOnAVehicle() of cargoTransportationMethods always return false.
            CtMethod ctmPerformerIsNotSeatedOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotSeatedOnAVehicle",
                    "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Seat;)Z");
            ctmPerformerIsNotSeatedOnAVehicle.setBody("return false;");
        }else{
            logger.log(Level.WARNING, "Hash of performerIsNotSeatedOnAVehicle() doesn't match vanilla WU. loadIntoDragged failure");
        }
        logger.log(Level.INFO, "loadIntoDragged alterations complete.");
    }

    @SuppressWarnings("ConstantConditions")
    private void menuAddLoadUnloadBytecode() throws NotFoundException, CannotCompileException {
        if (!boatInCart && !loadIntoDragged)
            return;
        pool.get("com.wurmonline.server.behaviours.ItemBehaviour").instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (Objects.equals(ctcCargoTransportationMethods.getName(), m.getClassName()) && Objects.equals("getLoadUnloadActions", m.getMethodName())) {
                    m.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getLoadUnloadActionsHook($1, $2);");
                    logger.log(Level.INFO, "replaced getLoadUnloadActionsHook() at " + m.getLineNumber());
                }
            }
        });
    }

    private void useMagicChestInCartBytecode() throws BadBytecode {
        if (!useMagicChestInCart)
            return;
        JDBByteCode find;
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
        logger.log(Level.INFO, "useMagicChestInCart   " + replaceByteResult);
    }

    private void loadIntoTrashbinBytecode() throws NotFoundException, CannotCompileException {
        if (!loadIntoTrashbin)
            return;
        CtMethod cmAction = ctcItemBehaviour.getMethod("action",
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;SF)Z");

        cmAction.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("getTemplateId", methodCall.getMethodName()) && methodCall.getLineNumber() == 4066){
                    methodCall.replace("$_ = com.Joedobo27.WUmod.LoadingUnchainedMod.getTemplateIdHook(target);");
                    logger.log(Level.INFO, "loadIntoTrashbin.... hook installed for ItemBehaviour.class, " +
                            "Action(Action,Creature,Item,short,float) at " + methodCall.getLineNumber());
                }
            }
        });
    }

    //</editor-fold>

    //<editor-fold desc="Reflection methods section.">

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
            int templateId = template.getTemplateId();
            Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
            if (templateId == ItemList.trashBin) {
                ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                otherCnt++;
            }
        }
        return otherCnt;
    }

    private void loadIntoTrashbinReflection() throws NoSuchFieldException, IllegalAccessException{
        if (!loadIntoTrashbin)
            return;
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        for (ItemTemplate template : fieldTemplates.values()) {
            int templateId = template.getTemplateId();
            if (templateId == ItemList.trashBin){
                //boolean isVehicleDragged; boolean draggable; private boolean isVehicle; boolean isMovingItem;
                Field fIsVehicleDragged = ReflectionUtil.getField(ItemTemplate.class, "isVehicleDragged");
                Field fDraggable = ReflectionUtil.getField(ItemTemplate.class, "draggable");
                Field fIsVehicle = ReflectionUtil.getField(ItemTemplate.class, "isVehicle");
                Field fIsMovingItem = ReflectionUtil.getField(ItemTemplate.class, "isMovingItem");

                ReflectionUtil.setPrivateField(template, fIsVehicleDragged, Boolean.TRUE);
                ReflectionUtil.setPrivateField(template, fDraggable, Boolean.TRUE);
                ReflectionUtil.setPrivateField(template, fIsVehicle, Boolean.TRUE);
                ReflectionUtil.setPrivateField(template, fIsMovingItem, Boolean.TRUE);
            }
        }
        logger.log(Level.INFO, "trashBin altered to facilitate loading stuff into it.");
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


    /**
     * Set for action method of ItemBehaviour.class
     *
     * @param cf type ClassFile
     * @param desc type String. JFM descriptor.
     * @param name type String. name of method.
     */
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
    //</editor-fold>
}

