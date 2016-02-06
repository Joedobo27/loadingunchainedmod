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
        if (useBedInCart || boatInCart || loadAltar || loadOther || craftWithinCart) {
            try {
                Map<Integer, ItemTemplate> fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                        ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
                int bedCnt = 0;
                int boatInCartCnt = 0;
                int altarCnt = 0;
                int otherCnt = 0;
                int craftWithinCartCnt =0;
                for (ItemTemplate template : fieldTemplates.values()) {
                    Integer templateId = template.getTemplateId();
                    if (useBedInCart) {
                        Field fieldUseOnGroundOnly = ReflectionUtil.getField(ItemTemplate.class, "useOnGroundOnly");
                        if (templateId == ItemList.bedStandard || templateId == ItemList.canopyBed) {
                            if (template.isUseOnGroundOnly()) {
                                ReflectionUtil.setPrivateField(template, fieldUseOnGroundOnly, Boolean.FALSE);
                                bedCnt++;
                            }
                        }
                    }
                    if (boatInCart) {
                        Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                        if (template.isFloating()){
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                            boatInCartCnt++;
                        }
                    }
                    if (loadAltar){
                        Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                        if (templateId == ItemList.altarWood || templateId == ItemList.altarGold || templateId == ItemList.altarSilver ||
                                templateId == ItemList.altarStone){
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                            altarCnt++;
                        }

                    }
                    if (loadOther){
                        Field fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                        if (templateId == ItemList.trashBin){
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                            otherCnt++;
                        }
                    }
                    if (craftWithinCart){
                        Field fieldUseOnGroundOnly = ReflectionUtil.getField(ItemTemplate.class, "useOnGroundOnly");
                        if (templateId == ItemList.loom || templateId == ItemList.spinningWheel) {
                            if (template.isUseOnGroundOnly()) {
                                ReflectionUtil.setPrivateField(template, fieldUseOnGroundOnly, Boolean.FALSE);
                                craftWithinCartCnt++;
                            }
                        }
                    }

                }
                logger.log(Level.INFO, "useBedInCart: " + bedCnt);
                logger.log(Level.INFO, "boatInCart: " + boatInCartCnt);
                logger.log(Level.INFO, "loadAltar: " + altarCnt);
                logger.log(Level.INFO, "loadOther: " + otherCnt);
                logger.log(Level.INFO, "craftWithinCart: " + craftWithinCartCnt);
            }catch(NoSuchFieldException | IllegalAccessException e){
                logger.log(Level.WARNING, e.toString());
            }
        }
    }

    @Override
    public void init() {
        String replaceByteResult;
        String printByteResult;
        jaseBT jbt;
        jaseBT jbt1;

        try {
            ClassPool pool = HookManager.getInstance().getClassPool();

            CtClass ctcSelf = pool.get(this.getClass().getName());

            CtClass ctcCargoTransportationMethods = pool.get("com.wurmonline.server.behaviours.CargoTransportationMethods");
            ClassFile cfCargoTransportationMethods = ctcCargoTransportationMethods.getClassFile();
            ConstPool cpCargoTransportationMethods = cfCargoTransportationMethods.getConstPool();

            CtClass ctcItem = pool.get("com.wurmonline.server.items.Item");
            ClassFile cfItem = ctcItem.getClassFile();
            ConstPool cpItem = cfItem.getConstPool();

            CtClass ctcMethodsItems = pool.get("com.wurmonline.server.behaviours.MethodsItems");
            ClassFile cfMethodsItems = ctcMethodsItems.getClassFile();
            ConstPool cpMethodsItems = cfMethodsItems.getConstPool();

            CtClass ctcItemBehaviour = pool.get("com.wurmonline.server.behaviours.ItemBehaviour");
            ClassFile cfItemBehaviour = ctcItemBehaviour.getClassFile();
            ConstPool cpItemBehaviour = cfItemBehaviour.getConstPool();

            if (!loadOnlyEmptyBins){
                CtMethod ctmTargetIsNotEmptyContainerCheck = ctcCargoTransportationMethods.getMethod("targetIsNotEmptyContainerCheck",
                        "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z");
                ctmTargetIsNotEmptyContainerCheck.setBody("{return false;}");
            }

            if (moveItemsIntoBinWithinContainer){
                jbt = new jaseBT();
                // Lines 3932 to 3987 in moveToItem of Javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList( Opcode.ALOAD,Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                        Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.IFNULL, Opcode.LDC_W,
                        Opcode.ICONST_1, Opcode.ANEWARRAY, Opcode.DUP, Opcode.ICONST_0, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.AASTORE,
                        Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                        Opcode.ICONST_0, Opcode.IRETURN)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("08",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getTopParent", "()J", null,"com/wurmonline/server/items/Item",null),"08",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getWurmId", "()J", null, "com/wurmonline/server/items/Item",null),"","002e","08",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "isCrate", "()Z", null, "com/wurmonline/server/items/Item",null), "0026","","0020",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_String, null, null, "The %s needs to be on the ground.", null,null),"",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Class, null, null, "java/lang/Object", null,null),"","","08",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getName", "()Ljava/lang/String;", null,"com/wurmonline/server/items/Item",null),"",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", null, "java/lang/String",null),
                        "13","",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getCommunicator", "()Lcom/wurmonline/server/creatures/Communicator;", null, "com/wurmonline/server/creatures/Creature",null),
                        "13",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "sendNormalServerMessage", "(Ljava/lang/String;)V", null, "com/wurmonline/server/creatures/Communicator",null),
                        "", "")));
                jbt.setOpcodeOperand();
                setMoveToItem(cfItem, "(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
                    replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(),jbt.getOpcodeOperand(),
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
                jbt = new jaseBT();
                // replaced lines 107-167 in cargoTransportationMethods of the javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD,
                        Opcode.INVOKEVIRTUAL, Opcode.SIPUSH, Opcode.IF_ICMPEQ, Opcode.ALOAD_2, Opcode.ARETURN, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                        Opcode.IFEQ, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.SIPUSH, Opcode.IF_ICMPNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                        Opcode.ASTORE, Opcode.ALOAD, Opcode.IFNONNULL, Opcode.ALOAD_2, Opcode.ARETURN, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                        Opcode.IFNE, Opcode.ALOAD_2, Opcode.ARETURN)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                        jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "isBoat", "()Z", null, "com/wurmonline/server/items/Item", null),
                        "0010","05",
                        jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "getTemplateId", "()I", null, "com/wurmonline/server/items/Item", null),
                        "0355", "0005", "", "", "",
                        jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "isUnfinished", "()Z", null, "com/wurmonline/server/items/Item", null),
                        "0025", "05",
                        jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "getTemplateId", "()I", null, "com/wurmonline/server/items/Item", null),
                        "0355", "001a","",
                        jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "getRealTemplate", "()Lcom/wurmonline/server/items/ItemTemplate;", null, "com/wurmonline/server/items/Item", null),
                        "06", "06","0005","","","06",
                        jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "isBoat", "()Z", null, "com/wurmonline/server/items/ItemTemplate", null),
                        "0005", "", "")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(),jbt.getOpcodeOperand(),
                        "00,000000,000000,0000,000000,000000,000000,00,00,00,000000,000000,0000,000000,000000,000000,00,000000,0000,0000,000000,00,00,0000,000000,000000,00,00",
                        getGetLoadUnloadActionsIterator(), "getLoadUnloadActions");
                //<editor-fold desc="Replaced code">
                /*
                2b,b6027c,990010,1905,b6031f,110355,9f0005,2c,b0,2b,b6042d,990025,1905,b6031f,110355,a0001a,2b,b60433,3a06,1906,c70005,2c,b0,1906,b60436,9a0005,2c,b0
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


                jbt = new jaseBT();
                // Working with lines 17-35 in addBedOptions of the javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                        Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                        Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("04", "0174","04",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"getStructure", "()Lcom/wurmonline/server/structures/Structure;", null, "com.wurmonline.server.zones.VolaTile", null),
                        "016c","04",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"getStructure", "()Lcom/wurmonline/server/structures/Structure;", null, "com.wurmonline.server.zones.VolaTile", null),
                        jaseBT.findConstantPoolReference(cpItemBehaviour, ConstPool.CONST_Methodref,"isTypeHouse", "()Z", null, "com/wurmonline/server/structures/Structure", null),
                        "0161", "", "", "", "", "", "", "", "", "", "", "", "")));
                jbt.setOpcodeOperand();

                jbt1 = new jaseBT();
                jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                        Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                        Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LCMP, Opcode.IFNE)));
                jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("04", "0165","04",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"getStructure", "()Lcom/wurmonline/server/structures/Structure;", null, "com.wurmonline.server.zones.VolaTile", null),
                        "015d","04",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"getStructure", "()Lcom/wurmonline/server/structures/Structure;", null, "com.wurmonline.server.zones.VolaTile", null),
                        jaseBT.findConstantPoolReference(cpItemBehaviour, ConstPool.CONST_Methodref,"isTypeHouse", "()Z", null, "com/wurmonline/server/structures/Structure", null),
                        "0152","",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"getTopParent","()J", null,"com/wurmonline/server/items/Item", null),
                        "",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"getWurmId","()J", null,"com/wurmonline/server/items/Item", null),
                        "","0146")));
                jbt1.setOpcodeOperand();

                replaceByteResult = jaseBT.byteCodeFindReplace( jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                        jbt1.getOpcodeOperand(), getAddBedOptionsIterator(), "addBedOptions");
                //<editor-fold desc="Replaced code">
                /*
                Added "&& target.getTopParent() == target.getWurmId()" to the logic statement:
                "if (t != null && t.getStructure() != null && t.getStructure().isTypeHouse()".
                If the bed is on ground and in a house use renting.
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);


                jbt = new jaseBT();
                // Working with lines 358-361 in addBedOptions of the javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.INVOKEVIRTUAL, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                        Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                        Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.RETURN)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList(
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"log", "(Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/Throwable;)V",null,"java/util/logging/Logger", null),
                        "","","","","","","","","","","","","","","","","","","")));
                jbt.setOpcodeOperand();

                jbt1 = new jaseBT();
                jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GOTO, Opcode.ALOAD_3, Opcode.GETSTATIC, Opcode.SIPUSH,
                        Opcode.AALOAD, Opcode.INVOKEINTERFACE, Opcode.NOP, Opcode.POP)));
                jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("0012","",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Fieldref,"actionEntrys", "[Lcom/wurmonline/server/behaviours/ActionEntry;", null, "com/wurmonline/server/behaviours/Actions", null),
                        "0145","",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_InterfaceMethodref, "add", "(Ljava/lang/Object;)Z", null, "java/util/List", "0200"),
                        "","")));
                jbt1.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(),
                        "00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00",
                        jbt1.getOpcodeOperand(),
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
                jbt = new jaseBT();
                // Working with lines 105 - 180 in askSleep of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                        Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD,
                        Opcode.INVOKEVIRTUAL, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1,
                        Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                        Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTileX","()I",null,"com/wurmonline/server/items/Item",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTileY","()I",null,"com/wurmonline/server/items/Item",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"isOnSurface","()Z",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTileOrNull","(IIZ)Lcom/wurmonline/server/zones/VolaTile;", null, "com/wurmonline/server/zones/Zones",null),
                        "05","05","00b7","05",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getStructure","()Lcom/wurmonline/server/structures/Structure;",null,"com/wurmonline/server/zones/VolaTile",null),
                        "000e","05",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getStructure","()Lcom/wurmonline/server/structures/Structure;",null,"com/wurmonline/server/zones/VolaTile",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"isTypeHouse", "()Z",null,"com/wurmonline/server/structures/Structure",null),
                        "000f","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getCommunicator","()Lcom/wurmonline/server/creatures/Communicator;",null,"com/wurmonline/server/creatures/Creature",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,"You would get no sleep outside tonight.",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"sendNormalServerMessage","(Ljava/lang/String;)V",null,"com/wurmonline/server/creatures/Communicator",null),
                        "","","05",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getStructure","()Lcom/wurmonline/server/structures/Structure;",null,"com/wurmonline/server/zones/VolaTile",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"isFinished","()Z",null,"com/wurmonline/server/structures/Structure",null),
                        "000f","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getCommunicator","()Lcom/wurmonline/server/creatures/Communicator;",null,"com/wurmonline/server/creatures/Creature",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,"The house is too windy to provide protection.",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"sendNormalServerMessage","(Ljava/lang/String;)V",null,"com/wurmonline/server/creatures/Communicator",null),
                        "","")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
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
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                
                jbt = new jaseBT();
                // Removing indexes 307 - 365 in askSleep of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GETSTATIC, Opcode.GETSTATIC, Opcode.NEW, Opcode.DUP, Opcode.LDC_W,
                        Opcode.INVOKESPECIAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL,
                        Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2,
                        Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList(
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"logger","Ljava/util/logging/Logger;",null,"com/wurmonline/server/behaviours/MethodsItems",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"WARNING","Ljava/util/logging/Level;",null,"java/util/logging/Level",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Class,null,null,"java/lang/StringBuilder",null,null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,"Why is tile for bed at ",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"<init>","(Ljava/lang/String;)V",null,"java/lang/StringBuilder",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTileX","()I",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append", "(I)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,",",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append","(Ljava/lang/String;)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTileY","()I",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append", "(I)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,",",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append","(Ljava/lang/String;)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"isOnSurface","()Z",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append","(Z)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null," null?",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append","(Ljava/lang/String;)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"toString", "()Ljava/lang/String;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"log","(Ljava/util/logging/Level;Ljava/lang/String;)V",null,"java/util/logging/Logger",null)
                )));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
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
                jbt = new jaseBT();
                // Removing indexes 48 - 71 in sleep of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                        Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTopParent","()J",null,"com/wurmonline/server/items/Item",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getWurmId","()J",null,"com/wurmonline/server/items/Item",null),
                        "","000f","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getCommunicator","()Lcom/wurmonline/server/creatures/Communicator;",null,"com/wurmonline/server/creatures/Creature",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,"The bed needs to be on the ground.",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"sendNormalServerMessage","(Ljava/lang/String;)V",null,"com/wurmonline/server/creatures/Communicator",null),
                        "","")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
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


                jbt = new jaseBT();
                // Removing indexes 170 - 223 in sleep of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                        Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                        Opcode.IRETURN, Opcode.ALOAD,Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                        Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("06",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getStructure","()Lcom/wurmonline/server/structures/Structure;",null,"com/wurmonline/server/zones/VolaTile",null),
                        "000e","06",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getStructure","()Lcom/wurmonline/server/structures/Structure;",null,"com/wurmonline/server/zones/VolaTile",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"isTypeHouse", "()Z",null,"com/wurmonline/server/structures/Structure",null),
                        "000f","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getCommunicator","()Lcom/wurmonline/server/creatures/Communicator;",null,"com/wurmonline/server/creatures/Creature",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,"You would get no sleep outside tonight.",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"sendNormalServerMessage","(Ljava/lang/String;)V",null,"com/wurmonline/server/creatures/Communicator",null),
                        "","","06",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getStructure","()Lcom/wurmonline/server/structures/Structure;",null,"com/wurmonline/server/zones/VolaTile",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"isFinished","()Z",null,"com/wurmonline/server/structures/Structure",null),
                        "000f","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getCommunicator","()Lcom/wurmonline/server/creatures/Communicator;",null,"com/wurmonline/server/creatures/Creature",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,"The house is too windy to provide protection.",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"sendNormalServerMessage","(Ljava/lang/String;)V",null,"com/wurmonline/server/creatures/Communicator",null),
                        "","")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                        "0000,000000,000000,0000,000000,000000,000000,00,000000,000000,000000,00,00,0000,000000,000000,000000,00,000000,000000,000000,00,00",
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


                jbt = new jaseBT();
                // Removing indexes 294 - 349 in sleep of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GETSTATIC, Opcode.GETSTATIC, Opcode.NEW, Opcode.DUP, Opcode.LDC_W,
                        Opcode.INVOKESPECIAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL,
                        Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2,
                        Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList(
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"logger","Ljava/util/logging/Logger;",null,"com/wurmonline/server/behaviours/MethodsItems",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"WARNING","Ljava/util/logging/Level;",null,"java/util/logging/Level",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Class,null,null,"java/lang/StringBuilder",null,null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,"Why is tile for bed at ",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"<init>","(Ljava/lang/String;)V",null,"java/lang/StringBuilder",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTileX","()I",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append", "(I)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,",",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append","(Ljava/lang/String;)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTileY","()I",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append", "(I)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,",",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append","(Ljava/lang/String;)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"isOnSurface","()Z",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append","(Z)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null," null?",null,null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"append","(Ljava/lang/String;)Ljava/lang/StringBuilder;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"toString", "()Ljava/lang/String;",null,"java/lang/StringBuilder",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"log","(Ljava/util/logging/Level;Ljava/lang/String;)V",null,"java/util/logging/Logger",null)
                )));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
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
                getSleepAttribute().computeMaxStack();
                getSleepMInfo().rebuildStackMapIf6(pool,cfMethodsItems);


                setMayUseBed(cfItem, "(Lcom/wurmonline/server/creatures/Creature;)Z", "mayUseBed");
                jbt = new jaseBT();
                // Removing indexes 10 - 55 in mayUseBed of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC,
                        Opcode.ASTORE_2, Opcode.ALOAD_2, Opcode.IFNULL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ACONST_NULL, Opcode.ASTORE_3,
                        Opcode.ALOAD_3, Opcode.IFNULL, Opcode.ALOAD_3, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD_3, Opcode.INVOKEVIRTUAL, Opcode.IFEQ,
                        Opcode.ICONST_1, Opcode.IRETURN, Opcode.ICONST_0)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                        jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"getTilePos","()Lcom/wurmonline/math/TilePos;",null,"com/wurmonline/server/items/Item",null),
                        "",
                        jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"isOnSurface","()Z",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"getTileOrNull","(Lcom/wurmonline/math/TilePos;Z)Lcom/wurmonline/server/zones/VolaTile;",null,"com/wurmonline/server/zones/Zones",null),
                        "","","000a","",
                        jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"getStructure","()Lcom/wurmonline/server/structures/Structure;",null,"com/wurmonline/server/zones/VolaTile",null),
                        "0004","","","","0013","",
                        jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"isTypeHouse","()Z",null,"com/wurmonline/server/structures/Structure",null),
                        "000c","",
                        jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"isFinished","()Z",null,"com/wurmonline/server/structures/Structure",null),
                        "0005","","","")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
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

            }
            if (useMagicChestInCart) {
                setMoveToItem(cfItem, "(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
                jbt = new jaseBT();
                //Removing indexes 58 - 69 in moveToItem of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.SIPUSH, Opcode.IF_ICMPEQ)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("08",
                        jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref,"getTemplateId","()I",null,"com/wurmonline/server/items/Item",null),
                        "0298","000b")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
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
                jbt = new jaseBT();
                //Removing indexes 110 - 162 in startFire of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ,
                        Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LCMP, Opcode.IFEQ, Opcode.LDC_W,
                        Opcode.ICONST_1, Opcode.ANEWARRAY, Opcode.DUP, Opcode.ICONST_0, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.AASTORE,
                        Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.ICONST_3, Opcode.INVOKEVIRTUAL,
                        Opcode.ICONST_1, Opcode.IRETURN)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTemplate","()Lcom/wurmonline/server/items/ItemTemplate;",null,"com/wurmonline/server/items/Item",null),
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"isTransportable","()Z",null,"com/wurmonline/server/items/ItemTemplate",null),
                        "002e","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getTopParent","()J",null,"com/wurmonline/server/items/Item",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getWurmId","()J",null,"com/wurmonline/server/items/Item",null),
                        "","0022",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,null,null,"The %s must be on the ground before you can light it.",null,null),
                        "","0003","","","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getName","()Ljava/lang/String;",null,"com/wurmonline/server/items/Item",null),
                        "",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"format","(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",null,"java/lang/String",null),
                        "09","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"getCommunicator","()Lcom/wurmonline/server/creatures/Communicator;",null,"com/wurmonline/server/creatures/Creature",null),
                        "09","",
                        jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"sendNormalServerMessage","(Ljava/lang/String;B)V",null,"com/wurmonline/server/creatures/Communicator",null),
                        "","")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                        "00,000000,000000,000000,00,000000,00,000000,00,000000,000000,00,000000,00,00,00,000000,00,000000,0000,00,000000,0000,00,000000,00,00",
                        getStartFireIterator(), "startFire");
                //<editor-fold desc="Replace code">
                /*
                Removed-
                    if (target.getTemplate().isTransportable() && target.getTopParent() != target.getWurmId()) {
                        final String message = String.format("The %s must be on the ground before you can light it.", target.getName());
                        performer.getCommunicator().sendNormalServerMessage(message, (byte)3);
                        return true;
                    }
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);
                getStartFireAttribute().computeMaxStack();
                getStartFireMInfo().rebuildStackMapIf6(pool, cfMethodsItems);


                setSetFire(cfMethodsItems, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z", "setFire");
                CtMethod ctmSetFire1 = ctcSelf.getMethod("setFire", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z");
                CtMethod ctmSetFire = ctcMethodsItems.getMethod("setFire", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z");
                ctmSetFire.setBody(ctmSetFire1, null);


                CtMethod ctmTargetIsOnFireCheck = ctcCargoTransportationMethods.getDeclaredMethod("targetIsOnFireCheck");
                ctmTargetIsOnFireCheck.setBody("{return false;}");


                CtMethod ctmLoadCargo = ctcCargoTransportationMethods.getMethod("loadCargo",
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
                String s = "if ($2.isOnFire()) {";
                s = s.concat("com.wurmonline.server.zones.Zone zone1 = ");
                s = s.concat("com.wurmonline.server.zones.Zones.getZone($1.getTileX(), $1.getTileY(), $1.isOnSurface());");
                s = s.concat("com.wurmonline.server.effects.Effect[] targetEffects = $2.getEffects();");
                s = s.concat("for (int i = 0; i < targetEffects.length; i++) {");
                s = s.concat("if (targetEffects[i].getType() == (short)0) {");
                s = s.concat("zone1.removeEffect(targetEffects[i]);}}}");
                ctmLoadCargo.insertAt(299,"{" + s + "}");


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
                ctmUnloadCargo.insertAt(1307, "{ " + s + " }");
            }
            if (craftWithinCart) {
                setAddCreationWindowOption(cfItemBehaviour,
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Ljava/util/List;)V", "addCreationWindowOption");
                jbt = new jaseBT();
                //Removing indexes 0 - 4 in addCreationWindowOption of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.IFEQ)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"isUseOnGroundOnly","()Z",null,"com/wurmonline/server/items/Item",null),
                        "0043")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                        "00,000000,000000", getAddCreationWindowOptionIterator(), "addCreationWindowOption");
                //<editor-fold desc="Replace code">
                /*
                Removed-
                    target.isUseOnGroundOnly() &&
                */
                //</editor-fold>
                logger.log(Level.INFO, replaceByteResult);


                jbt = new jaseBT();
                //Removing indexes 0 - 4 in addCreationWindowOption of javap.exe dump.
                jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                        Opcode.LCMP, Opcode.IFNE)));
                jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"getTopParent","()J",null,"com/wurmonline/server/items/Item",null),
                        "",
                        jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"getWurmId","()J",null,"com/wurmonline/server/items/Item",null),
                        "","0029")));
                jbt.setOpcodeOperand();
                replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
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

