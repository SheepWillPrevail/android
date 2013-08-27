package com.grazz.pebblerss.image;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;

public class PebbleImageKit {

	private static int calculateBytesPerRow(int width) {
		int bytesPerRow = (width + 7) / 8;
		return (bytesPerRow + 3) & ~0x03;
	}

	public static ByteBuffer convertBitmapToBytes(Bitmap source) {
		int bytesPerRow = calculateBytesPerRow(source.getWidth());
		ByteBuffer bytes = ByteBuffer.allocate(bytesPerRow * source.getHeight());
		byte[] buffer = new byte[bytesPerRow];
		for (int y = 0; y < source.getHeight() - 1; y++) {
			for (int i = 0; i < bytesPerRow; i++)
				buffer[i] = 0;
			for (int x = 0; x < source.getWidth() - 1; x++) {
				int pixel = source.getPixel(x, y);
				int luminance = (int) ((Color.red(pixel) * 0.3f) + (Color.green(pixel) * 0.59f) + (Color.blue(pixel) * 0.11f));
				buffer[x >> 3] |= (luminance < 128 ? 0 : 1) << (x % 8);
			}
			bytes.put(buffer);
		}
		bytes.rewind();
		return bytes;
	}

	public static Bitmap ConvertBytesToBitmap(ByteBuffer source, int width, int height) {
		int bytesPerRow = calculateBytesPerRow(width);
		Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		byte[] buffer = new byte[bytesPerRow];
		for (int y = 0; y < height - 1; y++) {
			for (int i = 0; i < bytesPerRow; i++)
				buffer[i] = 0;
			source.get(buffer);
			for (int x = 0; x < width - 1; x++)
				bitmap.setPixel(x, y, ((buffer[x >> 3] >> (x % 8)) & 1) == 1 ? Color.BLACK : Color.WHITE);
		}
		return bitmap;
	}

}
