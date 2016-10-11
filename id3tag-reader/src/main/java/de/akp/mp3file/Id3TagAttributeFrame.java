package de.akp.mp3file;

public enum Id3TagAttributeFrame {

	TFLT,
	TALB,
	TIT2, TPE1("Lead performer(s)/Soloist(s)"), TPR2, TRCK, TPOS, TCON, TSIZ,
	;

	private AttributeValue<?> val;
	private Class<?> klasse;

	private Id3TagAttributeFrame() {

	}

	private Id3TagAttributeFrame(String s) {

	}

	private Id3TagAttributeFrame(AttributeValue<?> av, Class<?> k) {
		val = av;
		klasse = k;
	}

	public Class<?> getKlasse() {
		return klasse;
	}

	public <T> T getType(Id3TagAttributeFrame atr, Class<T> klasse) {
		return  klasse.cast(val);
	}

	public static class AttributeValue<T> {
		public Id3TagAttributeFrame valueOf(T t) {
			return null;
		}
	}

}
