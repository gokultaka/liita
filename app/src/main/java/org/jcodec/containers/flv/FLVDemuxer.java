package org.jcodec.containers.flv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.flv.FLVPacket.AacAudioTagHeader;
import org.jcodec.containers.flv.FLVPacket.AudioTagHeader;
import org.jcodec.containers.flv.FLVPacket.AvcVideoTagHeader;
import org.jcodec.containers.flv.FLVPacket.TagHeader;
import org.jcodec.containers.flv.FLVPacket.Type;
import org.jcodec.containers.flv.FLVPacket.VideoTagHeader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * FLV ( Flash Media Video ) demuxer
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class FLVDemuxer {

    // Read buffer, 1M
    private static final int READ_BUFFER_SIZE = 0x100000;
    private LinkedList<FLVPacket> prevPkt = new LinkedList<FLVPacket>();
    private int frameNo;
    private byte[] metadata;
    private ByteBuffer readBuf;
    private ReadableByteChannel ch;
    private long globalPos;

    private static boolean platformBigEndian = ByteBuffer.allocate(0).order() == ByteOrder.BIG_ENDIAN;

    public static Codec[] audioCodecMapping = new Codec[] { Codec.PCM, Codec.ADPCM, Codec.MP3, Codec.PCM,
            Codec.NELLYMOSER, Codec.NELLYMOSER, Codec.NELLYMOSER, Codec.G711, Codec.G711, null, Codec.AAC, Codec.SPEEX,
            Codec.MP3, null };
    public static Codec[] videoCodecMapping = new Codec[] { null, null, Codec.SORENSON, Codec.FLASH_SCREEN_VIDEO,
            Codec.VP6, Codec.VP6, Codec.FLASH_SCREEN_V2, Codec.H264 };

    public static int[] sampleRates = new int[] { 5500, 11000, 22000, 44100 };

    public FLVDemuxer(ReadableByteChannel ch) throws IOException {
        this.ch = ch;
        readBuf = ByteBuffer.allocate(READ_BUFFER_SIZE);
        readBuf.order(ByteOrder.BIG_ENDIAN);
        int read = ch.read(readBuf);
        globalPos += read == -1 ? 0 : read;
        readBuf.flip();

        readHeader(readBuf);
    }

    public FLVPacket getPacket() throws IOException {
        FLVPacket pkt = readPacket(readBuf);
        if (pkt == null) {
            relocateBytes(readBuf);
            int read = ch.read(readBuf);
            globalPos += read == -1 ? 0 : read;
            readBuf.flip();
            pkt = readPacket(readBuf);
        }

        // Empty the queue
        if (pkt == null && prevPkt.size() > 0) {
            pkt = prevPkt.remove(0);
        }

        return pkt;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    private static void relocateBytes(ByteBuffer readBuf) {
        int rem = readBuf.remaining();
        for (int i = 0; i < rem; i++) {
            readBuf.put(i, readBuf.get());
        }
        readBuf.clear();
        readBuf.position(rem);
    }

    private FLVPacket readPacket(ByteBuffer readBuf) {
        for (;;) {
            if (readBuf.remaining() < 15) {
                return null;
            }

            int pos = readBuf.position();
            long packetPos = globalPos - pos;
            int startOfLastPacket = readBuf.getInt();
            int packetType = readBuf.get() & 0xff;
            int payloadSize = ((readBuf.getShort() & 0xffff) << 8) | (readBuf.get() & 0xff);
            int timestamp = ((readBuf.getShort() & 0xffff) << 8) | (readBuf.get() & 0xff)
                    | ((readBuf.get() & 0xff) << 24);
            int streamId = ((readBuf.getShort() & 0xffff) << 8) | (readBuf.get() & 0xff);

            if (readBuf.remaining() < payloadSize) {
                readBuf.position(pos);
                return null;
            }
            if (packetType == 0x12) {
                System.out.println("META");
                metadata = NIOUtils.toArray(NIOUtils.read(readBuf, payloadSize));
                FLVMetadata meta = parseMetadata(ByteBuffer.wrap(metadata));
                System.out.println(ToJSON.toJSON(meta));
                continue;
            } else if (packetType != 0x8 && packetType != 0x9) {
                NIOUtils.skip(readBuf, payloadSize);
                continue;
            }

            ByteBuffer payload = NIOUtils.clone(NIOUtils.read(readBuf, payloadSize));

            Type type;
            TagHeader tagHeader;
            if (packetType == 0x8) {
                type = Type.AUDIO;
                tagHeader = parseAudioTagHeader(payload);
            } else if (packetType == 0x9) {
                type = Type.VIDEO;
                tagHeader = parseVideoTagHeader(payload);
            } else {
                System.out.println("NON AV packet");
                continue;
            }
            boolean keyFrame = packetType == 0x8 || ((VideoTagHeader) tagHeader).getFrameType() == 1;

            FLVPacket pkt = new FLVPacket(type, payload, timestamp, 0, frameNo++, true, metadata, packetPos, tagHeader);

            for (ListIterator<FLVPacket> it = prevPkt.listIterator(prevPkt.size()); it.hasPrevious();) {
                FLVPacket flvPacket = it.previous();
                if (flvPacket.getType() == pkt.getType())
                    flvPacket.setDuration(timestamp - flvPacket.getPts());
            }
            prevPkt.add(pkt);

            if (!prevPkt.isEmpty() && prevPkt.peek().getDuration() != 0)
                return prevPkt.poll();
        }
    }

    private static void readHeader(ByteBuffer readBuf) throws IOException {
        if (readBuf.get() != 'F' || readBuf.get() != 'L' || readBuf.get() != 'V' || readBuf.get() != 1
                || (readBuf.get() & 0x5) == 0 || readBuf.getInt() != 9) {
            throw new IOException("Invalid FLV file");
        }
    }

    private static FLVMetadata parseMetadata(ByteBuffer bb) {
        if ("onMetaData".equals(readAMFData(bb, -1)))
            return new FLVMetadata((Map<String, Object>) readAMFData(bb, -1));
        return null;
    }

    private static Object readAMFData(ByteBuffer input, int type) {
        if (type == -1) {
            type = input.get() & 0xff;
        }
        switch (type) {
        case 0:
            return input.getDouble();
        case 1:
            return input.get() == 1;
        case 2:
            return readAMFString(input);
        case 3:
            return readAMFObject(input);
        case 8:
            return readAMFEcmaArray(input);
        case 10:
            return readAMFStrictArray(input);
        case 11:
            final Date date = new Date((long) input.getDouble());
            input.getShort(); // time zone
            return date;
        case 13:
            return "UNDEFINED";
        default:
            return null;
        }
    }

    private static Object readAMFStrictArray(ByteBuffer input) {
        int count = input.getInt();
        Object[] result = new Object[count];
        for (int i = 0; i < count; i++) {
            result[i] = readAMFData(input, -1);
        }
        return result;
    }

    private static String readAMFString(ByteBuffer input) {
        int size = input.getShort() & 0xffff;
        return new String(NIOUtils.toArray(NIOUtils.read(input, size)), Charset.forName("UTF-8"));
    }

    private static Object readAMFObject(ByteBuffer input) {
        Map<String, Object> array = new HashMap<String, Object>();
        while (true) {
            String key = readAMFString(input);
            int dataType = input.get() & 0xff;
            if (dataType == 9) { // object end marker
                break;
            }
            array.put(key, readAMFData(input, dataType));
        }
        return array;
    }

    private static Object readAMFEcmaArray(ByteBuffer input) {
        long size = input.getInt();
        Map<String, Object> array = new HashMap<String, Object>();
        for (int i = 0; i < size; i++) {
            String key = readAMFString(input);
            int dataType = input.get() & 0xff;
            array.put(key, readAMFData(input, dataType));
        }
        return array;
    }

    public static VideoTagHeader parseVideoTagHeader(ByteBuffer bb) {
        ByteBuffer dup = bb.duplicate();
        byte b0 = dup.get();
        int frameType = (b0 & 0xff) >> 4;
        int codecId = (b0 & 0xf);
        Codec codec = videoCodecMapping[codecId];

        if (codecId == 7) {
            byte avcPacketType = dup.get();
            if (avcPacketType == 0)
                System.out.println("SPS/PPS");
            int compOffset = (dup.getShort() << 8) | (dup.get() & 0xff);
            return new AvcVideoTagHeader(codec, frameType, avcPacketType, compOffset);
        }

        return new VideoTagHeader(codec, frameType);
    }

    public static TagHeader parseAudioTagHeader(ByteBuffer bb) {
        ByteBuffer dup = bb.duplicate();
        byte b = dup.get();

        int codecId = (b & 0xff) >> 4;
        int sampleRate = sampleRates[(b >> 2) & 0x3];
        if (codecId == 4 || codecId == 11)
            sampleRate = 16000;
        if (codecId == 5 || codecId == 14)
            sampleRate = 8000;

        int sampleSizeInBits = (b & 0x2) == 0 ? 8 : 16;
        boolean signed = codecId != 3 && codecId != 0 || sampleSizeInBits == 16;
        int channelCount = 1 + (b & 1);
        if (codecId == 11)
            channelCount = 1;

        AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channelCount, signed,
                codecId == 3 ? false : platformBigEndian);
        Codec codec = audioCodecMapping[codecId];
        if (codecId == 10) {
            byte packetType = dup.get();
            return new AacAudioTagHeader(codec, audioFormat, packetType);
        }

        return new AudioTagHeader(codec, audioFormat);
    }
}
