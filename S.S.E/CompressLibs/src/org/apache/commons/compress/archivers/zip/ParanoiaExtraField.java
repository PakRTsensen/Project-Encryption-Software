package org.apache.commons.compress.archivers.zip;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ParanoiaExtraField extends UnrecognizedExtraField implements ZipExtraField {
	
	private byte[] version = new byte[1];
	private byte[] unixMode;
	private byte[] data;
	
	public ParanoiaExtraField(){}
	
	public ParanoiaExtraField(short version, short unixMode)
	{
		this.unixMode = shortToByteArray(unixMode);
		this.version[0] = ((Short)version).byteValue();
	}
		
	public short getVersion()
	{
		return this.version[0];
	}
	
	public short getUnixMode()
	{
		return byteArrayToShort(this.unixMode);
	}
	
	
	@Override
	public ZipShort getHeaderId() {
		return new ZipShort(0x6570);
	}

	@Override
	public ZipShort getLocalFileDataLength() {
		return new ZipShort(getLocalFileDataData().length);
	}

	@Override
	public ZipShort getCentralDirectoryLength() {
		return getLocalFileDataLength();
	}

	@Override
	public byte[] getLocalFileDataData() {
        if (data == null) {
            this.assembleData();
        }
        return data;
	}

	@Override
	public byte[] getCentralDirectoryData() {
		return getLocalFileDataData();
	}

	@Override
	public void parseFromLocalFileData(byte[] buffer, int offset, int length) {
		version = new byte[1];
		unixMode = new byte[length];
		System.arraycopy(buffer, offset, version, 0, 1);
        System.arraycopy(buffer, offset + 1, unixMode, 0, length - 1);
        data = null;
	}

	@Override
	public void parseFromCentralDirectoryData(byte[] buffer, int offset, int length) {
		parseFromCentralDirectoryData(buffer, offset, length);
	}
	
    private void assembleData() 
    {
        data = new byte[1 + unixMode.length];
        System.arraycopy(version, 0, data, 0, 1);
        System.arraycopy(unixMode, 0, data, 1, unixMode.length);
    }
    
	private static byte[] shortToByteArray(short value) {
	    return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
	}

	private static short byteArrayToShort(byte [] byteBarray) {
	    return ByteBuffer.wrap(byteBarray).order(ByteOrder.LITTLE_ENDIAN).getShort();
	}
}
