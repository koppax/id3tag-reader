package de.akp.mp3file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class Mp3File {

	private static final int FRAMEHEADERLENGTH = 10;
	private Path path;
	private MappedByteBuffer buffer;
	private int framePointer = 10;
	private boolean isMp3File;

	public Mp3File(Path osFile) {
		this.path = osFile;

		try {
			openBuffer();
		} catch (IOException e) {
			e.printStackTrace();
			isMp3File = false;
		}
	}

	private void openBuffer() throws IOException {

		try (FileChannel channel = (FileChannel) Files.newByteChannel(path, StandardOpenOption.READ)) {
			buffer = channel.map(MapMode.READ_ONLY, 0, Files.size(path));
		}
		if(buffer.limit() < 3) {
			//throw new IOException("BufferUnderflow bei Datei \"" + path.getFileName().toString() + "\"!");
			isMp3File = false;
			return ;
		}
		isMp3File = (buffer.get() == 0x49 && buffer.get() == 0x44 && buffer.get() == 0x33);
		buffer.position(0);

	}

	private int readUnsynchedIntegerStartingFrom(int pos) {
		if(pos < 3) throw new IllegalStateException();
		int position = buffer.position();
		buffer.position(pos);
		int result = (buffer.get() & 0x7F) << 21
				| (buffer.get() & 0x7F) << 14
				| (buffer.get() & 0x7F) << 7
				| (buffer.get() & 0x7F) << 0;
		buffer.position(position);
		return result;
	}

	private void movePositionToFirstFrame() {
		framePointer = 10;
		buffer.position(framePointer);
	}

	public boolean hasMp3Header() {
		return isMp3File;
	}

	public int readLengthOfCurrentFrame() {
		int pos = framePointer + 4;
		return readUnsynchedIntegerStartingFrom(pos);
	}

	private String readContentOfCurrentFrame() {
		int len = readLengthOfCurrentFrame();
		if(len <= 2) return "";
		byte[] content = new byte[len];
		Charset encoding = readEncoding();
		buffer.get(content, 0, content.length);
//		if(content.length <= 2) return "";
		int last = len - 1;
		int start = 0;
		if(content[0] == 0x00 || content[0] == 0x03 ) {
			start = 1;
			last -= 1;
		} else if(content[0] == 0x01  && (content[1] == 0xFFFFFFFF || content[1] == 0xFFFFFFFE) ) { // BOM
			start = 1;
			last -= 2;
		}
		String contentAsString = encoding.decode(ByteBuffer.wrap(content, start, last)).toString();
		return contentAsString;
	}

	public boolean findNextFrame() {
		int len = readLengthOfCurrentFrame() + FRAMEHEADERLENGTH;
		if(! isValidPosition(len)) {
			movePosition(10);
			framePointer = 10;
			return false;
		}
		framePointer = framePointer + len;
		return isValidPosition(framePointer + FRAMEHEADERLENGTH + 1);
	}

	private Charset readEncoding() {
		movePosition(framePointer + FRAMEHEADERLENGTH);
		Charset result = StandardCharsets.ISO_8859_1;
		buffer.position();
		byte b = buffer.get();
		if(b == 0x03) {
			result = StandardCharsets.UTF_8;
		} else if(b == 0x01) {
			result = StandardCharsets.UTF_16;
		}
		movePosition(framePointer + FRAMEHEADERLENGTH);
		return result;
	}

	private boolean isValidPosition(int movePosition) {
		if(framePointer + movePosition >= buffer.capacity())
			return false;
		if(movePosition < 0)
			return false;
		return true;
	}

	private void movePosition(int len) {
		buffer.position(len);
	}

	public String getMp3Attribute(Id3TagAttributeFrame tflt) {
		if(!isMp3File) return "";
		if(moveToFrame(tflt)) {
			return readContentOfCurrentFrame();
		}
		return "";
	}

	public String getMp3AttributeForKey(String key) {
		if(Objects.toString(key, "").isEmpty()) {
			throw new NullPointerException();
		}

		Id3TagAttributeFrame attribute = null;
		attribute = Id3TagAttributeFrame.valueOf(key);
		if(attribute == null) {
			throw new IllegalArgumentException("Invalid Key: " + key);
		}
		return getMp3Attribute(attribute);
	}

	private boolean moveToFrame(String name) {
		byte[] bytes= name.getBytes();
		movePositionToFirstFrame();
		while(!bytesFitToBuffer(bytes)) {
			if(!findNextFrame()) {
				return false;
			}
		}
		return true;
	}

	private boolean moveToFrame(Id3TagAttributeFrame tflt) {
		return moveToFrame(tflt.name());
	}


	private boolean bytesFitToBuffer(byte[] bytes) {
		buffer.position(framePointer);
		for (int i = 0; i < bytes.length; i++) {
			byte b = buffer.get();
			if(b != bytes[i]) {
				return false;
			}
		}
		return true;
	}

	public int getMp3HeaderLength() {
		if(!isMp3File) {
			throw new IllegalArgumentException("\"" + path.getFileName() + "\" is not a Mp3Datei or has no IDv2-Header!");
		}
		return readUnsynchedIntegerStartingFrom(6);
	}
	
	public Path getPath() {
		return this.path;
	}

	public String getNameOfCurrentFrame() {
		if(!isMp3File) return "";
		int position = buffer.position();
		byte[] b = new byte[4];
		buffer.position(framePointer);
		buffer.get(b);
		buffer.position(position);
		return StandardCharsets.ISO_8859_1.decode(ByteBuffer.wrap(b)).toString();
	}


}
