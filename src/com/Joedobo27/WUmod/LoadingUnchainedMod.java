package com.Joedobo27.WUmod;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.effects.Effect;
import com.wurmonline.server.effects.EffectFactory;
import com.wurmonline.server.items.*;
import javassist.*;
import javassist.bytecode.*;
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
import java.util.spi.CalendarNameProvider;


@SuppressWarnings("unused")
public class LoadingUnchainedMod implements WurmMod, Initable, Configurable, ServerStartedListener {

    private static Logger logger = Logger.getLogger(LoadingUnchainedMod.class.getName());

    //<editor-fold desc="Configure controls.">
    private boolean loadAnyBin = true;
    private boolean moveItemsIntoBinWithinContainer = false;
    private boolean useCustomProximity = false;
    private int loadProximityRequired = 4;
    private boolean useCustomLoadTime = false;
    private int loadDurationTime = 100;
    private boolean loadIntoDragged = true;
    private boolean boatInCart = false;
    private boolean useBedInCart = false;
    private boolean useMagicChestInCart = false;
    private boolean useForgeInCart = false;
    private boolean craftWithinCart = false;
    private boolean loadAltar = false;
    private boolean useAltarInCart = false;
    private boolean loadOther = false;
    //</editor-fold>

    //<editor-fold desc="Bytecode objects">
    private static ClassPool pool;
    private static CtClass ctcSelf;
    private static CtClass ctcCargoTransportationMethods;
    private static ClassFile cfCargoTransportationMethods;
    private static ConstPool cpCargoTransportationMethods;
    private static ClassFile cfItem;
    private static ConstPool cpItem;
    private static CtClass ctcMethodsItems;
    private static ClassFile cfMethodsItems;
    private static ConstPool cpMethodsItems;
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
    //</editor-fold>

    public void configure(Properties properties) {
        loadAnyBin = Boolean.valueOf(properties.getProperty("loadAnyBin", Boolean.toString(loadAnyBin)));
        moveItemsIntoBinWithinContainer = Boolean.valueOf(properties.getProperty("moveItemsIntoBinWithinContainer",
                Boolean.toString(moveItemsIntoBinWithinContainer)));
        useCustomProximity = Boolean.valueOf(properties.getProperty("useCustomProximity", Boolean.toString(useCustomProximity)));
        loadProximityRequired = Integer.valueOf(properties.getProperty("loadProximityRequired", Integer.toString(loadProximityRequired)));
        loadProximityRequired = Math.max(loadProximityRequired, 1);
        loadProximityRequired = Math.min(loadProximityRequired, 160);
        useCustomLoadTime = Boolean.valueOf(properties.getProperty("useCustomLoadTime", Boolean.toString(useCustomLoadTime)));
        loadDurationTime = Integer.valueOf(properties.getProperty("loadDurationTime", Integer.toString(loadDurationTime)));
        loadIntoDragged = Boolean.valueOf(properties.getProperty("LoadIntoDragged", Boolean.toString(loadIntoDragged)));
        boatInCart = Boolean.valueOf(properties.getProperty("boatInCart", Boolean.toString(boatInCart)));
        useBedInCart = Boolean.valueOf(properties.getProperty("useBedInCart", Boolean.toString(useBedInCart)));
        useMagicChestInCart = Boolean.valueOf(properties.getProperty("useMagicChestInCart", Boolean.toString(useMagicChestInCart)));
        useForgeInCart = Boolean.valueOf(properties.getProperty("useForgeInCart", Boolean.toString(useForgeInCart)));
        craftWithinCart = Boolean.valueOf(properties.getProperty("craftWithinCart", Boolean.toString(craftWithinCart)));
        loadAltar = Boolean.valueOf(properties.getProperty("loadAltar", Boolean.toString(loadAltar)));
        useAltarInCart = Boolean.valueOf(properties.getProperty("useAltarInCart", Boolean.toString(useAltarInCart)));
        loadOther = Boolean.valueOf(properties.getProperty("loadOther", Boolean.toString(loadOther)));
    }

    @Override
    public void onServerStarted() {
        ArrayList<Boolean> optionSwitches = new ArrayList<>(Arrays.asList(useBedInCart, boatInCart, loadAltar, loadOther,
                craftWithinCart));
        try {
            int bedCnt = useBedReflection(optionSwitches);
            int boatInCartCnt = boatInCartReflection(optionSwitches);
            int altarCnt = loadAltarReflection(optionSwitches);
            int otherCnt = loadOtherReflection(optionSwitches);
            int craftWithinCartCnt = craftWithinCartReflection(optionSwitches);

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

            ArrayList<Boolean> optionSwitches = new ArrayList<>(Arrays.asList(loadAnyBin, moveItemsIntoBinWithinContainer,
                    useCustomLoadTime, useCustomProximity, loadIntoDragged, boatInCart, useBedInCart, useMagicChestInCart,
                    useForgeInCart, craftWithinCart, useAltarInCart));
            loadAnyBinBytecode(optionSwitches);
            moveItemsIntoBinWithinContainerBytecode(optionSwitches);
            useCustomLoadTimeBytecode(optionSwitches);
            useCustomProximityBytecode(optionSwitches);
            loadIntoDraggedBytecode(optionSwitches);
            boatInCartBytecode(optionSwitches);
            useBedInCartBytecode(optionSwitches);
            useMagicChestInCartBytecode(optionSwitches);
            useForgeInCartBytecode(optionSwitches);
            craftWithinCartBytecode(optionSwitches);
            useAltarInCartBytecode(optionSwitches);
        } catch (BadBytecode | NotFoundException | CannotCompileException e) {
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
        CtClass ctcItem = pool.get("com.wurmonline.server.items.Item");
        cfItem = ctcItem.getClassFile();
        cpItem = cfItem.getConstPool();
    }

    private void setJSMethodsItems() throws NotFoundException {
        ctcMethodsItems = pool.get("com.wurmonline.server.behaviours.MethodsItems");
        cfMethodsItems = ctcMethodsItems.getClassFile();
        cpMethodsItems = cfMethodsItems.getConstPool();
    }

    private void setJSItemBehaviour() throws NotFoundException {
        CtClass ctcItemBehaviour = pool.get("com.wurmonline.server.behaviours.ItemBehaviour");
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

    private void loadAnyBinBytecode(ArrayList<Boolean> optionSwitches) throws NotFoundException, CannotCompileException {
        if (!optionSwitches.get(0))
            return;
        // Alter targetIsNotEmptyContainerCheck() of cargoTransportationMethods.class to always return false.
        CtMethod ctmTargetIsNotEmptyContainerCheck = ctcCargoTransportationMethods.getMethod("targetIsNotEmptyContainerCheck",
                "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z");
        ctmTargetIsNotEmptyContainerCheck.setBody("{return false;}");
    }

    private void moveItemsIntoBinWithinContainerBytecode(ArrayList<Boolean> optionSwitches) throws BadBytecode {
        if (!optionSwitches.get(1))
            return;
        JDBByteCode jbt;
        JDBByteCode jbt1;
        String replaceByteResult;

        //<editor-fold desc="moveToItem() of Item.class changes">
            /*
            Lines 3975 to 4031 in moveToItem of Javap.exe dump.
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
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getTopParent:()J"),
                "08",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getWurmId:()J"),
                "", "002e", "08",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "isCrate:()Z"),
                "0026", "", "0020",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_String, "The %s needs to be on the ground."),
                "",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Class, "java/lang/Object"),
                "", "", "08",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getName:()Ljava/lang/String;"),
                "",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "com/wurmonline/server/utils/StringUtil.format:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
                "13", "",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                "13",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "", "")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "0000,000000,0000,000000,00,000000,0000,000000,000000,00,000000,000000,00,000000,00,00,0000,000000,00,000000,0000,00,000000,0000,000000,00,00",
                moveToItemIterator, "moveToItem");
        logger.log(Level.INFO, replaceByteResult);
    }

    private void useCustomLoadTimeBytecode(ArrayList<Boolean> optionSwitches)throws NotFoundException, CannotCompileException {
        if (!optionSwitches.get(2))
            return;
        // alter getLoadUnloadActionTime() of Actions.class to always return the properties value in loadDurationTime.
        CtMethod cmGetLoadUnloadActionTime = ctcActions.getMethod("getLoadUnloadActionTime", "(Lcom/wurmonline/server/creatures/Creature;)I");
        cmGetLoadUnloadActionTime.setBody("{ return " + loadDurationTime + "; }");
        logger.log(Level.INFO, "Custom load time set.");
    }

    private void useCustomProximityBytecode(ArrayList<Boolean> optionSwitches) throws  NotFoundException, CannotCompileException {
        if (!optionSwitches.get(3))
            return;
        // alter getLoadActionDistance() of  cargoTransportationMethods.class to return properties value in loadProximityRequired.
        CtMethod ctmGetLoadActionDistance = ctcCargoTransportationMethods.getMethod("getLoadActionDistance",
                "(Lcom/wurmonline/server/items/Item;)I");
        ctmGetLoadActionDistance.setBody("{return " + loadProximityRequired + ";}");
    }

    private void loadIntoDraggedBytecode(ArrayList<Boolean> optionSwitches) throws BadBytecode, NotFoundException, CannotCompileException {
        if (!optionSwitches.get(4))
            return;
        String replaceByteResult;

        //performerIsNotOnATransportVehicle() of cargoTransportationMethods: Removed "vehicle == null ||".
        setPerformerIsNotOnATransportVehicle(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Vehicle;)Z",
                "performerIsNotOnATransportVehicle");
        replaceByteResult = JDBByteCode.byteCodeFindReplace("2B,C6000A,2B", "2B,C6000A", "00,000000", performerIsNotOnATransportVehicleIterator, "performerIsNotOnATransportVehicle");
        logger.log(Level.INFO, replaceByteResult);

        // performerIsNotOnAVehicle() of cargoTransportationMethods always return false.
        CtMethod ctmPerformerIsNotOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotOnAVehicle",
                "(Lcom/wurmonline/server/creatures/Creature;)Z");
        ctmPerformerIsNotOnAVehicle.setBody("{return false;}");

        // performerIsNotSeatedOnAVehicle() of cargoTransportationMethods always return false.
        CtMethod ctmPerformerIsNotSeatedOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotSeatedOnAVehicle",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Seat;)Z");
        ctmPerformerIsNotSeatedOnAVehicle.setBody("{return false;}");
    }

    private void boatInCartBytecode(ArrayList<Boolean> optionSwitches) throws BadBytecode, CannotCompileException, NotFoundException {
        if (!optionSwitches.get(5))
            return;
        JDBByteCode jbt;
        JDBByteCode jbt1;
        String replaceByteResult;

        /*
        List toReturn = new LinkedList();
        if (!target.getTemplate().isTransportable() || !target.getRealTemplate().isTransportable())
            return toReturn;
        if (player.getVehicle() == -10L & player.getDraggedItem().getWurmId() == -10L)
            return toReturn;
        com.wurmonline.server.behaviours.Vehicle vehicle;
        if (player.getVehicle() != -10)
            vehicle = Vehicles.getVehicleForId(player.getVehicle());
        if (player.getDraggedItem().getWurmId() != -10L)
            vehicle = Vehicles.getVehicleForId(player.getDraggedItem().getWurmId());
        com.wurmonline.server.items.Item vehicleItem = Items.getItem(vehicle.getWurmid());
        if (vehicle != null && !vehicle.creature && !vehicle.isChair() &&
                (MethodsItems.mayUseInventoryOfVehicle(player, vehicleItem) || vehicleItem.getLockId() == -10L || Items.isItemDragged(vehicleItem))) {
            toReturn.add(Actions.actionEntrys[605]);
            if (target.getTopParent() != target.getWurmId())
                toReturn.add(Actions.actionEntrys[606]);
        }
        */
        // Creature player, Item target
        String boatAndDrag = "List toReturn = new LinkedList();";
        boatAndDrag = boatAndDrag.concat("if (!$2.getTemplate().isTransportable() || !$2.getRealTemplate().isTransportable()){return toReturn;}");
        boatAndDrag = boatAndDrag.concat("if ($1.getVehicle() == -10L & $1.getDraggedItem().getWurmId() == -10L){return toReturn;}");
        boatAndDrag = boatAndDrag.concat("com.wurmonline.server.behaviours.Vehicle vehicle;");
        boatAndDrag = boatAndDrag.concat("if ($1.getVehicle() != -10){vehicle = Vehicles.getVehicleForId($1.getVehicle());}");
        boatAndDrag = boatAndDrag.concat("if ($1.getDraggedItem().getWurmId() != -10L){vehicle = Vehicles.getVehicleForId($1.getDraggedItem().getWurmId());}");
        boatAndDrag = boatAndDrag.concat("com.wurmonline.server.items.Item vehicleItem = Items.getItem(vehicle.getWurmid());");
        boatAndDrag = boatAndDrag.concat("if (vehicle != null && !vehicle.creature && !vehicle.isChair() && ");
        boatAndDrag = boatAndDrag.concat("(MethodsItems.mayUseInventoryOfVehicle($1, vehicleItem) || vehicleItem.getLockId() == -10L || Items.isItemDragged(vehicleItem))) {");
        boatAndDrag = boatAndDrag.concat("toReturn.add(Actions.actionEntrys[605]);");
        boatAndDrag = boatAndDrag.concat("if ($2.getTopParent() != $2.getWurmId()){toReturn.add(Actions.actionEntrys[606]);}");
        boatAndDrag = boatAndDrag.concat("}return toReturn;");

        CtMethod cmGetLoadUnloadActions = ctcCargoTransportationMethods.getMethod("getLoadUnloadActions", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Ljava/util/List;");
        cmGetLoadUnloadActions.setBody("{ " + boatAndDrag + " }");


        //<editor-fold desc="getLoadUnloadActions() of CargoTransportationMethods.class changes.">
            /*
            replaced lines 107-167 in cargoTransportationMethods of the javap.exe dump.
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

        //</editor-fold>
        setGetLoadUnloadActions(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Ljava/util/List;",
                "getLoadUnloadActions");
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD,
                Opcode.INVOKEVIRTUAL, Opcode.SIPUSH, Opcode.IF_ICMPEQ, Opcode.ALOAD_2, Opcode.ARETURN, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                Opcode.IFEQ, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.SIPUSH, Opcode.IF_ICMPNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                Opcode.ASTORE, Opcode.ALOAD, Opcode.IFNONNULL, Opcode.ALOAD_2, Opcode.ARETURN, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.IFNE, Opcode.ALOAD_2, Opcode.ARETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.isBoat:()Z"),
                "0010","05",
                JDBByteCode.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.getTemplateId:()I"),
                "0355", "0005", "", "", "",
                JDBByteCode.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.isUnfinished:()Z"),
                "0025", "05",
                JDBByteCode.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.getTemplateId:()I"),
                "0355", "001a","",
                JDBByteCode.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.getRealTemplate:()Lcom/wurmonline/server/items/ItemTemplate;"),
                "06", "06","0005","","","06",
                JDBByteCode.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/ItemTemplate.isBoat:()Z"),
                "0005", "", "")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(),jbt.getOpcodeOperand(),
                "00,000000,000000,0000,000000,000000,000000,00,00,00,000000,000000,0000,000000,000000,000000,00,000000,0000,0000,000000,00,00,0000,000000,000000,00,00",
                getLoadUnloadActionsIterator, "getLoadUnloadActions");
        logger.log(Level.INFO, replaceByteResult);
        getLoadUnloadActionsMInfo.rebuildStackMapIf6(pool, cfCargoTransportationMethods);
        */
    }

    private void useBedInCartBytecode(ArrayList<Boolean> optionSwitches) throws BadBytecode {
        if (!optionSwitches.get(6))
            return;
        JDBByteCode jbt;
        JDBByteCode jbt1;
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
        addBedOptionsIterator.insertExGap(373, 18);
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("04", "0174","04",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "016c","04",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "0161", "", "", "", "", "", "", "", "", "", "", "", "")));
        jbt.setOpcodeOperand();

        jbt1 = new JDBByteCode();
        jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LCMP, Opcode.IFNE)));
        jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("04", "0165","04",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "015d","04",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpItemBehaviour, ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "0152","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTopParent:()J"),
                "",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getWurmId:()J"),
                "","0146")));
        jbt1.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace( jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                jbt1.getOpcodeOperand(), addBedOptionsIterator, "addBedOptions");
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
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.INVOKEVIRTUAL, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.RETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList(
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/Throwable;)V"),
                "","","","","","","","","","","","","","","","","","","")));
        jbt.setOpcodeOperand();
        jbt1 = new JDBByteCode();
        jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GOTO, Opcode.ALOAD_3, Opcode.GETSTATIC, Opcode.SIPUSH,
                Opcode.AALOAD, Opcode.INVOKEINTERFACE, Opcode.NOP, Opcode.POP)));
        jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("0012","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Fieldref,"com/wurmonline/server/behaviours/Actions.actionEntrys:[Lcom/wurmonline/server/behaviours/ActionEntry;"),
                "0145","",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref, "java/util/List.add:(Ljava/lang/Object;)Z") + "0200",
                "","")));
        jbt1.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(),
                "00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00",
                jbt1.getOpcodeOperand(),
                addBedOptionsIterator, "addBedOptions");
        logger.log(Level.INFO, replaceByteResult);
        //getAddBedOptionsAttribute().computeMaxStack();
        addBedOptionsMInfo.rebuildStackMapIf6(pool, cfItemBehaviour);

        //<editor-fold desc="askSleep() of MethodsItems.class changes">
            /*
            Working with lines 105 - 180 in askSleep of javap.exe dump.
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
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD,
                Opcode.INVOKEVIRTUAL, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1,
                Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileX:()I"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileY:()I"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.isOnSurface:()Z"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/Zones.getTileOrNull:(IIZ)Lcom/wurmonline/server/zones/VolaTile;"),
                "05","05","00b7","05",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "000e","05",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"You would get no sleep outside tonight."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","","05",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isFinished:()Z"),
                "000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"The house is too windy to provide protection."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "00,000000,00,000000,00,000000,000000,0000,0000,000000,0000,000000,000000,0000,000000,000000,000000,00,000000,000000,000000,00,00,0000,000000,000000,000000,00,000000,000000,000000,00,00",
                askSleepIterator, "askSleep");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="askSleep() of MethodsItems.class changes">
            /*
            Removing indexes 307 - 365 in askSleep of javap.exe dump.
            Removed-
                else {
                    MethodsItems.logger.log(Level.WARNING, "Why is tile for bed at " + target.getTileX() + "," + target.getTileY() + "," + target.isOnSurface() + " null?");
                }
            */
        //</editor-fold>
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GETSTATIC, Opcode.GETSTATIC, Opcode.NEW, Opcode.DUP, Opcode.LDC_W,
                Opcode.INVOKESPECIAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2,
                Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList(
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"logger:Ljava/util/logging/Logger;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"java/util/logging/Level.WARNING:Ljava/util/logging/Level;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Class,"java/lang/StringBuilder"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"Why is tile for bed at "),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.<init>:(Ljava/lang/String;)V"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileX:()I"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,","),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileY:()I"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,","),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.isOnSurface:()Z"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Z)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String," null?"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.toString:()Ljava/lang/String;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;)V")
        )));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "000000,000000,000000,00,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,000000,000000",
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
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTopParent:()J"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getWurmId:()J"),
                "","000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"The bed needs to be on the ground."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "00,000000,00,000000,00,000000,00,000000,000000,000000,00,00",
                sleepIterator, "sleep");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="sleep() of MethodsItems.class changes.">
            /*
            Removing indexes 170 - 223 in sleep of javap.exe dump.
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
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                Opcode.IRETURN, Opcode.ALOAD,Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("06",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "000e","06",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"You would get no sleep outside tonight."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","","06",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isFinished:()Z"),
                "000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"The house is too windy to provide protection."),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "0000,000000,000000,0000,000000,000000,000000,00,000000,000000,000000,00,00,0000,000000,000000,000000,00,000000,000000,000000,00,00",
                sleepIterator, "sleep");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="sleep() of MethodsItems.class changes.">
            /*
            Removing indexes 291 - 349 in sleep of javap.exe dump.
            -Removed
                else {
                    MethodsItems.logger.log(Level.WARNING, "Why is tile for bed at " + target.getTileX() + "," + target.getTileY() + "," + target.isOnSurface() + " null?");
                }
            */
        //</editor-fold>
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GETSTATIC, Opcode.GETSTATIC, Opcode.NEW, Opcode.DUP, Opcode.LDC_W,
                Opcode.INVOKESPECIAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2,
                Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList(
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"logger:Ljava/util/logging/Logger;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"java/util/logging/Level.WARNING:Ljava/util/logging/Level;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Class,"java/lang/StringBuilder"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"Why is tile for bed at "),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.<init>:(Ljava/lang/String;)V"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileX:()I"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,","),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileY:()I"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,","),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.isOnSurface:()Z"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Z)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String," null?"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.toString:()Ljava/lang/String;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;)V")
        )));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "000000,000000,000000,00,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,00,000000,000000,000000,000000,000000,000000",
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
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC,
                Opcode.ASTORE_2, Opcode.ALOAD_2, Opcode.IFNULL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ACONST_NULL, Opcode.ASTORE_3,
                Opcode.ALOAD_3, Opcode.IFNULL, Opcode.ALOAD_3, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD_3, Opcode.INVOKEVIRTUAL, Opcode.IFEQ,
                Opcode.ICONST_1, Opcode.IRETURN, Opcode.ICONST_0)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"getTilePos:()Lcom/wurmonline/math/TilePos;"),
                "",
                JDBByteCode.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"isOnSurface:()Z"),
                JDBByteCode.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/Zones.getTileOrNull:(Lcom/wurmonline/math/TilePos;Z)Lcom/wurmonline/server/zones/VolaTile;"),
                "","","000a","",
                JDBByteCode.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                "0004","","","","0013","",
                JDBByteCode.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                "000c","",
                JDBByteCode.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isFinished:()Z"),
                "0005","","","")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "00,000000,00,000000,000000,00,00,000000,00,000000,000000,00,00,00,000000,00,000000,000000,00,000000,000000,00,00,04",
                mayUseBedIterator, "mayUseBed");
        logger.log(Level.INFO, replaceByteResult);
        //getMayUseBedAttribute().computeMaxStack();
        mayUseBedMInfo.rebuildStackMapIf6(pool, cfItem);
    }

    private void useMagicChestInCartBytecode(ArrayList<Boolean> optionSwitches) throws BadBytecode {
        if (!optionSwitches.get(7))
            return;
        JDBByteCode jbt;
        JDBByteCode jbt1;
        String replaceByteResult;

        //<editor-fold desc="Replace code">
            /*
            Removing indexes 58 - 69 in moveToItem of javap.exe dump.
            Removed-
                target.getTemplateId() == 664 ||
            */
        //</editor-fold>
        setMoveToItem(cfItem, "(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.SIPUSH, Opcode.IF_ICMPEQ)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("08",
                JDBByteCode.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref,"getTemplateId:()I"),
                "0298","000b")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "0000,000000,000000,000000",
                moveToItemIterator, "moveToItem");
        logger.log(Level.INFO, replaceByteResult);
    }

    private void useForgeInCartBytecode(ArrayList<Boolean> optionSwitches) throws BadBytecode, NotFoundException, CannotCompileException {
        if (!optionSwitches.get(8))
            return;
        JDBByteCode jbt;
        JDBByteCode jbt1;
        String replaceByteResult;


        //<editor-fold desc="startFire() changes">
            /*
            Removing indexes 110 - 162 in startFire of javap.exe dump.
            Removed-
                if (target.getTemplate().isTransportable() && target.getTopParent() != target.getWurmId()) {
                    final String message = StringUtil.format("The %s must be on the ground before you can light it.", target.getName());
                    performer.getCommunicator().sendNormalServerMessage(message, (byte)3);
                    return true;
                }
            */
        //</editor-fold>
        setStartFire(cfMethodsItems, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;F)Z", "startFire");
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ,
                Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LCMP, Opcode.IFEQ, Opcode.LDC_W,
                Opcode.ICONST_1, Opcode.ANEWARRAY, Opcode.DUP, Opcode.ICONST_0, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.AASTORE,
                Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.ICONST_3, Opcode.INVOKEVIRTUAL,
                Opcode.ICONST_1, Opcode.IRETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTemplate:()Lcom/wurmonline/server/items/ItemTemplate;"),
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/ItemTemplate.isTransportable:()Z"),
                "002e","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTopParent:()J"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getWurmId:()J"),
                "","0022",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"The %s must be on the ground before you can light it."),
                "","0003","","","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getName:()Ljava/lang/String;"),
                "",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/utils/StringUtil.format:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
                "09","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                "09","",
                JDBByteCode.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;B)V"),
                "","")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
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
        ctmTargetIsOnFireCheck.setBody("{return false;}");

        // Alter loadCargo() of cargoTransportationMethods.class to destory fire effect on furnace load.
        CtMethod ctmLoadCargo = ctcCargoTransportationMethods.getMethod("loadCargo",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
        String s = "if ($2.isOnFire()) {";
        s = s.concat("com.wurmonline.server.zones.Zone zone1 = ");
        s = s.concat("com.wurmonline.server.zones.Zones.getZone($1.getTileX(), $1.getTileY(), $1.isOnSurface());");
        s = s.concat("com.wurmonline.server.effects.Effect[] targetEffects = $2.getEffects();");
        s = s.concat("for (int i = 0; i < targetEffects.length; i++) {");
        s = s.concat("if (targetEffects[i].getType() == (short)0) {");
        s = s.concat("zone1.removeEffect(targetEffects[i]);}}}");
        // Insert code right after the "You finish loading..." message.
        ctmLoadCargo.insertAt(291,"{" + s + "}");

        // Alter unloadCargo() of cargoTransportationMethods.class to create a fire effect on furnace unload.
        CtMethod ctmUnloadCargo = ctcCargoTransportationMethods.getMethod("unloadCargo",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
        s = "if ($2.isOnFire()) {";
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

    private void craftWithinCartBytecode(ArrayList<Boolean> optionSwitches) throws BadBytecode {
        if (!optionSwitches.get(9))
            return;
        JDBByteCode jbt;
        JDBByteCode jbt1;
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
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.IFEQ)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.isUseOnGroundOnly:()Z"),
                "0043")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "00,000000,000000", addCreationWindowOptionIterator, "addCreationWindowOption");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="Replace code">
            /*
            Removing indexes 42 - 51 in addCreationWindowOption of javap.exe dump.
            Removed-
                if (target.getTopParent() == target.getWurmId()) {
            */
        //</editor-fold>
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                Opcode.LCMP, Opcode.IFNE)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTopParent:()J"),
                "",
                JDBByteCode.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getWurmId:()J"),
                "","0029")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "00,000000,00,000000,00,000000", addCreationWindowOptionIterator, "addCreationWindowOption");
        logger.log(Level.INFO, replaceByteResult);
        //getAddCreationWindowOptionAttribute().computeMaxStack();
        addCreationWindowOptionMInfo.rebuildStackMapIf6(pool, cfItemBehaviour);
    }

    private void useAltarInCartBytecode(ArrayList<Boolean> optionSwitches) throws BadBytecode {
        if (!optionSwitches.get(10))
            return;
        JDBByteCode jbt;
        JDBByteCode jbt1;
        String replaceByteResult;

        //<editor-fold desc="pray() of MethodsReligion.Class changes.">
            /*
            // lines 136-158 in pray of MethodsReligion removed.
            if (altar.getParentId() != -10L) {
                performer.getCommunicator().sendNormalServerMessage("The altar needs to be on the ground to be used.");
                return true;
            }
            */
        //</editor-fold>
        setPray(cfMethodsReligion,
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                "pray");
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                Opcode.IRETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getParentId:()J"),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Long,"-10"),
                "","000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_String,"The altar needs to be on the ground to be used."),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "00,000000,000000,00,000000,00,000000,000000,000000,00,00",prayIterator,"pray");
        logger.log(Level.INFO, replaceByteResult);

        //<editor-fold desc="sacrifice() of MethodsReligion.class changes.">
            /*
            // lines 162-184 in sacrifice of MethodsReligion removed.
            if (altar.getParentId() != -10L) {
                performer.getCommunicator().sendNormalServerMessage("The altar needs to be on the ground to be used.");
                return true;
            }
            */
        //</editor-fold>
        setSacrifice(cfMethodsReligion,
                "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z",
                "sacrifice");
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                Opcode.IRETURN)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getParentId:()J"),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Long,"-10"),
                "","000f","",
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_String,"The altar needs to be on the ground to be used."),
                JDBByteCode.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "","")));
        jbt.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                "00,000000,000000,00,000000,00,000000,000000,000000,00,00",sacrificeIterator,"sacrifice");
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
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ALOAD_2,
                Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_1, Opcode.FLOAD, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("04",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getParentId:()J"),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Long,"-10"),
                "","000f","",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_String,"The altar needs to be on the ground to be used.").substring(2),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "0098","","04","","","06",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/behaviours/MethodsReligion.holdSermon:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z"),
                "07","0089")));
        jbt.setOpcodeOperand();
        jbt1 = new JDBByteCode();
        jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.ALOAD_2,Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_1, Opcode.FLOAD,
                Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
        jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("","","","","","","","","","","","","","","","","","","","","","","","",
                "","04","","","06",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/behaviours/MethodsReligion.holdSermon:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z"),
                "07","0089")));
        jbt1.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(), jbt1.getOpcodeOperand(),
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
        jbt = new JDBByteCode();
        jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ALOAD_2,
                Opcode.ALOAD_3, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
        jbt.setOperandStructure(new ArrayList<>(Arrays.asList("04",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getParentId:()J"),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Long,"-10"),
                "","000f","",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_String,"The altar needs to be on the ground to be used.").substring(2),
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                "0062","","",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/behaviours/MethodsReligion.sendRechargeQuestion:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z"),
                "07","0058")));
        jbt.setOpcodeOperand();
        jbt1 = new JDBByteCode();
        jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.ALOAD_2,
                Opcode.ALOAD_3, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
        jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("","","","","","","","","","","","","","","","","","","","","","","","",
                "","",
                JDBByteCode.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/behaviours/MethodsReligion.sendRechargeQuestion:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z"),
                "07","0058")));
        jbt1.setOpcodeOperand();
        replaceByteResult = JDBByteCode.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(), jbt1.getOpcodeOperand(),
                actionDBIterator, "action");
        logger.log(Level.INFO, replaceByteResult);
    }
    //</editor-fold>

    //<editor-fold desc="Reflection methods section.">
    private int useBedReflection(ArrayList<Boolean> optionSwitches) throws IllegalAccessException, NoSuchFieldException {
        int bedCnt = 0;
        if (!optionSwitches.get(0))
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

    private int boatInCartReflection(ArrayList<Boolean> optionSwitches) throws IllegalAccessException, NoSuchFieldException {
        int boatInCartCnt = 0;
        if (!optionSwitches.get(1))
            return boatInCartCnt;
        Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
        for (ItemTemplate template : fieldTemplates.values()) {
            Integer templateId = template.getTemplateId();
            Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
            if (template.isFloating()) {
                ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                boatInCartCnt++;
            }
        }
        return boatInCartCnt;
    }

    private int loadAltarReflection(ArrayList<Boolean> optionSwitches) throws IllegalAccessException, NoSuchFieldException {
        int altarCnt = 0;
        if (!optionSwitches.get(2))
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

    private int loadOtherReflection(ArrayList<Boolean> optionSwitches) throws IllegalAccessException, NoSuchFieldException {
        int otherCnt = 0;
        if (!optionSwitches.get(3))
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

    private int craftWithinCartReflection(ArrayList<Boolean> optionSwitches) throws IllegalAccessException, NoSuchFieldException {
        int craftWithinCartCnt = 0;
        if (!optionSwitches.get(4))
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
    private void setActionDB(ClassFile cf, String desc, String name){
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

    private void setSacrifice(ClassFile cf, String desc, String name){
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

    private void setPray(ClassFile cf, String desc, String name){
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

    private void setAddBedOptions(ClassFile cf, String desc, String name) {
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

    private void setMoveToItem(ClassFile cf, String desc, String name) {
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

    public void setLoadCargo(ClassFile cf, String desc, String name) {
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

    public void setUnloadCargo(ClassFile cf, String desc, String name) {
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

    private void setPerformerIsNotOnATransportVehicle(ClassFile cf, String desc, String name) {
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

    private void setGetLoadUnloadActions(ClassFile cf, String desc, String name) {
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

    private void setAskSleep(ClassFile cf, String desc, String name) {
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

    private void setSleep(ClassFile cf, String desc, String name) {
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

    private void setMayUseBed(ClassFile cf, String desc, String name){
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

    private void setStartFire(ClassFile cf, String desc, String name){
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

    private void setSetFire(ClassFile cf, String desc, String name){
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

