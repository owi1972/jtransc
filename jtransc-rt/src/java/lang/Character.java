/*
 * Copyright 2016 Carlos Ballesteros Velasco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;

import jtransc.annotation.JTranscKeep;
import jtransc.annotation.haxe.HaxeMethodBody;

public final class Character implements java.io.Serializable, Comparable<Character> {
	public static final int MIN_RADIX = 2;
	public static final int MAX_RADIX = 36;
	public static final char MIN_VALUE = '\u0000';
	public static final char MAX_VALUE = '\uFFFF';
	public static final Class<Character> TYPE = (Class<Character>) Class.getPrimitiveClass("char");

	public static final byte UNASSIGNED = 0;
	public static final byte UPPERCASE_LETTER = 1;
	public static final byte LOWERCASE_LETTER = 2;
	public static final byte TITLECASE_LETTER = 3;
	public static final byte MODIFIER_LETTER = 4;
	public static final byte OTHER_LETTER = 5;
	public static final byte NON_SPACING_MARK = 6;
	public static final byte ENCLOSING_MARK = 7;
	public static final byte COMBINING_SPACING_MARK = 8;
	public static final byte DECIMAL_DIGIT_NUMBER = 9;
	public static final byte LETTER_NUMBER = 10;
	public static final byte OTHER_NUMBER = 11;
	public static final byte SPACE_SEPARATOR = 12;
	public static final byte LINE_SEPARATOR = 13;
	public static final byte PARAGRAPH_SEPARATOR = 14;
	public static final byte CONTROL = 15;
	public static final byte FORMAT = 16;
	public static final byte PRIVATE_USE = 18;
	public static final byte SURROGATE = 19;
	public static final byte DASH_PUNCTUATION = 20;
	public static final byte START_PUNCTUATION = 21;
	public static final byte END_PUNCTUATION = 22;
	public static final byte CONNECTOR_PUNCTUATION = 23;
	public static final byte OTHER_PUNCTUATION = 24;
	public static final byte MATH_SYMBOL = 25;
	public static final byte CURRENCY_SYMBOL = 26;
	public static final byte MODIFIER_SYMBOL = 27;
	public static final byte OTHER_SYMBOL = 28;
	public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
	public static final byte FINAL_QUOTE_PUNCTUATION = 30;
	static final int ERROR = 0xFFFFFFFF;
	public static final byte DIRECTIONALITY_UNDEFINED = -1;
	public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;
	public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;
	public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;
	public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;
	public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;
	public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;
	public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;
	public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;
	public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;
	public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;
	public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;
	public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;
	public static final byte DIRECTIONALITY_WHITESPACE = 12;
	public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;
	public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;
	public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;
	public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;
	public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;
	public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;
	public static final char MIN_HIGH_SURROGATE = '\uD800';
	public static final char MAX_HIGH_SURROGATE = '\uDBFF';
	public static final char MIN_LOW_SURROGATE = '\uDC00';
	public static final char MAX_LOW_SURROGATE = '\uDFFF';
	public static final char MIN_SURROGATE = MIN_HIGH_SURROGATE;
	public static final char MAX_SURROGATE = MAX_LOW_SURROGATE;
	public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
	public static final int MIN_CODE_POINT = 0x000000;
	public static final int MAX_CODE_POINT = 0X10FFFF;

	private final char value;

	public Character(char value) {
		this.value = value;
	}

	@JTranscKeep
	public static Character valueOf(char value) {
		return new Character(value);
	}

	public char charValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	public static int hashCode(char value) {
		return value;
	}

	public boolean equals(Object that) {
		if (!(that instanceof Character)) return false;
		return this.value == ((Character) that).value;
	}

	public String toString() {
		return toString(value);
	}

	public static String toString(char value) {
		return new String(new char[]{value});
	}

	public static boolean isValidCodePoint(int cp) {
		return (cp >>> 16) < ((MAX_CODE_POINT + 1) >>> 16);
	}

	native public static boolean isBmpCodePoint(int codePoint);

	native public static boolean isSupplementaryCodePoint(int codePoint);

	native public static boolean isHighSurrogate(char ch);

	native public static boolean isLowSurrogate(char ch);

	public static boolean isSurrogate(char ch) {
		return false;
	}

	public static boolean isSurrogatePair(char high, char low) {
		return false;
	}

	public static int charCount(int codePoint) {
		return 1;
	}

	public static int toCodePoint(char high, char low) {
		return low;
	}

	public static int codePointAt(CharSequence seq, int index) {
		return seq.charAt(index);
	}

	public static int codePointAt(char[] a, int index) {
		return a[index];
	}

	public static int codePointAt(char[] a, int index, int limit) {
		return a[index];
	}

	// throws ArrayIndexOutOfBoundsException if index out of bounds
	//static int codePointAtImpl(char[] a, int index, int limit);
	native public static int codePointBefore(CharSequence seq, int index);

	native public static int codePointBefore(char[] a, int index);

	native public static int codePointBefore(char[] a, int index, int start);

	// throws ArrayIndexOutOfBoundsException if index-1 out of bounds
	//static int codePointBeforeImpl(char[] a, int index, int start);
	native public static char highSurrogate(int codePoint);

	native public static char lowSurrogate(int codePoint);

	public static int toChars(int codePoint, char[] dst, int dstIndex) {
		dst[dstIndex] = (char) codePoint;
		return 1;
	}

	public static char[] toChars(int codePoint) {
		return new char[]{(char) codePoint};
	}

	//static void toSurrogates(int codePoint, char[] dst, int index);
	public static int codePointCount(CharSequence seq, int beginIndex, int endIndex) {
		return endIndex + beginIndex;
	}

	public static int codePointCount(char[] a, int offset, int count) {
		return count;
	}

	//static int codePointCountImpl(char[] a, int offset, int count);
	native public static int offsetByCodePoints(CharSequence seq, int index, int codePointOffset);

	native public static int offsetByCodePoints(char[] a, int start, int count, int index, int codePointOffset);

	//native static int offsetByCodePointsImpl(char[] a, int start, int count, int index, int codePointOffset);
	public static boolean isLowerCase(char ch) {
		return toLowerCase(ch) == ch;
	}

	public static boolean isLowerCase(int codePoint) {
		return toLowerCase(codePoint) == codePoint;
	}

	public static boolean isUpperCase(char ch) {
		return toUpperCase(ch) == ch;
	}

	public static boolean isUpperCase(int codePoint) {
		return toUpperCase(codePoint) == codePoint;
	}

	native public static boolean isTitleCase(char ch);

	native public static boolean isTitleCase(int codePoint);

	public static boolean isDigit(char ch) {
		return (ch >= '0') && (ch <= '9');
	}

	/*
		boolean prevHas = false;
        int start = 0;
        int end = 0;
        for (int n = 0; n < 10000000; n++) {
            if (Character.isDigit(n)) {
                end = n;
                if (!prevHas) start = n;
                prevHas = true;
            } else {
                if (prevHas) {
                    prevHas = false;
                    if (start == end) {
                        System.out.println(start + " // " + new String(new int[] { start }, 0, 1));
                    } else {
                        System.out.println(start + ":" + end + " // " + new String(new int[] { start }, 0, 1) + "-" + new String(new int[] { end }, 0, 1));

                    }
                }
            }
        }

        48:57 // 0-9
		1632:1641 // ٠-٩
		1776:1785 // ۰-۹
		1984:1993 // ߀-߉
		2406:2415 // ०-९
		2534:2543 // ০-৯
		2662:2671 // ੦-੯
		2790:2799 // ૦-૯
		2918:2927 // ୦-୯
		3046:3055 // ௦-௯
		3174:3183 // ౦-౯
		3302:3311 // ೦-೯
		3430:3439 // ൦-൯
		3664:3673 // ๐-๙
		3792:3801 // ໐-໙
		3872:3881 // ༠-༩
		4160:4169 // ၀-၉
		4240:4249 // ႐-႙
		6112:6121 // ០-៩
		6160:6169 // ᠐-᠙
		6470:6479 // ᥆-᥏
		6608:6617 // ᧐-᧙
		6784:6793 // ᪀-᪉
		6800:6809 // ᪐-᪙
		6992:7001 // ᭐-᭙
		7088:7097 // ᮰-᮹
		7232:7241 // ᱀-᱉
		7248:7257 // ᱐-᱙
		42528:42537 // ꘠-꘩
		43216:43225 // ꣐-꣙
		43264:43273 // ꤀-꤉
		43472:43481 // ꧐-꧙
		43600:43609 // ꩐-꩙
		44016:44025 // ꯰-꯹
		65296:65305 // ０-９
		66720:66729 // 𐒠-𐒩
		69734:69743 // 𑁦-𑁯
		69872:69881 // 𑃰-𑃹
		69942:69951 // 𑄶-𑄿
		70096:70105 // 𑇐-𑇙
		71360:71369 // 𑛀-𑛉
		120782:120831 // 𝟎-𝟿

	 */

	public static boolean isDigit(int codePoint) {
		return isDigit((char) codePoint);
	}

	native public static boolean isDefined(char ch);

	native public static boolean isDefined(int codePoint);

	public static boolean isLetter(char ch) {
		return ((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'));
	}

	public static boolean isLetter(int codePoint) {
		return isLetter((char)codePoint);
	}

	public static boolean isLetterOrDigit(char ch) {
		return isLetter(ch) || isDigit(ch);
	}

	public static boolean isLetterOrDigit(int codePoint) {
		return isLetter(codePoint) || isDigit(codePoint);
	}

	@Deprecated
	native public static boolean isJavaLetter(char ch);

	@Deprecated
	native public static boolean isJavaLetterOrDigit(char ch);

	native public static boolean isAlphabetic(int codePoint);

	native public static boolean isIdeographic(int codePoint);

	native public static boolean isJavaIdentifierStart(char ch);

	native public static boolean isJavaIdentifierStart(int codePoint);

	native public static boolean isJavaIdentifierPart(char ch);

	native public static boolean isJavaIdentifierPart(int codePoint);

	native public static boolean isUnicodeIdentifierStart(char ch);

	native public static boolean isUnicodeIdentifierStart(int codePoint);

	native public static boolean isUnicodeIdentifierPart(char ch);

	native public static boolean isUnicodeIdentifierPart(int codePoint);

	native public static boolean isIdentifierIgnorable(char ch);

	native public static boolean isIdentifierIgnorable(int codePoint);

	@HaxeMethodBody("return String.fromCharCode(p0).toLowerCase().charCodeAt(0);")
	native public static char toLowerCase(char ch);

	@HaxeMethodBody("return String.fromCharCode(p0).toLowerCase().charCodeAt(0);")
	native public static int toLowerCase(int codePoint);

	@HaxeMethodBody("return String.fromCharCode(p0).toUpperCase().charCodeAt(0);")
	native public static char toUpperCase(char ch);

	@HaxeMethodBody("return String.fromCharCode(p0).toUpperCase().charCodeAt(0);")
	native public static int toUpperCase(int codePoint);

	public static char toTitleCase(char ch) {
		// @TODO: Approximation
		return toUpperCase(ch);
	}

	public static int toTitleCase(int codePoint) {
		return toTitleCase((char)codePoint);
	}

	public static int digit(char ch, int radix) {
		if (ch >= '0' && ch <= '9') return ch - '0';
		if (ch >= 'a' && ch <= 'z') return (ch - 'a') + 10;
		if (ch >= 'A' && ch <= 'Z') return (ch - 'A') + 10;
		return -1;
	}

	public static int digit(int codePoint, int radix) {
		return digit((char) codePoint, radix);
	}

	public static int getNumericValue(char ch) {
		return digit(ch, 10);
	}

	public static int getNumericValue(int codePoint) {
		return digit(codePoint, 10);
	}

	@Deprecated
	public static boolean isSpace(char value) {
		return (value <= 0x0020) && (((((1L << 0x0009) | (1L << 0x000A) | (1L << 0x000C) | (1L << 0x000D) | (1L << 0x0020)) >> value) & 1L) != 0);
	}

	public static boolean isSpaceChar(char ch) {
		return isSpaceChar((int) ch);
	}

	public static boolean isSpaceChar(int codePoint) {
		switch (codePoint) {
			case 0x0020:
			case 0x00A0:
			case 0x1680:
			case 0x180E:
			case 0x2000:
			case 0x2001:
			case 0x2002:
			case 0x2003:
			case 0x2004:
			case 0x2005:
			case 0x2006:
			case 0x2007:
			case 0x2008:
			case 0x2009:
			case 0x200A:
			case 0x200B:
			case 0x202F:
			case 0x205F:
			case 0x3000:
			case 0xFEFF:
				return true;
		}
		return false;
	}

	public static boolean isWhitespace(char ch) {
		return isWhitespace((int)ch);
	}

	public static boolean isWhitespace(int codePoint) {
		switch (codePoint) {
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 28:
			case 29:
			case 30:
			case 31:
			case 32:
			case 5760:
			case 6158:
			case 8192:
			case 8193:
			case 8194:
			case 8195:
			case 8196:
			case 8197:
			case 8198:
			case 8200:
			case 8201:
			case 8202:
			case 8232:
			case 8233:
			case 8287:
			case 12288:
				return true;
		}
		return false;
	}

	public static boolean isISOControl(char ch) {
		return isISOControl((int) ch);
	}

	public static boolean isISOControl(int codePoint) {
		return codePoint <= 0x9F && (codePoint >= 0x7F || (codePoint >>> 5 == 0));
	}

	native public static int getType(char ch);

	native public static int getType(int codePoint);

	public static char forDigit(int digit, int radix) {
		if (digit >= 0 && digit <= 9) return (char) ('0' + (digit - 0));
		if (digit >= 10 && digit <= 35) return (char) ('a' + (digit - 10));
		return '\0';
	}

	public static byte getDirectionality(char ch) {
		return getDirectionality((int) ch);
	}

	public static boolean isMirrored(char ch) {
		return isMirrored((int) ch);
	}

	native public static byte getDirectionality(int codePoint);

	native public static boolean isMirrored(int codePoint);

	public int compareTo(Character anotherCharacter) {
		return compare(this.value, anotherCharacter.value);
	}

	public static int compare(char l, char r) {
		return l - r;
	}

	static char[] toUpperCaseCharArray(int codePoint) {
		return new char[]{(char) toUpperCase(codePoint)};
	}

	public static final int SIZE = 16;
	public static final int BYTES = SIZE / Byte.SIZE;

	@HaxeMethodBody("return HaxeNatives.swap16(p0) & 0xFFFF;")
	public static char reverseBytes(char ch) {
		return (char) (((ch & 0xFF00) >> 8) | (ch << 8));
	}

	public static String getName(int codePoint) {
		// @TODO: Not implemented!
		return Integer.toHexString(codePoint);
	}

	public static class Subset {
	}

	public static final class UnicodeBlock extends Subset {
		public static final UnicodeBlock forName(String name) {
			return new UnicodeBlock();
		}

		public static final UnicodeBlock of(int codePoint) {
			return new UnicodeBlock();
		}
	}

	public static enum UnicodeScript {
		COMMON;

		public static final UnicodeScript forName(String name) {
			return COMMON;
		}

		public static final UnicodeScript of(int codePoint) {
			return COMMON;
		}
	}
}
