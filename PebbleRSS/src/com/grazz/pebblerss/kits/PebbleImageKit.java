package com.grazz.pebblerss.kits;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;

public class PebbleImageKit {

	private static final int MAX_IMAGE_WIDTH = 144;
	private static final int MAX_IMAGE_HEIGHT = 152;

	public static int calculateBytesPerRow(int width) {
		int bytesPerRow = (width + 7) / 8;
		return (bytesPerRow + 3) & ~0x03;
	}

	private static void safeIntBufferAdd(IntBuffer dither, int i, int error) {
		if (i > dither.capacity() - 1 || i < 0)
			return;
		int value = dither.get(i);
		value += error;
		dither.put(i, value);
	}

	// thank you http://en.wikipedia.org/wiki/Bill_Atkinson
	public static ByteBuffer convertBitmapToBytes(Bitmap source) {
		int width = source.getWidth();
		int height = source.getHeight();
		int bytesPerRow = calculateBytesPerRow(width);

		IntBuffer dither = IntBuffer.allocate(width * height);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++) {
				int i = x + (y * width);
				int pixel = source.getPixel(x, y);
				int luminance = (int) ((Color.red(pixel) * 0.3f) + (Color.green(pixel) * 0.59f) + (Color.blue(pixel) * 0.11f));
				luminance += dither.get(i);
				int value = luminance < 128 ? 0 : 255;
				int error = (luminance - value) / 8;
				dither.put(i, value);
				safeIntBufferAdd(dither, i + 1, error);
				safeIntBufferAdd(dither, i + 2, error);
				safeIntBufferAdd(dither, i + width - 1, error);
				safeIntBufferAdd(dither, i + width, error);
				safeIntBufferAdd(dither, i + width + 1, error);
				safeIntBufferAdd(dither, i + (2 * width), error);
			}

		byte[] row = new byte[bytesPerRow];
		ByteBuffer bytes = ByteBuffer.allocate(bytesPerRow * height);
		for (int y = 0; y < height; y++) {
			for (int i = 0; i < bytesPerRow; i++)
				row[i] = 0;
			for (int x = 0; x < width; x++) {
				int pixel = dither.get(x + (y * width));
				row[x >> 3] |= (pixel < 128 ? 0 : 1) << (x % 8);
			}
			bytes.put(row);
		}
		bytes.rewind();

		return bytes;
	}

	public static Bitmap convertBytesToBitmap(ByteBuffer source, int width, int height) {
		int bytesPerRow = calculateBytesPerRow(width);

		if (source.remaining() < bytesPerRow * height)
			throw new IllegalArgumentException("source has insufficient data for specified image dimensions");

		Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		byte[] row = new byte[bytesPerRow];
		for (int y = 0; y < height - 1; y++) {
			source.get(row);
			for (int x = 0; x < width - 1; x++)
				bitmap.setPixel(x, y, ((row[x >> 3] >> (x % 8)) & 1) == 1 ? Color.WHITE : Color.BLACK);
		}

		return bitmap;
	}

	public static Bitmap conformImageToPebble(Bitmap source) {
		float tempWidth = source.getWidth();
		float tempHeight = source.getHeight();

		if (tempWidth > MAX_IMAGE_WIDTH) {
			tempWidth = MAX_IMAGE_WIDTH;
			tempHeight *= tempWidth / source.getWidth();
		}

		if (tempHeight > MAX_IMAGE_HEIGHT) {
			float previousHeight = tempHeight;
			tempHeight = MAX_IMAGE_HEIGHT;
			tempWidth *= tempHeight / previousHeight;
		}

		return Bitmap.createScaledBitmap(source, (int) tempWidth, (int) tempHeight, true);
	}
}
