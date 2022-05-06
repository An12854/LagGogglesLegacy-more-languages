package com.falsepattern.laggoggles.packet;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class CPacketRequestScan implements IMessage{

    public CPacketRequestScan(){
        length = 5;
    }

    public int length;

    @Override
    public void fromBytes(ByteBuf buf) {
        length = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(length);
    }
}
