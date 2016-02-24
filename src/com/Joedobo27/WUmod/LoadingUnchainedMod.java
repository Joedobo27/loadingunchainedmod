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
    private boolean loadAnyBin = true;
    private boolean moveItemsIntoBinWithinContainer = false;
    private boolean useCustomProximity = false;
    private int loadProximityRequired = 4;
    private boolean useCustomLoadTime = false;
    private int loadDurationTime = 100;
    private boolean LoadIntoDragged = true;
    private boolean boatInCart = false;
    private boolean useBedInCart = false;
    private boolean useMagicChestInCart = false;
    private boolean useForgeInCart = false;
    private boolean craftWithinCart = false;
    private boolean loadAltar = false;
    private boolean useAltarInCart = false;
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

    private CodeAttribute prayAttribute;
    private CodeIterator prayIterator;
    private MethodInfo prayMInfo;

    private CodeAttribute sacrificeAttribute;
    private CodeIterator sacrificeIterator;
    private MethodInfo sacrificeMInfo;

    private CodeAttribute actionDBAttribute;
    private CodeIterator actionDBIterator;
    private MethodInfo actionDBMInfo;

    //</editor-fold>

    private static Logger logger = Logger.getLogger(LoadingUnchainedMod.class.getName());

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
        LoadIntoDragged = Boolean.valueOf(properties.getProperty("LoadIntoDragged", Boolean.toString(LoadIntoDragged)));
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
        if (useBedInCart || boatInCart || loadAltar || loadOther || craftWithinCart) {
            Map<Integer, ItemTemplate> fieldTemplates = null;
            try {
                fieldTemplates = ReflectionUtil.getPrivateField(ItemTemplateFactory.class,
                        ReflectionUtil.getField(ItemTemplateFactory.class, "templates"));
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
            int bedCnt = 0;
            int boatInCartCnt = 0;
            int altarCnt = 0;
            int otherCnt = 0;
            int craftWithinCartCnt = 0;
            for (ItemTemplate template : fieldTemplates.values()) {
                Integer templateId = template.getTemplateId();
                if (useBedInCart) {
                    Field fieldUseOnGroundOnly = null;
                    try {
                        fieldUseOnGroundOnly = ReflectionUtil.getField(ItemTemplate.class, "useOnGroundOnly");
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    if (templateId == ItemList.bedStandard || templateId == ItemList.canopyBed) {
                        if (template.isUseOnGroundOnly()) {
                            try {
                                ReflectionUtil.setPrivateField(template, fieldUseOnGroundOnly, Boolean.FALSE);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            bedCnt++;
                        }
                    }
                }
                if (boatInCart) {
                    Field fieldIsTransportable = null;
                    try {
                        fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    if (template.isFloating()){
                        try {
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        boatInCartCnt++;
                    }
                }
                if (loadAltar){
                    Field fieldIsTransportable = null;
                    try {
                        fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    if (templateId == ItemList.altarWood || templateId == ItemList.altarGold || templateId == ItemList.altarSilver ||
                            templateId == ItemList.altarStone){
                        try {
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        altarCnt++;
                    }

                }
                if (loadOther){
                    Field fieldIsTransportable = null;
                    try {
                        fieldIsTransportable = ReflectionUtil.getField(ItemTemplate.class, "isTransportable");
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    if (templateId == ItemList.trashBin){
                        try {
                            ReflectionUtil.setPrivateField(template, fieldIsTransportable, Boolean.TRUE);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        otherCnt++;
                    }
                }
                if (craftWithinCart){
                    Field fieldUseOnGroundOnly = null;
                    try {
                        fieldUseOnGroundOnly = ReflectionUtil.getField(ItemTemplate.class, "useOnGroundOnly");
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    if (templateId == ItemList.loom || templateId == ItemList.spinningWheel) {
                        if (template.isUseOnGroundOnly()) {
                            try {
                                ReflectionUtil.setPrivateField(template, fieldUseOnGroundOnly, Boolean.FALSE);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
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
        }
    }

    @Override
    public void init() {
        String replaceByteResult;
        String printByteResult;
        jaseBT jbt;
        jaseBT jbt1;

        ClassPool pool = HookManager.getInstance().getClassPool();

        CtClass ctcSelf = pool.makeClass("Default");
        try {
            ctcSelf = pool.get(this.getClass().getName());
        } catch (NotFoundException e) {
            e.printStackTrace();
        }

        CtClass ctcCargoTransportationMethods = pool.makeClass("Default");
        try {
            ctcCargoTransportationMethods = pool.get("com.wurmonline.server.behaviours.CargoTransportationMethods");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        ClassFile cfCargoTransportationMethods = ctcCargoTransportationMethods.getClassFile();
        ConstPool cpCargoTransportationMethods = cfCargoTransportationMethods.getConstPool();

        CtClass ctcItem = pool.makeClass("Default");
        try {
            ctcItem = pool.get("com.wurmonline.server.items.Item");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        ClassFile cfItem = ctcItem.getClassFile();
        ConstPool cpItem = cfItem.getConstPool();

        CtClass ctcMethodsItems = pool.makeClass("Default");
        try {
            ctcMethodsItems = pool.get("com.wurmonline.server.behaviours.MethodsItems");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        ClassFile cfMethodsItems = ctcMethodsItems.getClassFile();
        ConstPool cpMethodsItems = cfMethodsItems.getConstPool();

        CtClass ctcItemBehaviour = pool.makeClass("Default");
        try {
            ctcItemBehaviour = pool.get("com.wurmonline.server.behaviours.ItemBehaviour");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        ClassFile cfItemBehaviour = ctcItemBehaviour.getClassFile();
        ConstPool cpItemBehaviour = cfItemBehaviour.getConstPool();

        CtClass ctcMethodsReligion = pool.makeClass("Default");
        try {
            ctcMethodsReligion = pool.get("com.wurmonline.server.behaviours.MethodsReligion");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        ClassFile cfMethodsReligion = ctcMethodsReligion.getClassFile();
        ConstPool cpMethodsReligion = cfMethodsReligion.getConstPool();

        CtClass ctcDomainItemBehaviour = pool.makeClass("Default");
        try {
            ctcDomainItemBehaviour = pool.get("com.wurmonline.server.behaviours.DomainItemBehaviour");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        ClassFile cfDomainItemBehaviour = ctcDomainItemBehaviour.getClassFile();
        ConstPool cpDomainItemBehaviour = cfDomainItemBehaviour.getConstPool();

        CtClass ctcActions = pool.makeClass("Default");
        try {
            ctcActions = pool.get("com.wurmonline.server.behaviours.Actions");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }


        if (loadAnyBin){
            CtMethod ctmTargetIsNotEmptyContainerCheck = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcCargoTransportationMethods);
            try {
                ctmTargetIsNotEmptyContainerCheck = ctcCargoTransportationMethods.getMethod("targetIsNotEmptyContainerCheck",
                        "(Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            try {
                ctmTargetIsNotEmptyContainerCheck.setBody("{return false;}");
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
        }

        if (moveItemsIntoBinWithinContainer){
            jbt = new jaseBT();
            // Lines 3975 to 4031 in moveToItem of Javap.exe dump.
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList( Opcode.ALOAD,Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                    Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.IFNE, Opcode.ALOAD_1, Opcode.IFNULL, Opcode.LDC_W,
                    Opcode.ICONST_1, Opcode.ANEWARRAY, Opcode.DUP, Opcode.ICONST_0, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.AASTORE,
                    Opcode.INVOKESTATIC, Opcode.ASTORE, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                    Opcode.ICONST_0, Opcode.IRETURN)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("08",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getTopParent:()J"),
                    "08",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getWurmId:()J"),
                    "","002e","08",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "isCrate:()Z"),
                    "0026","","0020",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_String,"The %s needs to be on the ground."),
                    "",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Class,"java/lang/Object"),
                    "","","08",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "getName:()Ljava/lang/String;"),
                    "",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "com/wurmonline/server/utils/StringUtil.format:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
                    "13","",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    "13",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref, "com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
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
                        final String message = StringUtil.format("The %s needs to be on the ground.", target.getName());
                        mover.getCommunicator().sendNormalServerMessage(message);
                    }
                    return false;
                }
            */
            //</editor-fold>
            logger.log(Level.INFO, replaceByteResult);
        }
        if (useCustomLoadTime) {
            CtMethod cmGetLoadUnloadActionTime = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcActions);
            try {
                cmGetLoadUnloadActionTime = ctcActions.getMethod("getLoadUnloadActionTime", "(Lcom/wurmonline/server/creatures/Creature;)I");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            try {
                cmGetLoadUnloadActionTime.setBody("{ return " + loadDurationTime + "; }");
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
            logger.log(Level.INFO, "Custom load time set.");
            /*
            String s = "10" + String.format("%02X", loadDurationTime & 0xff);
            setLoadCargo(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z", "loadCargo");
            replaceByteResult = jaseBT.byteCodeFindReplace("AC,1064,3608", "1064", s, getLoadCargoIterator(), "loadCargo");
            logger.log(Level.INFO, replaceByteResult);
            setUnloadCargo(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z", "unloadCargo");
            replaceByteResult = jaseBT.byteCodeFindReplace("AC,1064,3606", "1064", s, getUnloadCargoIterator(), "unloadCargo");
            logger.log(Level.INFO, replaceByteResult);
            */
        }
        if (useCustomProximity){
            CtMethod ctmGetLoadActionDistance = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcCargoTransportationMethods);
            try {
                ctmGetLoadActionDistance = ctcCargoTransportationMethods.getMethod("getLoadActionDistance",
                        "(Lcom/wurmonline/server/items/Item;)I");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }

            try {
                ctmGetLoadActionDistance.setBody("{return " + loadProximityRequired + ";}");
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
        }
        if (LoadIntoDragged) {
            setPerformerIsNotOnATransportVehicle(cfCargoTransportationMethods, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Vehicle;)Z",
                    "performerIsNotOnATransportVehicle");
            replaceByteResult = jaseBT.byteCodeFindReplace("2B,C6000A,2B", "2B,C6000A", "00,000000", getPerformerIsNotOnATransportVehicleIterator(), "performerIsNotOnATransportVehicle");
            //Removed "vehicle == null ||".
            logger.log(Level.INFO, replaceByteResult);
            CtMethod ctmPerformerIsNotOnAVehicle = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcCargoTransportationMethods);
            try {
                ctmPerformerIsNotOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotOnAVehicle",
                        "(Lcom/wurmonline/server/creatures/Creature;)Z");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            try {
                ctmPerformerIsNotOnAVehicle.setBody("{return false;}");
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }

            CtMethod ctmPerformerIsNotSeatedOnAVehicle = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcCargoTransportationMethods);
            try {
                ctmPerformerIsNotSeatedOnAVehicle = ctcCargoTransportationMethods.getMethod("performerIsNotSeatedOnAVehicle",
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Seat;)Z");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            try {
                ctmPerformerIsNotSeatedOnAVehicle.setBody("{return false;}");
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
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
                    jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.isBoat:()Z"),
                    "0010","05",
                    jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.getTemplateId:()I"),
                    "0355", "0005", "", "", "",
                    jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.isUnfinished:()Z"),
                    "0025", "05",
                    jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.getTemplateId:()I"),
                    "0355", "001a","",
                    jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/Item.getRealTemplate:()Lcom/wurmonline/server/items/ItemTemplate;"),
                    "06", "06","0005","","","06",
                    jaseBT.findConstantPoolReference(cpCargoTransportationMethods, ConstPool.CONST_Methodref, "com/wurmonline/server/items/ItemTemplate.isBoat:()Z"),
                    "0005", "", "")));
            jbt.setOpcodeOperand();
            replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(),jbt.getOpcodeOperand(),
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
            //getGetLoadUnloadActionsAttribute().computeMaxStack();
            try {
                getGetLoadUnloadActionsMInfo().rebuildStackMapIf6(pool, cfCargoTransportationMethods);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }
        }
        if (useBedInCart) {
            setAddBedOptions(cfItemBehaviour,
                    "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Ljava/util/List;)V", "addBedOptions");
            try {
                getAddBedOptionsIterator().insertGap(41, 12);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }
            try {
                getAddBedOptionsIterator().insertExGap(373, 18);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }


            jbt = new jaseBT();
            // Working with lines 17-35 in addBedOptions of the javap.exe dump.
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                    Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.NOP, Opcode.NOP, Opcode.NOP,
                    Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP, Opcode.NOP)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("04", "0174","04",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    "016c","04",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    jaseBT.findConstantPoolReference(cpItemBehaviour, ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                    "0161", "", "", "", "", "", "", "", "", "", "", "", "")));
            jbt.setOpcodeOperand();

            jbt1 = new jaseBT();
            jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL,
                    Opcode.IFNULL, Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                    Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LCMP, Opcode.IFNE)));
            jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("04", "0165","04",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    "015d","04",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    jaseBT.findConstantPoolReference(cpItemBehaviour, ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                    "0152","",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTopParent:()J"),
                    "",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getWurmId:()J"),
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
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/Throwable;)V"),
                    "","","","","","","","","","","","","","","","","","","")));
            jbt.setOpcodeOperand();

            jbt1 = new jaseBT();
            jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GOTO, Opcode.ALOAD_3, Opcode.GETSTATIC, Opcode.SIPUSH,
                    Opcode.AALOAD, Opcode.INVOKEINTERFACE, Opcode.NOP, Opcode.POP)));
            jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("0012","",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Fieldref,"com/wurmonline/server/behaviours/Actions.actionEntrys:[Lcom/wurmonline/server/behaviours/ActionEntry;"),
                    "0145","",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref, "java/util/List.add:(Ljava/lang/Object;)Z") + "0200",
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
            //getAddBedOptionsAttribute().computeMaxStack();
            try {
                getAddBedOptionsMInfo().rebuildStackMapIf6(pool, cfItemBehaviour);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }


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
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileX:()I"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileY:()I"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.isOnSurface:()Z"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/Zones.getTileOrNull:(IIZ)Lcom/wurmonline/server/zones/VolaTile;"),
                    "05","05","00b7","05",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    "000e","05",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                    "000f","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"You would get no sleep outside tonight."),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                    "","","05",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isFinished:()Z"),
                    "000f","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"The house is too windy to provide protection."),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
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
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"logger:Ljava/util/logging/Logger;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"java/util/logging/Level.WARNING:Ljava/util/logging/Level;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Class,"java/lang/StringBuilder"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"Why is tile for bed at "),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.<init>:(Ljava/lang/String;)V"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileX:()I"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,","),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileY:()I"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,","),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.isOnSurface:()Z"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Z)Ljava/lang/StringBuilder;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String," null?"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.toString:()Ljava/lang/String;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;)V")
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
            //getAskSleepAttribute().computeMaxStack();
            try {
                getAskSleepMInfo().rebuildStackMapIf6(pool, cfMethodsItems);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }


            setSleep(cfMethodsItems, "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                    "sleep");
            jbt = new jaseBT();
            // Removing indexes 48 - 71 in sleep of javap.exe dump.
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL,
                    Opcode.LCMP, Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1, Opcode.IRETURN)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTopParent:()J"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getWurmId:()J"),
                    "","000f","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"The bed needs to be on the ground."),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
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
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    "000e","06",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                    "000f","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"You would get no sleep outside tonight."),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                    "","","06",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isFinished:()Z"),
                    "000f","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"The house is too windy to provide protection."),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
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
            // Removing indexes 291 - 349 in sleep of javap.exe dump.
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.GETSTATIC, Opcode.GETSTATIC, Opcode.NEW, Opcode.DUP, Opcode.LDC_W,
                    Opcode.INVOKESPECIAL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL,
                    Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_2,
                    Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL, Opcode.INVOKEVIRTUAL)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList(
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"logger:Ljava/util/logging/Logger;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Fieldref,"java/util/logging/Level.WARNING:Ljava/util/logging/Level;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Class,"java/lang/StringBuilder"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"Why is tile for bed at "),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.<init>:(Ljava/lang/String;)V"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileX:()I"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,","),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTileY:()I"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,","),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.isOnSurface:()Z"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Z)Ljava/lang/StringBuilder;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String," null?"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/lang/StringBuilder.toString:()Ljava/lang/String;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"java/util/logging/Logger.log:(Ljava/util/logging/Level;Ljava/lang/String;)V")
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
            //getSleepAttribute().computeMaxStack();
            try {
                getSleepMInfo().rebuildStackMapIf6(pool,cfMethodsItems);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }


            setMayUseBed(cfItem, "(Lcom/wurmonline/server/creatures/Creature;)Z", "mayUseBed");
            jbt = new jaseBT();
            // Removing indexes 10 - 55 in mayUseBed of javap.exe dump.
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_0, Opcode.INVOKEVIRTUAL, Opcode.INVOKESTATIC,
                    Opcode.ASTORE_2, Opcode.ALOAD_2, Opcode.IFNULL, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ACONST_NULL, Opcode.ASTORE_3,
                    Opcode.ALOAD_3, Opcode.IFNULL, Opcode.ALOAD_3, Opcode.INVOKEVIRTUAL, Opcode.IFEQ, Opcode.ALOAD_3, Opcode.INVOKEVIRTUAL, Opcode.IFEQ,
                    Opcode.ICONST_1, Opcode.IRETURN, Opcode.ICONST_0)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                    jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"getTilePos:()Lcom/wurmonline/math/TilePos;"),
                    "",
                    jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"isOnSurface:()Z"),
                    jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/Zones.getTileOrNull:(Lcom/wurmonline/math/TilePos;Z)Lcom/wurmonline/server/zones/VolaTile;"),
                    "","","000a","",
                    jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"com/wurmonline/server/zones/VolaTile.getStructure:()Lcom/wurmonline/server/structures/Structure;"),
                    "0004","","","","0013","",
                    jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isTypeHouse:()Z"),
                    "000c","",
                    jaseBT.findConstantPoolReference(cpItem,ConstPool.CONST_Methodref,"com/wurmonline/server/structures/Structure.isFinished:()Z"),
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

            //getMayUseBedAttribute().computeMaxStack();
            try {
                getMayUseBedMInfo().rebuildStackMapIf6(pool, cfItem);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }

        }
        if (useMagicChestInCart) {
            setMoveToItem(cfItem, "(Lcom/wurmonline/server/creatures/Creature;JZ)Z", "moveToItem");
            jbt = new jaseBT();
            //Removing indexes 58 - 69 in moveToItem of javap.exe dump.
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.SIPUSH, Opcode.IF_ICMPEQ)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("08",
                    jaseBT.findConstantPoolReference(cpItem, ConstPool.CONST_Methodref,"getTemplateId:()I"),
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
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTemplate:()Lcom/wurmonline/server/items/ItemTemplate;"),
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/ItemTemplate.isTransportable:()Z"),
                    "002e","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTopParent:()J"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getWurmId:()J"),
                    "","0022",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_String,"The %s must be on the ground before you can light it."),
                    "","0003","","","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getName:()Ljava/lang/String;"),
                    "",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/utils/StringUtil.format:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
                    "09","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    "09","",
                    jaseBT.findConstantPoolReference(cpMethodsItems,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;B)V"),
                    "","")));
            jbt.setOpcodeOperand();
            replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                    "00,000000,000000,000000,00,000000,00,000000,00,000000,000000,00,000000,00,00,00,000000,00,000000,0000,00,000000,0000,00,000000,00,00",
                    getStartFireIterator(), "startFire");
            //<editor-fold desc="Replace code">
            /*
            Removed-
                if (target.getTemplate().isTransportable() && target.getTopParent() != target.getWurmId()) {
                    final String message = StringUtil.format("The %s must be on the ground before you can light it.", target.getName());
                    performer.getCommunicator().sendNormalServerMessage(message, (byte)3);
                    return true;
                }
            */
            //</editor-fold>
            logger.log(Level.INFO, replaceByteResult);
            //getStartFireAttribute().computeMaxStack();
            try {
                getStartFireMInfo().rebuildStackMapIf6(pool, cfMethodsItems);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }


            setSetFire(cfMethodsItems, "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z", "setFire");
            CtMethod ctmSetFire1 = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcMethodsItems);
            try {
                ctmSetFire1 = ctcSelf.getMethod("setFire", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            CtMethod ctmSetFire = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcMethodsItems);
            try {
                ctmSetFire = ctcMethodsItems.getMethod("setFire", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            try {
                ctmSetFire.setBody(ctmSetFire1, null);
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }


            CtMethod ctmTargetIsOnFireCheck = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcCargoTransportationMethods);
            try {
                ctmTargetIsOnFireCheck = ctcCargoTransportationMethods.getDeclaredMethod("targetIsOnFireCheck");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            try {
                ctmTargetIsOnFireCheck.setBody("{return false;}");
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }


            CtMethod ctmLoadCargo = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcCargoTransportationMethods);
            try {
                ctmLoadCargo = ctcCargoTransportationMethods.getMethod("loadCargo",
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            String s = "if ($2.isOnFire()) {";
            s = s.concat("com.wurmonline.server.zones.Zone zone1 = ");
            s = s.concat("com.wurmonline.server.zones.Zones.getZone($1.getTileX(), $1.getTileY(), $1.isOnSurface());");
            s = s.concat("com.wurmonline.server.effects.Effect[] targetEffects = $2.getEffects();");
            s = s.concat("for (int i = 0; i < targetEffects.length; i++) {");
            s = s.concat("if (targetEffects[i].getType() == (short)0) {");
            s = s.concat("zone1.removeEffect(targetEffects[i]);}}}");
            try {
                ctmLoadCargo.insertAt(299,"{" + s + "}");
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }


            CtMethod ctmUnloadCargo = new CtMethod(CtClass.voidType,"default",new CtClass[]{},ctcCargoTransportationMethods);
            try {
                ctmUnloadCargo = ctcCargoTransportationMethods.getMethod("unloadCargo",
                        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z");
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
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
            try {
                ctmUnloadCargo.insertAt(1307, "{ " + s + " }");
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
        }
        if (craftWithinCart) {
            setAddCreationWindowOption(cfItemBehaviour,
                    "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Ljava/util/List;)V", "addCreationWindowOption");
            jbt = new jaseBT();
            //Removing indexes 0 - 4 in addCreationWindowOption of javap.exe dump.
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.IFEQ)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.isUseOnGroundOnly:()Z"),
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
            //Removing indexes 42 - 51 in addCreationWindowOption of javap.exe dump.
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL,
                    Opcode.LCMP, Opcode.IFNE)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getTopParent:()J"),
                    "",
                    jaseBT.findConstantPoolReference(cpItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getWurmId:()J"),
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
            //getAddCreationWindowOptionAttribute().computeMaxStack();
            try {
                getAddCreationWindowOptionMInfo().rebuildStackMapIf6(pool, cfItemBehaviour);
            } catch (BadBytecode badBytecode) {
                badBytecode.printStackTrace();
            }
        }
        if (useAltarInCart){

            setPray(cfMethodsReligion,
                    "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
                    "pray");
            //<editor-fold desc="Changes">
            /*
            // lines 136-158 in pray of MethodsReligion removed.
            if (altar.getParentId() != -10L) {
                performer.getCommunicator().sendNormalServerMessage("The altar needs to be on the ground to be used.");
                return true;
            }
            */
            //</editor-fold>
            jbt = new jaseBT();
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                    Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                    Opcode.IRETURN)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getParentId:()J"),
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Long,"-10"),
                    "","000f","",
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_String,"The altar needs to be on the ground to be used."),
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                    "","")));
            jbt.setOpcodeOperand();
            replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                    "00,000000,000000,00,000000,00,000000,000000,000000,00,00",getPrayIterator(),"pray");
            logger.log(Level.INFO, replaceByteResult);


            setSacrifice(cfMethodsReligion,
                    "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z",
                    "sacrifice");
            //<editor-fold desc="Changes">
            /*
            // lines 162-184 in sacrifice of MethodsReligion removed.
            if (altar.getParentId() != -10L) {
                performer.getCommunicator().sendNormalServerMessage("The altar needs to be on the ground to be used.");
                return true;
            }
            */
            //</editor-fold>
            jbt = new jaseBT();
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                    Opcode.IFEQ, Opcode.ALOAD_1, Opcode.INVOKEVIRTUAL, Opcode.LDC_W, Opcode.INVOKEVIRTUAL, Opcode.ICONST_1,
                    Opcode.IRETURN)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("",
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getParentId:()J"),
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Long,"-10"),
                    "","000f","",
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_String,"The altar needs to be on the ground to be used."),
                    jaseBT.findConstantPoolReference(cpMethodsReligion,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                    "","")));
            jbt.setOpcodeOperand();
            replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(),
                    "00,000000,000000,00,000000,00,000000,000000,000000,00,00",getSacrificeIterator(),"sacrifice");
            logger.log(Level.INFO, replaceByteResult);


            setActionDB(cfDomainItemBehaviour,
                    "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;SF)Z",
                    "action");

            //<editor-fold desc="Changes">
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
            jbt = new jaseBT();
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                    Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ALOAD_2,
                    Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_1, Opcode.FLOAD, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("04",
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getParentId:()J"),
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Long,"-10"),
                    "","000f","",
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_String,"The altar needs to be on the ground to be used.").substring(2),
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                    "0098","","04","","","06",
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/behaviours/MethodsReligion.holdSermon:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z"),
                    "07","0089")));
            jbt.setOpcodeOperand();
            jbt1 = new jaseBT();
            jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                    Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                    Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.ALOAD_2,Opcode.ALOAD, Opcode.ALOAD_3, Opcode.ALOAD_1, Opcode.FLOAD,
                    Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
            jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("","","","","","","","","","","","","","","","","","","","","","","","",
                    "","04","","","06",
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/behaviours/MethodsReligion.holdSermon:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z"),
                    "07","0089")));
            jbt1.setOpcodeOperand();
            replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(), jbt1.getOpcodeOperand(),
                    getActionDBIterator(), "action");
            logger.log(Level.INFO, replaceByteResult);


            //<editor-fold desc="Changes">
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
            jbt = new jaseBT();
            jbt.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.ALOAD, Opcode.INVOKEVIRTUAL, Opcode.LDC2_W, Opcode.LCMP,
                    Opcode.IFEQ, Opcode.ALOAD_2, Opcode.INVOKEVIRTUAL, Opcode.LDC, Opcode.INVOKEVIRTUAL, Opcode.GOTO, Opcode.ALOAD_2,
                    Opcode.ALOAD_3, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
            jbt.setOperandStructure(new ArrayList<>(Arrays.asList("04",
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/items/Item.getParentId:()J"),
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Long,"-10"),
                    "","000f","",
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Creature.getCommunicator:()Lcom/wurmonline/server/creatures/Communicator;"),
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_String,"The altar needs to be on the ground to be used.").substring(2),
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/creatures/Communicator.sendNormalServerMessage:(Ljava/lang/String;)V"),
                    "0062","","",
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/behaviours/MethodsReligion.sendRechargeQuestion:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z"),
                    "07","0058")));
            jbt.setOpcodeOperand();
            jbt1 = new jaseBT();
            jbt1.setOpCodeStructure(new ArrayList<>(Arrays.asList(Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                    Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,
                    Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.NOP,Opcode.ALOAD_2,
                    Opcode.ALOAD_3, Opcode.INVOKESTATIC, Opcode.ISTORE, Opcode.GOTO)));
            jbt1.setOperandStructure(new ArrayList<>(Arrays.asList("","","","","","","","","","","","","","","","","","","","","","","","",
                    "","",
                    jaseBT.findConstantPoolReference(cpDomainItemBehaviour,ConstPool.CONST_Methodref,"com/wurmonline/server/behaviours/MethodsReligion.sendRechargeQuestion:(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z"),
                    "07","0058")));
            jbt1.setOpcodeOperand();
            replaceByteResult = jaseBT.byteCodeFindReplace(jbt.getOpcodeOperand(), jbt.getOpcodeOperand(), jbt1.getOpcodeOperand(),
                    getActionDBIterator(), "action");
            logger.log(Level.INFO, replaceByteResult);
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

    /*
    Multiple CodeIterators pointing to the same CodeAttribute can cause problems.
    Inserting gaps will always break other iterators pointing to the same attribute.
    CodeAttribute and MethodInfo also cause problems when multiple objects exist representing the same Wurm method.
    Use one class wide object for each CodeAttribute, CodeIterator, MethodInfo for all Wurm methods.
    The following section are get and set methods to achieve this.
    */
    public void setActionDB(ClassFile cf, String desc, String name){
        if (this.actionDBMInfo == null || this.actionDBIterator == null || this.actionDBAttribute == null){
            for (List a : new List[]{cf.getMethods()}){
                for (Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.actionDBMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.actionDBMInfo == null){
                throw new NullPointerException();
            }
            this.actionDBAttribute = this.actionDBMInfo.getCodeAttribute();
            this.actionDBIterator = this.actionDBAttribute.iterator();
        }
    }

    public CodeIterator getActionDBIterator(){return this.actionDBIterator;}

    public CodeAttribute getActionDBAttribute(){return this.actionDBAttribute;}

    public MethodInfo getActionDBMInfo(){return this.actionDBMInfo;}

    public void setSacrifice(ClassFile cf, String desc, String name){
        if (this.sacrificeMInfo == null || this.sacrificeIterator == null || this.sacrificeAttribute == null){
            for (List a : new List[]{cf.getMethods()}){
                for (Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.sacrificeMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.sacrificeMInfo == null){
                throw new NullPointerException();
            }
            this.sacrificeAttribute = this.sacrificeMInfo.getCodeAttribute();
            this.sacrificeIterator = this.sacrificeAttribute.iterator();
        }
    }

    public CodeIterator getSacrificeIterator(){return this.sacrificeIterator;}

    public CodeAttribute getSacrificeAttribute(){return this.sacrificeAttribute;}

    public MethodInfo getSacrificeMInfo(){return this.sacrificeMInfo;}

    public void setPray(ClassFile cf, String desc, String name){
        if (this.prayMInfo == null || this.prayIterator == null || this.prayAttribute == null){
            for (List a : new List[]{cf.getMethods()}){
                for (Object b : a){
                    MethodInfo MInfo = (MethodInfo) b;
                    if (Objects.equals(MInfo.getDescriptor(), desc) && Objects.equals(MInfo.getName(), name)){
                        this.prayMInfo = MInfo;
                        break;
                    }
                }
            }
            if (this.prayMInfo == null){
                throw new NullPointerException();
            }
            this.prayAttribute = this.prayMInfo.getCodeAttribute();
            this.prayIterator = this.prayAttribute.iterator();
        }
    }

    public CodeIterator getPrayIterator(){return this.prayIterator;}

    public CodeAttribute getPrayAttribute(){return this.prayAttribute;}

    public MethodInfo getPrayMInfo(){return this.prayMInfo;}

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

