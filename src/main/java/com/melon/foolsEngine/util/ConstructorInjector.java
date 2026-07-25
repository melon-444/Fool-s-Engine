package com.melon.foolsEngine.util;

import java.util.Arrays;

/**
 * Injects {@code EventBus.addListener(this)} into every {@code <init>}
 * method, right after the super/this constructor call (bytecode offset 4).
 * <p>
 * Only transforms classes that contain an {@code @InstanceBusSubscriber} annotation
 * (detected via a UTF-8 constant-pool scan).
 */
public final class ConstructorInjector {

    private static final byte[] INJECTION = {
            (byte) 0x2A,       // aload_0
            (byte) 0xB8,       // invokestatic
            0, 0               // cp index (patched at injection time)
    };
    private static final int INJECT_OFFSET = 4;

    private ConstructorInjector() {}

    public static byte[] inject(byte[] bytes) {
        if (!hasAnnotation(bytes)) return bytes;
        if (!isClassFile(bytes)) return bytes;

        int cpCount = u2(bytes, 8);
        int bodyPos = 8 + 2 + cpSize(bytes, 8 + 2, cpCount);
        int[] cpOffs = new int[cpCount];
        buildCpOffsets(bytes, 8 + 2, cpCount, cpOffs);

        int newCpIdx = cpCount;
        byte[] newCp = buildExtraCp(newCpIdx);
        int growth = newCp.length + 256;

        byte[] out = new byte[bytes.length + growth];
        int delta = 0;
        int reader = 0;

        System.arraycopy(bytes, 0, out, 0, 8); reader = 8; delta += 0;
        writeU2(out, 8, cpCount + 6); out[8] = bytes[8];  reader = 10;
        System.arraycopy(bytes, 10, out, 10, bodyPos - 10); reader = bodyPos; delta += 0;
        System.arraycopy(newCp, 0, out, reader, newCp.length); delta += newCp.length;

        int wp = reader + delta;

        int access = u2(bytes, reader); reader += 2;
        int self = u2(bytes, reader); reader += 2;
        int superCls = u2(bytes, reader); reader += 2;
        writeU2(out, wp, access); wp += 2;
        writeU2(out, wp, self); wp += 2;
        writeU2(out, wp, superCls); wp += 2;

        int ifCount = u2(bytes, reader); reader += 2;
        int ifLen = 2 + ifCount * 2;
        writeU2(out, wp, ifCount); wp += 2;
        System.arraycopy(bytes, reader, out, wp, ifLen - 2); reader += ifLen - 2; wp += ifLen - 2;

        int fCount = u2(bytes, reader);
        writeU2(out, wp, fCount); wp += 2; reader += 2;
        for (int f = 0; f < fCount; f++) {
            int fAttrCount = u2(bytes, reader + 6);
            int fSize = 8;
            for (int a = 0; a < fAttrCount; a++) {
                int aLen = u4(bytes, reader + fSize + 2);
                fSize += 6 + aLen;
            }
            System.arraycopy(bytes, reader, out, wp, fSize);
            reader += fSize; wp += fSize;
        }

        int mCount = u2(bytes, reader); reader += 2;
        writeU2(out, wp, mCount); wp += 2;

        for (int m = 0; m < mCount; m++) {
            int mStart = reader;
            int mAccess = u2(bytes, mStart);
            int nameIdx = u2(bytes, mStart + 2);
            int descIdx = u2(bytes, mStart + 4);
            int attrCount = u2(bytes, mStart + 6);
            reader += 8;
            String name = utf8(bytes, cpOffs, nameIdx);

            if ("<init>".equals(name)) {
                writeU2(out, wp, mAccess); wp += 2;
                writeU2(out, wp, nameIdx); wp += 2;
                writeU2(out, wp, descIdx); wp += 2;
                writeU2(out, wp, attrCount); wp += 2;

                for (int a = 0; a < attrCount; a++) {
                    int aNameIdx = u2(bytes, reader);
                    int aLen = u4(bytes, reader + 2);
                    String aName = utf8(bytes, cpOffs, aNameIdx);
                    if ("Code".equals(aName)) {
                        copyCodeAttr(bytes, reader + 6, aLen, out, wp + 6, newCpIdx + 6, cpOffs);
                        int oldLen = 6 + aLen;
                        int newLen = oldLen + INJECTION.length;
                        wp += newLen;
                        delta += INJECTION.length;
                    } else {
                        System.arraycopy(bytes, reader, out, wp, 6 + aLen);
                        wp += 6 + aLen;
                    }
                    reader += 6 + aLen;
                }
            } else {
                int mSize = 8;
                int rr = mStart + 8;
                for (int a = 0; a < attrCount; a++) {
                    int aLen = u4(bytes, rr + 2);
                    mSize += 6 + aLen;
                    rr += 6 + aLen;
                }
                System.arraycopy(bytes, mStart, out, wp, mSize); wp += mSize;
                reader = mStart + mSize;
            }
        }

        int tail = bytes.length - reader;
        if (tail > 0) System.arraycopy(bytes, reader, out, wp, tail);
        wp += tail;

        return Arrays.copyOf(out, wp);
    }

    // ── annotation detection ──

    private static boolean hasAnnotation(byte[] bytes) {
        String s = new String(bytes, 0, Math.min(bytes.length, 8192),
                java.nio.charset.StandardCharsets.UTF_8);
        return s.contains("Lcom/melon/foolsEngine/core/annotation/InstanceBusSubscriber;");
    }

    private static boolean isClassFile(byte[] bytes) {
        return bytes.length >= 4
                && (bytes[0] & 0xFF) == 0xCA && (bytes[1] & 0xFF) == 0xFE
                && (bytes[2] & 0xFF) == 0xBA && (bytes[3] & 0xFF) == 0xBE;
    }

    // ── code copy ──

    private static void copyCodeAttr(byte[] src, int codePos, int attrLen,
                                      byte[] dst, int outPos, int cpIdx, int[] cpOffs) {
        int maxStack = u2(src, codePos);
        int maxLocals = u2(src, codePos + 2);
        int codeLen = u4(src, codePos + 4);
        int newCodeLen = codeLen + INJECTION.length;

        writeU2(dst, outPos, Math.max(maxStack, 1));
        writeU2(dst, outPos + 2, maxLocals);
        writeU4(dst, outPos + 4, newCodeLen);

        int dc = outPos + 8;
        System.arraycopy(src, codePos + 8, dst, dc, INJECT_OFFSET); dc += INJECT_OFFSET;
        System.arraycopy(INJECTION, 0, dst, dc, INJECTION.length);
        dst[dc + 1] = (byte) (cpIdx >> 8);
        dst[dc + 2] = (byte) cpIdx;
        dc += INJECTION.length;
        int tailLen = codeLen - INJECT_OFFSET;
        System.arraycopy(src, codePos + 8 + INJECT_OFFSET, dst, dc, tailLen);
        dc += tailLen;

        int excCount = u2(src, codePos + 8 + codeLen);
        writeU2(dst, dc, excCount); dc += 2;
        int ep = codePos + 8 + codeLen + 2;
        for (int e = 0; e < excCount; e++) {
            writeU2(dst, dc, shiftGte(u2(src, ep), INJECT_OFFSET, INJECTION.length)); dc += 2; ep += 2;
            writeU2(dst, dc, shiftGte(u2(src, ep), INJECT_OFFSET, INJECTION.length)); dc += 2; ep += 2;
            writeU2(dst, dc, shiftGte(u2(src, ep), INJECT_OFFSET, INJECTION.length)); dc += 2; ep += 2;
            System.arraycopy(src, ep, dst, dc, 2); dc += 2; ep += 2;
        }

        int subCount = u2(src, ep);
        writeU2(dst, dc, subCount); dc += 2; ep += 2;
        for (int a = 0; a < subCount; a++) {
            int nameIdx = u2(src, ep);
            int len = u4(src, ep + 2);
            String sname = utf8(src, cpOffs, nameIdx);
            writeU2(dst, dc, nameIdx); dc += 2;
            writeU4(dst, dc, len); dc += 2;
            if ("StackMapTable".equals(sname)) {
                copyStackMap(src, ep + 6, len, dst, dc, INJECT_OFFSET, INJECTION.length);
            } else if ("LineNumberTable".equals(sname)) {
                copyOffsetTable(src, ep + 6, dst, dc, 2, INJECT_OFFSET, INJECTION.length);
            } else if ("LocalVariableTable".equals(sname)) {
                copyOffsetTable(src, ep + 6, dst, dc, 5, INJECT_OFFSET, INJECTION.length);
            } else {
                System.arraycopy(src, ep + 6, dst, dc, len);
            }
            ep += 6 + len;
            dc += len;
        }
    }

    // ── stack map table ──

    private static void copyStackMap(byte[] src, int pos, int attrLen,
                                      byte[] dst, int dp, int at, int by) {
        int end = pos + attrLen;
        int sp = pos + 2;
        writeU2(dst, dp, u2(src, pos)); dp += 2;
        int cum = 0;
        while (sp < end) {
            int ft = src[sp] & 0xFF;
            int delta, frameSz;
            if (ft <= 63)          { delta = ft; frameSz = 1; }
            else if (ft <= 127)    { delta = ft - 64; frameSz = 2; }
            else if (ft == 247)    { delta = u2(src, sp + 1); frameSz = 4; }
            else if (ft <= 250)    { delta = u2(src, sp + 1); frameSz = 3; }
            else if (ft == 251)    { delta = u2(src, sp + 1); frameSz = 3; }
            else if (ft <= 254)    { delta = u2(src, sp + 1); frameSz = 3 + (ft - 251) * 3; }
            else /* 255 */         { delta = u2(src, sp + 1);
                                     int lc = u2(src, sp + 3);
                                     int sc = u2(src, sp + 5);
                                     frameSz = 7 + lc * 3 + sc * 3; }
            int effective = cum + delta;
            if (effective > at) {
                delta += by;
            }
            cum += delta;

            if (effective > at) {
                if (ft <= 63 && ft + by > 63) {
                    dst[dp] = (byte) 251; writeU2(dst, dp + 1, delta);
                } else if (ft >= 64 && ft <= 127 && ft + by > 127) {
                    dst[dp] = (byte) 247; writeU2(dst, dp + 1, delta);
                } else if (ft >= 247) {
                    writeU2(dst, dp + 1, delta);
                } else if (ft <= 63) {
                    dst[dp] = (byte) delta;
                } else {
                    dst[dp] = (byte) (delta + 64);
                }
            } else {
                System.arraycopy(src, sp, dst, dp, frameSz);
            }
            sp += frameSz;
            dp += frameSz;
        }
    }

    private static void copyOffsetTable(byte[] src, int pos, byte[] dst, int dp,
                                         int entryWords, int at, int by) {
        int count = u2(src, pos);
        writeU2(dst, dp, count); dp += 2;
        int sp = pos + 2;
        for (int i = 0; i < count; i++) {
            writeU2(dst, dp, shiftGte(u2(src, sp), at, by)); dp += 2; sp += 2;
            System.arraycopy(src, sp, dst, dp, entryWords * 2 - 2);
            dp += entryWords * 2 - 2;
            sp += entryWords * 2 - 2;
        }
    }

    // ── field / interface helpers ──

    private static int copyU2List(byte[] src, int sp, byte[] dst, int dp) {
        int count = u2(src, sp);
        writeU2(dst, dp, count);
        int bytes = 2 + count * 2;
        System.arraycopy(src, sp, dst, dp, bytes);
        return sp + bytes;
    }

    private static int copyFields(byte[] src, int sp, byte[] dst, int dp) {
        return skipFields(src, sp);
    }

    private static int skipFields(byte[] src, int sp) {
        int count = u2(src, sp); sp += 2;
        for (int f = 0; f < count; f++) {
            int attrCount = u2(src, sp + 6);
            sp += 8;
            for (int a = 0; a < attrCount; a++) {
                int len = u4(src, sp + 2);
                sp += 6 + len;
            }
        }
        return sp;
    }

    // ── constant pool ──

    private static int cpSize(byte[] data, int pos, int count) {
        int start = pos - 2;
        for (int i = 1; i < count; i++) {
            int tag = data[pos] & 0xFF; pos++;
            switch (tag) {
                case 1:  pos += 2 + u2(data, pos); break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18: pos += 4; break;
                case 5: case 6: pos += 8; i++; break;
                case 7: case 8: case 16: case 19: case 20: pos += 2; break;
                case 15: pos += 3; break;
            }
        }
        return pos - start;
    }

    private static void buildCpOffsets(byte[] data, int pos, int count, int[] offs) {
        offs[0] = 0;
        for (int i = 1; i < count; i++) {
            offs[i] = pos;
            int tag = data[pos] & 0xFF; pos++;
            switch (tag) {
                case 1:  pos += 2 + u2(data, pos); break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18: pos += 4; break;
                case 5: case 6: pos += 8; i++; break;
                case 7: case 8: case 16: case 19: case 20: pos += 2; break;
                case 15: pos += 3; break;
            }
        }
    }

    private static String utf8(byte[] data, int[] offs, int idx) {
        if (idx <= 0 || idx >= offs.length) return "";
        int off = offs[idx];
        if (off == 0) return "";
        if ((data[off] & 0xFF) != 1) return "";
        int len = u2(data, off + 1);
        return new String(data, off + 3, len, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] buildExtraCp(int baseIdx) {
        byte[] buf = new byte[256];
        int p = 0;
        wUtf(buf, p, "com/melon/foolsEngine/core/events/EventBus"); p += 3 + 48;
        buf[p++] = 7; writeU2(buf, p, baseIdx); p += 2;
        wUtf(buf, p, "addListener"); p += 3 + 11;
        wUtf(buf, p, "(Ljava/lang/Object;)V"); p += 3 + 20;
        buf[p++] = 12; writeU2(buf, p, baseIdx + 2); writeU2(buf, p + 2, baseIdx + 3); p += 4;
        buf[p++] = 10; writeU2(buf, p, baseIdx + 1); writeU2(buf, p + 2, baseIdx + 4); p += 4;
        return Arrays.copyOf(buf, p);
    }

    private static void wUtf(byte[] buf, int p, String s) {
        buf[p] = 1;
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeU2(buf, p + 1, b.length);
        System.arraycopy(b, 0, buf, p + 3, b.length);
    }

    // ── arithmetic ──

    private static int u2(byte[] d, int off) {
        return ((d[off] & 0xFF) << 8) | (d[off + 1] & 0xFF);
    }

    private static int u4(byte[] d, int off) {
        return ((d[off] & 0xFF) << 24) | ((d[off + 1] & 0xFF) << 16)
                | ((d[off + 2] & 0xFF) << 8) | (d[off + 3] & 0xFF);
    }

    private static void writeU2(byte[] d, int off, int v) {
        d[off] = (byte) (v >> 8);
        d[off + 1] = (byte) v;
    }

    private static void writeU4(byte[] d, int off, int v) {
        d[off] = (byte) (v >> 24);
        d[off + 1] = (byte) (v >> 16);
        d[off + 2] = (byte) (v >> 8);
        d[off + 3] = (byte) v;
    }

    private static int shiftGte(int v, int at, int by) {
        return v >= at ? v + by : v;
    }
}
