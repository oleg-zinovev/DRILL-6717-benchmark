/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package ozinoviev;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;

@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 10, time = 1)
@Threads(Threads.MAX)
public class JavaStringBenchmark {

    @Param({"10", "100", "1000", "10000", "100000"})
    public int size;

    @Param({"ASCII", "NON_ASCII", "MIXED"})
    public CharList charList;

    int bufferSize;
    ByteBuf buffer;
    ByteBuf output;

    @SuppressWarnings("unused")
    @Setup
    public void init() {
        String random = RandomStringUtils.random(size, charList.getChars());
        byte[] bytes = random.getBytes(StandardCharsets.UTF_8);
        buffer = Unpooled.directBuffer(bytes.length)
                .writeBytes(bytes)
                .resetWriterIndex()
                .resetReaderIndex();

        bufferSize = bytes.length;

        output = Unpooled.directBuffer(bufferSize);
    }

    @Benchmark
    public void testBenZviToUpper() {
        byte[] bytes = new byte[bufferSize];
        buffer.getBytes(0, bytes);
        String str = new String(bytes, StandardCharsets.UTF_8);
        bytes = str.toUpperCase().getBytes(StandardCharsets.UTF_8);
        output.setBytes(0, bytes);
    }

    @Benchmark
    public void testOZToUpper() {

        for (int id = 0; id < bufferSize; id++) {
            byte currentByte = buffer.getByte(id);

            int length = utf8CharLen(currentByte);
            if (length == 1) {
                output.setByte(id, Character.toUpperCase(currentByte));
            } else {
                byte[] symbolBytes = new byte[length];
                buffer.getBytes(id, symbolBytes);

                byte[] encoded = new String(symbolBytes, StandardCharsets.UTF_8).toUpperCase().getBytes(StandardCharsets.UTF_8);
                output.setBytes(id, encoded);

                id += length - 1;
            }
        }
    }

    @Benchmark
    public void testMergedToUpper(Blackhole blackhole) {

        int id;
        for (id = 0; id < bufferSize; id++) {
            byte currentByte = buffer.getByte(id);

            int length = utf8CharLen(currentByte);
            if (length == 1) {
                output.setByte(id, Character.toUpperCase(currentByte));
            } else {
                break;
            }
        }

        if (id != bufferSize) {
            byte[] remaining = new byte[bufferSize - id];
            buffer.getBytes(id, remaining);

            byte[] encoded = new String(remaining, StandardCharsets.UTF_8).toUpperCase().getBytes(StandardCharsets.UTF_8);
            output.setBytes(id, encoded);
        }
    }

    @Benchmark
    public void testBenZviToLower() {
        byte[] bytes = new byte[bufferSize];
        buffer.getBytes(0, bytes);
        String str = new String(bytes, StandardCharsets.UTF_8);
        bytes = str.toLowerCase().getBytes(StandardCharsets.UTF_8);
        output.setBytes(0, bytes);
    }

    @Benchmark
    public void testOZToLower() {

        for (int id = 0; id < bufferSize; id++) {
            byte currentByte = buffer.getByte(id);

            int length = utf8CharLen(currentByte);
            if (length == 1) {
                output.setByte(id, Character.toLowerCase(currentByte));
            } else {
                byte[] symbolBytes = new byte[length];
                buffer.getBytes(id, symbolBytes);

                byte[] encoded = new String(symbolBytes, StandardCharsets.UTF_8).toLowerCase().getBytes(StandardCharsets.UTF_8);
                output.setBytes(id, encoded);

                id += length - 1;
            }
        }
    }

    @Benchmark
    public void testMergedToLower(Blackhole blackhole) {
        int id;
        for (id = 0; id < bufferSize; id++) {
            byte currentByte = buffer.getByte(id);

            int length = utf8CharLen(currentByte);
            if (length == 1) {
                blackhole.consume(Character.toLowerCase(currentByte));
            } else {
                break;
            }
        }

        if (id != bufferSize) {
            byte[] remaining = new byte[bufferSize - id];
            buffer.getBytes(id, remaining);

            byte[] encoded = new String(remaining, StandardCharsets.UTF_8).toLowerCase().getBytes(StandardCharsets.UTF_8);
            output.setBytes(id, encoded);
        }
    }

    public static int utf8CharLen(byte currentByte) {
        if (currentByte >= 0) {                 // 1-byte char. First byte is 0xxxxxxx.
            return 1;
        } else if ((currentByte & 0xE0) == 0xC0) {   // 2-byte char. First byte is 110xxxxx
            return 2;
        } else if ((currentByte & 0xF0) == 0xE0) {   // 3-byte char. First byte is 1110xxxx
            return 3;
        } else if ((currentByte & 0xF8) == 0xF0) {    //4-byte char. First byte is 11110xxx
            return 4;
        }
        throw new IllegalStateException("Unexpected byte 0x" + Integer.toString((int) currentByte & 0xff, 16) + " encountered while decoding UTF8 string.");
    }

}
