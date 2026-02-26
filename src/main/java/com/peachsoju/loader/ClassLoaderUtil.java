package com.peachsoju.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassLoaderUtil {
    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        byte[] byteArray = new byte[1024];
        int bytesCount;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesCount = inputStream.read(byteArray, 0, 1024)) != -1) {
            output.write(byteArray, 0, bytesCount);
        }
        byte[] bytes = output.toByteArray();
        output.close();
        inputStream.close();
        return bytes;
    }
}
