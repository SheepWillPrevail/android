package com.grazz.pebblerss.kits;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.getpebble.android.kit.util.PebbleDictionary;

public class ChunkTransferKit {

	private final static int CHUNK_SIZE = 64;
	private final static int PARAM_DATA = 9999;
	private final static int PARAM_OFFSET = 9998;
	private final static int PARAM_LENGTH = 9997;
	private final static int PARAM_PARTS = 9996;

	private ByteBuffer _source;

	public ChunkTransferKit(ByteBuffer source) {
		_source = source;
		_source.rewind();
	}

	public List<PebbleDictionary> getDictionaries() {
		List<PebbleDictionary> dictionaries = new ArrayList<PebbleDictionary>();

		int parts = (_source.remaining() + (CHUNK_SIZE - 1)) / CHUNK_SIZE;
		int offset = 0;
		for (int i = 0; i < parts; i++) {
			int length = _source.remaining();
			if (length > CHUNK_SIZE)
				length = CHUNK_SIZE;
			byte[] buffer = new byte[length];
			_source.get(buffer, 0, length);
			PebbleDictionary dictionary = new PebbleDictionary();
			dictionary.addBytes(PARAM_DATA, buffer);
			dictionary.addUint16(PARAM_OFFSET, (short) offset);
			dictionary.addUint8(PARAM_LENGTH, (byte) length);
			dictionary.addUint8(PARAM_PARTS, (byte) parts);
			dictionaries.add(dictionary);
			offset += length;
		}

		return dictionaries;
	}
}
