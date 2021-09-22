package io.hqwu.commons.dubbo.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.common.serialize.ObjectOutput;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-21
 * Time: 09:13
 */
public class JacksonObjectOutput implements ObjectOutput {

    private final PrintWriter writer;
    private final ObjectMapper objectMapper;

    public JacksonObjectOutput(OutputStream out, ObjectMapper objectMapper) {
        this.writer = new PrintWriter(new OutputStreamWriter(out));
        this.objectMapper = objectMapper;
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        writer.println(objectMapper.writeValueAsString(obj));
        writer.flush();
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {
        writer.println(new String(v, StandardCharsets.UTF_8));
    }

    @Override
    public void writeBytes(byte[] v, int offset, int len) throws IOException {
        writer.println(new String(v, offset, len, StandardCharsets.UTF_8));
    }

    @Override
    public void flushBuffer() throws IOException {
        writer.flush();
    }
}
