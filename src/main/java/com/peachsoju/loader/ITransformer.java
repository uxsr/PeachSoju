package com.peachsoju.loader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Base64;

public interface ITransformer {

    String getClassName();

    void transform(ClassNode classNode, String name) throws Throwable;

    default byte[] getBytes(Class<?> clazz) throws IOException {
        return ClassLoaderUtil.toByteArray(
                clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class")
        );
    }

    default ClassNode getClassNode(Class<?> clazz) throws IOException {
        ClassNode node = new ClassNode();
        new ClassReader(ClassLoaderUtil.toByteArray(
                clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class")
        )).accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    default ClassNode getClassNode(String byteArray) {
        ClassNode node = new ClassNode();
        new ClassReader(Base64.getDecoder().decode(byteArray)).accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }
}
