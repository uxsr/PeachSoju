package com.peachsoju.loader;

import com.peachsoju.PeachSoju;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Iterator;

public class MinecraftTransformer implements ITransformer {

    @Override
    public String getClassName() {
        return "net.minecraft.client.MinecraftClient";
    }

    @Override
    public void transform(ClassNode classNode, String name) throws Throwable {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("run")) { // run method - happens after full init
                // Inject at the start of the run method
                methodNode.instructions.insert(
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "com/peachsoju/loader/MinecraftTransformer",
                                "init",
                                "()V",
                                false
                        )
                );
            } else if (methodNode.name.equals("tick")) { // tick replaces runTick
                Iterator<AbstractInsnNode> iter = methodNode.instructions.iterator();
                while (iter.hasNext()) {
                    AbstractInsnNode insn = iter.next();
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode node = ((MethodInsnNode) insn);
                        // Add your tick hooks here if needed
                    }
                }
            }
        }
    }

    public static void init() {
        PeachSoju.INSTANCE.init();
    }
}