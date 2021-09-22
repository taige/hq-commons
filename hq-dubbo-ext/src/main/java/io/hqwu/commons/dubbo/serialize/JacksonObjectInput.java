package io.hqwu.commons.dubbo.serialize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.common.serialize.ObjectInput;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-21
 * Time: 09:44
 */
public class JacksonObjectInput implements ObjectInput {

    private final BufferedReader reader;
    private final ObjectMapper objectMapper;

    public JacksonObjectInput(InputStream inputStream, ObjectMapper objectMapper) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.objectMapper = objectMapper;
    }

    private String readLine() throws IOException, EOFException {
        String line = reader.readLine();
        if (line == null || line.trim().length() == 0) {
            throw new EOFException();
        }
        return line;
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return objectMapper.readValue(readLine(), Object.class);
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return objectMapper.readValue(readLine(), cls);
    }

    @Override
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        return objectMapper.readValue(readLine(), new TypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        });
    }

    @Override
    public boolean readBool() throws IOException {
        return objectMapper.readValue(readLine(), Boolean.class);
    }

    @Override
    public byte readByte() throws IOException {
        return objectMapper.readValue(readLine(), Byte.class);
    }

    @Override
    public short readShort() throws IOException {
        return objectMapper.readValue(readLine(), Short.class);
    }

    @Override
    public int readInt() throws IOException {
        return objectMapper.readValue(readLine(), Integer.class);
    }

    @Override
    public long readLong() throws IOException {
        return objectMapper.readValue(readLine(), Long.class);
    }

    @Override
    public float readFloat() throws IOException {
        return objectMapper.readValue(readLine(), Float.class);
    }

    @Override
    public double readDouble() throws IOException {
        return objectMapper.readValue(readLine(), Double.class);
    }

    @Override
    public String readUTF() throws IOException {
        return objectMapper.readValue(readLine(), String.class);
    }

    @Override
    public byte[] readBytes() throws IOException {
        return readLine().getBytes(StandardCharsets.UTF_8);
    }
}
