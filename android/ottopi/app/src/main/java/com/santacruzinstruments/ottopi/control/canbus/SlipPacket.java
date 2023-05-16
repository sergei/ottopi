package com.santacruzinstruments.ottopi.control.canbus;

public class SlipPacket {

    private static final int SLIP_END =  0x00C0;
    private static final int SLIP_ESC = 0x00DB;
    private static final int SLIP_ESC_END = 0x00DC;
    private static final int SLIP_ESC_ESC = 0x00DD;
    private static final int MAX_SLIP_BUFF = 32;

    public interface SlipListener {
        void onPacketReceived(byte[] packet, int len);
    }
    public interface SlipWriter {
        void write(byte[] packet, int len);
    }

    private final SlipListener listener;
    private final SlipWriter writer;
    private boolean escapeNext = false;
    private int slipPacketIndex = 0;
    byte [] packet = new byte[MAX_SLIP_BUFF];
    public SlipPacket(SlipListener listener, SlipWriter writer) {
        this.listener = listener;
        this.writer = writer;
    }

    void onSlipByteReceived(int b) {
        b = b & 0x00FF;
        if( b == SLIP_END){
            listener.onPacketReceived(packet, slipPacketIndex);
            slipPacketIndex = 0;
        }else if (b == SLIP_ESC) {
            escapeNext = true;
        }else {
            if( escapeNext ) {
                escapeNext = false;
                if (b == SLIP_ESC_END) {
                    b = SLIP_END;
                } else if (b == SLIP_ESC_ESC) {
                    b = SLIP_ESC;
                }
            }
            if (slipPacketIndex < MAX_SLIP_BUFF)
                packet[slipPacketIndex++] = (byte)b;
        }
    }

    void encodeAndSendPacket(byte [] data, int  len){
        byte [] packet = new byte[len*2 + 1];
        int packetIndex = 0;
        for (int i = 0; i < len; i++) {
            if (data[i] == (byte)SLIP_END) {
                packet[packetIndex++] = (byte)SLIP_ESC;
                packet[packetIndex++] = (byte)SLIP_ESC_END;
            } else if (data[i] == (byte)SLIP_ESC) {
                packet[packetIndex++] = (byte)SLIP_ESC;
                packet[packetIndex++] = (byte)SLIP_ESC_ESC;
            } else {
                packet[packetIndex++] = data[i];
            }
        }
        packet[packetIndex++] = (byte)SLIP_END;
        writer.write(packet, packetIndex);
    }


}
