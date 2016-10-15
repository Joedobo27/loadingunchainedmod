package com.joedobo27.loadingunchained;

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.joedobo27.loadingunchained.BytecodeTools.*;


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
    private static JAssistClassData itemClass;
    private static JAssistClassData itemBehaviour;
    private static JAssistClassData actionsClass;
    private static JAssistClassData cargoTransportationMethods;
    private static JAssistMethodData loadCargo;
    //</editor-fold>

    private static final Logger logger = Logger.getLogger(LoadingUnchainedMod.class.getName());

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
            itemClass = new JAssistClassData("com.wurmonline.server.items.Item", pool);
            itemBehaviour = new JAssistClassData("com.wurmonline.server.behaviours.ItemBehaviour", pool);
            actionsClass = new JAssistClassData("com.wurmonline.server.behaviours.Actions", pool);
            cargoTransportationMethods = new JAssistClassData("com.wurmonline.server.behaviours.CargoTransportationMethods", pool);

            loadCargo = new JAssistMethodData(cargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                    "loadCargo");

            loadFromWithinContainersBytecode();
            loadEmptyOrNotBinBytecode();
            moveToItemChanges();
            useCustomLoadTimeBytecode();
            useCustomProximityBytecode();
            loadIntoDraggedBytecode();
            menuAddLoadUnloadBytecode();
            minimumStrengthBytecode();
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
            boolean isDragged = vehicleDraggingItem != null;
            vehicleDragged =  isDragged ? vehicleDraggingItem.getWurmId() : -10L;

            boolean notDragOrEmbark = vehicleDragged == -10L && vehicleEmbark == -10L;
            boolean isEmbarked = !notDragOrEmbark && vehicleDragged == -10L;
            if (notDragOrEmbark) {
                toReturn = -10L;
            } else if (isEmbarked) {
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

    @SuppressWarnings("unused")
    public static void printDataHook(Item target, Creature performer){
        Vehicle vehicle = Vehicles.getVehicle(target);

        boolean isDraggable = target.isDraggable();
        logger.log(Level.FINE, "isDraggable: " + Boolean.toString(isDraggable));

        logger.log(Level.FINE, "vehiclePilot: " + Long.toString(vehicle.getPilotId()));

        boolean vehicleHasDraggers = vehicle.draggers != null && !vehicle.draggers.isEmpty();
        logger.log(Level.FINE, "vehicleHasDraggers: " + Boolean.toString(vehicleHasDraggers));

        logger.log(Level.FINE, "id: " + Integer.toString(target.getTemplateId()));

        logger.log(Level.FINE, "target kingdom: " + Integer.toString(target.getKingdom()));
        logger.log(Level.FINE, "performer kingdom: " + Integer.toString(performer.getKingdomId()));

        try {
            Class creatureClass = Class.forName("com.wurmonline.server.creatures.Creature");
            Class itemClass = Class.forName("com.wurmonline.server.items.Item");
            Method hasPermission = Class.forName("com.wurmonline.server.behaviours.VehicleBehaviour")
                    .getDeclaredMethod("hasPermission", creatureClass, itemClass);
            hasPermission.setAccessible(true);
            Object hasPermissionReturn = hasPermission.invoke(null, performer, target);
            logger.log(Level.FINE, "hasPermissionReturn: " + hasPermissionReturn);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e){
            logger.log(Level.WARNING, e.getMessage(), e);
        }

        boolean mayDragVehicle = target.mayDrag(performer);
        logger.log(Level.FINE, "mayDragVehicle: " + Boolean.toString(mayDragVehicle));
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

    private void loadFromWithinContainersBytecode() throws NotFoundException, CannotCompileException, BadBytecode {
        if (!loadItemsNotOnGround)
            return;
        cargoTransportationMethods.getCtClass().getMethod("targetIsNotOnTheGround",
                "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Z)Z")
                .setBody("return false;");

        // removeItem() in Item.class is private. None of the downstream methods that end up calling this are useful here.
        // Add a new method that is just a public wrapper of removeItem().
        //String methodSource = "public com.wurmonline.server.items.Item removeItemA(long _id, final boolean setPosition, boolean ignoreWatchers, boolean skipPileRemoval){";
        String methodSource = "{com.wurmonline.server.items.Item item = null; try { item = this.removeItem($1, $2, $3, $4);";
        methodSource += "}catch (com.wurmonline.server.NoSuchItemException ignore) {} finally { return item;}}";

        CtMethod removeItemA = CtNewMethod.make(itemClass.getCtClass(), "removeItemA",
                new CtClass[]{CtClass.longType, CtClass.booleanType, CtClass.booleanType, CtClass.booleanType}, null,
                methodSource, itemClass.getCtClass());
        itemClass.getCtClass().addMethod(removeItemA);

        // Each container Item has an array of items that it contains. I'm not sure this array is getting updated for
        // recursive parent containers when something is nested. recursive step up each parent and call carrierItem.removeItemA.
        String insertCode = "if (target.getWurmId() != target.getTopParent()){";
        insertCode += "try{com.wurmonline.server.items.Item carrierItem = com.wurmonline.server.Items.getItem(target.getTopParent());";
        insertCode += "carrierItem.removeItemA(target.getWurmId(), false, false, false);}catch(com.wurmonline.server.NoSuchItemException ignore){};}";
        // insert this new method in load code...parentItem.removeItemA(targetItemLong, false, false, false);
        // Insert code right before "if (targetIsNotOnTheGround(target, performer, true)) ".
        int insertResult = loadCargo.getCtMethod().insertAt(269, "{" + insertCode + "}");
        logger.log(Level.FINE, "loadItemsNotOnGround insert in loadCargo() at: " + Integer.toString(insertResult));
        logger.log(Level.INFO, "loadItemsNotOnGround alterations complete.");
    }

    private void loadEmptyOrNotBinBytecode() throws NotFoundException, CannotCompileException, BadBytecode {
        if (!loadEmptyOrNotBin)
            return;
        // Alter targetIsNotEmptyContainerCheck() of cargoTransportationMethods.class to always return false.
        cargoTransportationMethods.getCtClass().getMethod("targetIsNotEmptyContainerCheck",
                "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z")
                .setBody("return false;");
        logger.log(Level.INFO, "loadEmptyOrNotBin alterations complete.");
    }

    private void moveToItemChanges() throws NotFoundException, CannotCompileException {
        if (!moveItemsIntoBinWithinContainer && !useMagicChestInCart)
            return;
        final int[] moveItemsIntoBinWithinContainerSuccesses = new int[]{0};
        final int[] useMagicChestInCartSuccesses = new int[]{0};

        JAssistMethodData moveToItem = new JAssistMethodData(itemClass,"(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
        // moveItemsIntoBinWithinContainer alter " if (target.getTopParent() != target.getWurmId() && !target.isCrate()) "
        // so target.getTopParent returns target.getWurmId.
        // useMagicChestInCart alter the returned value of getTemplateId() at line 2150 to always return 1.
        // 1 never equals the magic chest ItemID.


        moveToItem.getCtMethod().instrument( new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("getTopParent", methodCall.getMethodName()) && methodCall.getLineNumber() == 2758 && moveItemsIntoBinWithinContainer) {
                    logger.log(Level.FINE, moveToItem.getCtMethod().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = target.getWurmId();");
                    moveItemsIntoBinWithinContainerSuccesses[0] = 1;
                } else if (Objects.equals("getTemplateId", methodCall.getMethodName()) && methodCall.getLineNumber() == 2145 && useMagicChestInCart) {
                    logger.log(Level.FINE, moveToItem.getCtMethod().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = 1;");
                    useMagicChestInCartSuccesses[0] = 1;
                }
            }
        });
        if (moveItemsIntoBinWithinContainer)
            evaluateChangesArray(moveItemsIntoBinWithinContainerSuccesses, "moveItemsIntoBinWithinContainer");

        if (useMagicChestInCart)
            evaluateChangesArray(useMagicChestInCartSuccesses, "useMagicChestInCart");
    }

    private void useCustomProximityBytecode() throws  NotFoundException, CannotCompileException, BadBytecode {
        if (loadProximityRequired < 0)
            return;
        // alter getLoadActionDistance() of  cargoTransportationMethods.class to return properties value in loadProximityRequired.
        cargoTransportationMethods.getCtClass().getMethod("getLoadActionDistance", "(Lcom/wurmonline/server/items/Item;)I")
                .setBody("return " + loadProximityRequired + ";");
        logger.log(Level.INFO, "Custom loading proximity requirement set to " + loadProximityRequired);
    }

    private void useCustomLoadTimeBytecode()throws NotFoundException, CannotCompileException, BadBytecode {
        if (loadDurationTime < 0)
            return;
        // alter getLoadUnloadActionTime() of Actions.class to always return the properties value in loadDurationTime.
        actionsClass.getCtClass().getMethod("getLoadUnloadActionTime", "(Lcom/wurmonline/server/creatures/Creature;)I")
                .setBody("return " + loadDurationTime + ";");
        logger.log(Level.INFO, "Custom load time set to " + loadDurationTime / 10 + " seconds.");

    }

    private void loadIntoDraggedBytecode() throws BadBytecode, NotFoundException, CannotCompileException, FileNotFoundException {
        if (!loadIntoDragged && !loadIntoTrashbin)
            return;

        JAssistMethodData actionItemBehaviour = new JAssistMethodData(itemBehaviour, "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;SF)Z"
                , "action");
        final int[] loadIntoDraggedSuccesses = new int[]{0,0,0,0,0,0};
        final int[] loadIntoTrashbinSuccesses = new int[]{0};


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
        byte[] findPoolResult;
        Bytecode find = new Bytecode(itemBehaviour.getConstPool());
        find.addReturn(CtClass.intType);
        find.addOpcode(Opcode.ALOAD_2);

        find.addOpcode(Opcode.INVOKEVIRTUAL);
        findPoolResult = findConstantPoolReference(itemBehaviour.getConstPool(), "// Method com/wurmonline/server/creatures/Creature.getVehicle:()J");
        find.add(findPoolResult[0], findPoolResult[1]);

        find.addOpcode(Opcode.LDC2_W);
        findPoolResult = findConstantPoolReference(itemBehaviour.getConstPool(), "// long -10l");
        find.add(findPoolResult[0], findPoolResult[1]);

        find.addOpcode(Opcode.LCMP);
        find.addOpcode(Opcode.IFEQ); find.add(46, 194); //Jump forward 11,970 (2EC2) lines.
        find.addOpcode(Opcode.ALOAD_2);

        find.addOpcode(Opcode.INVOKEVIRTUAL);
        findPoolResult = findConstantPoolReference(itemBehaviour.getConstPool(), "// Method com/wurmonline/server/creatures/Creature.getVehicle:()J");
        find.add(findPoolResult[0], findPoolResult[1]);

        find.addOpcode(Opcode.INVOKESTATIC);
        findPoolResult = findConstantPoolReference(itemBehaviour.getConstPool(), "// Method com/wurmonline/server/Items.getItem:(J)Lcom/wurmonline/server/items/Item;");
        find.add(findPoolResult[0], findPoolResult[1]);


        Bytecode replace = new Bytecode(itemBehaviour.getConstPool());
        replace.addReturn(CtClass.intType);
        replace.addOpcode(Opcode.ALOAD_2);

        replace.addOpcode(Opcode.INVOKEVIRTUAL);
        findPoolResult = addConstantPoolReference(itemBehaviour.getConstPool(), "// Method com/wurmonline/server/players/Player.getVehicleIdDragOrEmbark:()J");
        replace.add(findPoolResult[0], findPoolResult[1]);

        replace.addOpcode(Opcode.LDC2_W);
        findPoolResult = findConstantPoolReference(itemBehaviour.getConstPool(), "// long -10l");
        replace.add(findPoolResult[0], findPoolResult[1]);

        replace.addOpcode(Opcode.LCMP);
        replace.addOpcode(Opcode.IFEQ);
        replace.add(46, 194); //Jump forward 11,970 (2EC2) lines.
        replace.addOpcode(Opcode.ALOAD_2);

        replace.addOpcode(Opcode.INVOKEVIRTUAL);
        findPoolResult = addConstantPoolReference(itemBehaviour.getConstPool(), "// Method com/wurmonline/server/players/Player.getVehicleIdDragOrEmbark:()J");
        replace.add(findPoolResult[0], findPoolResult[1]);

        replace.addOpcode(Opcode.INVOKESTATIC);
        findPoolResult = findConstantPoolReference(itemBehaviour.getConstPool(), "// Method com/wurmonline/server/Items.getItem:(J)Lcom/wurmonline/server/items/Item;");
        replace.add(findPoolResult[0], findPoolResult[1]);

        boolean findResult = findReplaceCodeIterator(actionItemBehaviour.getCodeIterator(), find, replace);
        loadIntoDraggedSuccesses[0] = findResult ? 1 : 0;

            actionItemBehaviour.getCtMethod().instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals(methodCall.getMethodName(), "getVehicleIdDragOrEmbark")){
                    logger.log(Level.FINE, actionItemBehaviour.getCtMethod().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = com.joedobo27.loadingunchained.LoadingUnchainedMod.getVehicleIdFromDragEmbark(performer);");
                    loadIntoDraggedSuccesses[1] = 1;
                } else if (Objects.equals("getTemplateId", methodCall.getMethodName()) && methodCall.getLineNumber() == 4152 &&
                        loadIntoTrashbin){
                    logger.log(Level.FINE, actionItemBehaviour.getCtMethod().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = com.joedobo27.loadingunchained.LoadingUnchainedMod.getTemplateIdHook(target);");
                    loadIntoTrashbinSuccesses[0] = 1;
                }
            }
        });

        loadCargo.getCtMethod().instrument( new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals(methodCall.getMethodName(), "getVehicle")) {
                    logger.log(Level.FINE, loadCargo.getCtMethod().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = com.joedobo27.loadingunchained.LoadingUnchainedMod.getVehicleIdFromDragEmbark(performer);");
                    loadIntoDraggedSuccesses[2] = 1;
                }
                else if (Objects.equals(methodCall.getMethodName(), "performerIsNotOnATransportVehicle")) {
                    logger.log(Level.FINE, loadCargo.getCtMethod().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = com.joedobo27.loadingunchained.LoadingUnchainedMod.loadIntoContainerInvalid(vehicle);");
                    loadIntoDraggedSuccesses[3] = 1;
                }
            }
        });
        JAssistMethodData loadShip = new JAssistMethodData(cargoTransportationMethods,
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z", "loadShip");
        loadShip.getCtMethod().instrument( new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals(methodCall.getMethodName(), "getVehicle")) {
                    logger.log(Level.FINE, loadShip.getCtMethod().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = com.joedobo27.loadingunchained.LoadingUnchainedMod.getVehicleIdFromDragEmbark(performer);");
                    loadIntoDraggedSuccesses[4] = 1;
                }
                else if (Objects.equals(methodCall.getMethodName(), "performerIsNotOnATransportVehicle")) {
                    logger.log(Level.FINE, loadShip.getCtMethod().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = com.joedobo27.loadingunchained.LoadingUnchainedMod.loadIntoContainerInvalid(vehicle);");
                    loadIntoDraggedSuccesses[5] = 1;
                }
            }
        });

        // performerIsNotOnAVehicle() of cargoTransportationMethods always return false.
        CtMethod ctmPerformerIsNotOnAVehicle = cargoTransportationMethods.getCtClass().getMethod("performerIsNotOnAVehicle",
                "(Lcom/wurmonline/server/creatures/Creature;)Z");
        ctmPerformerIsNotOnAVehicle.setBody("return false;");

        // performerIsNotSeatedOnAVehicle() of cargoTransportationMethods always return false.
        CtMethod ctmPerformerIsNotSeatedOnAVehicle = cargoTransportationMethods.getCtClass().getMethod("performerIsNotSeatedOnAVehicle",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Seat;)Z");
        ctmPerformerIsNotSeatedOnAVehicle.setBody("return false;");

        evaluateChangesArray(loadIntoDraggedSuccesses, "loadIntoDragged");
        evaluateChangesArray(loadIntoTrashbinSuccesses, "loadIntoTrashbin");
    }

    @SuppressWarnings("ConstantConditions")
    private void menuAddLoadUnloadBytecode() throws NotFoundException, CannotCompileException {
        if (!boatInCart && !loadIntoDragged)
            return;
        itemBehaviour.getCtClass().instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals(cargoTransportationMethods.getCtClass().getName(), methodCall.getClassName()) && Objects.equals("getLoadUnloadActions", methodCall.getMethodName())) {
                    logger.log(Level.FINE, itemBehaviour.getCtClass().getName() + " method,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("$_ = com.joedobo27.loadingunchained.LoadingUnchainedMod.getLoadUnloadActionsHook($1, $2);");
                }
            }
        });
    }


    private void minimumStrengthBytecode() throws CannotCompileException, NotFoundException {
        if (minimumStrength < 19)
            return;
        final int[] minimumStrengthsuccesses = new int[]{0};
        cargoTransportationMethods.getCtClass().instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("strengthCheck", methodCall.getMethodName())) {
                    logger.log(Level.FINE, cargoTransportationMethods.getCtClass().getName() + " class,  edit call to " +
                            methodCall.getMethodName() + " at index " + methodCall.getLineNumber());
                    methodCall.replace("{ $2 = " + minimumStrength + "; $_ = $proceed($$); }");
                    minimumStrengthsuccesses[0] = 1;
                }
            }
        });
        evaluateChangesArray(minimumStrengthsuccesses, "minimumStrength");
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

    private static void evaluateChangesArray(int[] ints, String option) {
        boolean changesSuccessful = !Arrays.stream(ints).anyMatch(value -> value == 0);
        if (changesSuccessful) {
            logger.log(Level.INFO, option + " option changes SUCCESSFUL");
        } else {
            logger.log(Level.INFO, option + " option changes FAILURE");
            logger.log(Level.FINE, Arrays.toString(ints));
        }
    }
}

