package com.peachsoju.loader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.ILegacyClassTransformer;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


public class ClassTransformer implements ILegacyClassTransformer {
    private static final Map<String, ITransformer> transformerMap = new HashMap<>();

    public ClassTransformer() {
        registerTransformer(new MinecraftTransformer());
    }

    private static void registerTransformer(ITransformer transformer) {
        transformerMap.put(transformer.getClassName(), transformer);
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        ITransformer transformer = transformerMap.get(transformedName);
        if (transformer == null) return basicClass;

        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, ClassReader.EXPAND_FRAMES);

        try {
            transformer.transform(node, transformedName);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException("Failed to inject bytecode. Please report this.");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] getBytes(String bytearr) {
        return Base64.getDecoder().decode(bytearr);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean isDelegationExcluded() {
        return false;
    }
}
