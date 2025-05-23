package com.diycircuits.cangjie.host;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Convert {

	public static void convertQuick() {
		try {

			HashMap<String, ArrayList<Character>> charList = new HashMap<>();

			int totalQuickColumn = 3;
			InputStream fis = Convert.class.getClassLoader().getResourceAsStream("quick-classic.txt");
			InputStreamReader input = new InputStreamReader(fis, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(input);
			String str;
			int index = 0;
			int total = 0;
			ArrayList<String> keyList = new ArrayList<>();
			System.out.println("#define QUICK_COLUMN " + totalQuickColumn);
			System.out.println("const jchar quick[][QUICK_COLUMN] = {");
			while((str = reader.readLine()) != null) {
				index = str.indexOf('\t');
				if (index < 0) index = str.indexOf(' ');
				if (index > 0) {
					StringBuffer sb = new StringBuffer();
					// System.out.print("\t { ");
					if ((int) str.charAt(1) == 9 || str.charAt(1) == ' ')  {
						// System.out.print("'" + str.charAt(0) + "',   0, ");
						sb.append(str.charAt(0));
					} else {
						sb.append(str.charAt(0));
						sb.append(str.charAt(1));
						// System.out.print("'" + str.charAt(0) + "', '" + str.charAt(1) + "', ");
					}
					String key = sb.toString();
					// System.out.println((int) str.charAt(index + 1) + " }, ");
					char ch = str.charAt(index + 1);

					if (!keyList.contains(key)) keyList.add(key);

					if (charList.containsKey(key)) {
						charList.get(key).add(ch);
					} else {
						ArrayList<Character> c = new ArrayList<Character>();
						c.add(ch);
						charList.put(key, c);
					}
					total++;
				}
			}
			for (int count = 0; count < keyList.size(); count++) {
				String k = keyList.get(count);
				ArrayList<Character> l = charList.get(k);
				for (int loop = 0; loop < l.size(); loop++) {
					if (k.length() == 1) {
						System.out.println("\t { '" + k.charAt(0) + "',   0, " + l.get(loop) + " }, ");
					} else {
						System.out.println("\t { '" + k.charAt(0) + "', '" + k.charAt(1) + "', " + l.get(loop) + " }, ");
					}
				}
			}
			System.out.println("};");
			System.out.println("jint quick_index[" + total + "];");
			System.out.println("jint quick_frequency[" + total + "];");
			reader.close();
			input.close();
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void convertCangjie() {
		try {
			int totalCangjieColumn = 6;
			InputStream fis = Convert.class.getClassLoader().getResourceAsStream("cj");
			InputStreamReader input = new InputStreamReader(fis, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(input);
			String str;
			int index = 0;
			int total = 0;
			char[] column = new char[5];
			System.out.println("#define CANGJIE_COLUMN " + totalCangjieColumn);
			System.out.println("const jchar cangjie[][CANGJIE_COLUMN] = {");
			while((str = reader.readLine()) != null) {
				index = str.indexOf('\t');
				if (index < 0) index = str.indexOf(' ');
				if (index > 0) {
					System.out.print("\t { ");
					for (int count = 0; count < 5; count++) {
						if (count < index) {
							column[count] = str.charAt(count);
							if (column[count] < 'a' || column[count] > 'z') column[count] = 0;
							if (((int) column[count]) >= 10 || ((int) column[count]) <= 99) System.out.print(' ');
							if (((int) column[count]) <= 9) System.out.print(' ');
							System.out.print(((int)	column[count]));
						} else {
							System.out.print("  0");
						}
						System.out.print(", ");
					}
					System.out.println((int) str.charAt(index + 1) + " }, ");
					total++;
				}
			}
			System.out.println("};");
			System.out.println("jint cangjie_index[" + total + "];");
			System.out.println("jint cangjie_frequency[" + total + "];");
			reader.close();
			input.close();
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static class CangjieChar {
		public char c;
		public boolean hk;

		public CangjieChar(char _c, boolean _h) { c = _c; hk = _h; }
	}

	public static void convertCangjieHK() {
		try {
			ArrayList<String> codeList = new ArrayList<String>();
			HashMap<String, ArrayList<CangjieChar>> codeMap = new HashMap<String, ArrayList<CangjieChar>>();
			int totalCangjieColumn = 8;
			InputStream fis = Convert.class.getClassLoader().getResourceAsStream("cangjie3.txt");
			InputStreamReader input = new InputStreamReader(fis, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(input);
			String str = null;
			int index = 0;
			int total = 0;
			char[] column = new char[5];
			boolean hkchar = false;
			int[] counter = new int[26];

            System.out.println("#define CANGJIE_COLUMN " + totalCangjieColumn);
			System.out.println("const jchar cangjie[][CANGJIE_COLUMN] = {");
			while((str = reader.readLine()) != null) {
				if (str.compareTo("#####") == 0) {
					hkchar = true;
					continue;
				}
				index = str.indexOf('\t');
				if (index < 0) index = str.indexOf(' ');
				if (index > 0) {
					int type = Character.getType(str.charAt(index + 1));
					if (Character.isLetter(str.charAt(index + 1)) ||
							type == Character.START_PUNCTUATION || type == Character.END_PUNCTUATION ||
							type == Character.OTHER_PUNCTUATION || type == Character.MATH_SYMBOL ||
							type == Character.DASH_PUNCTUATION  || type == Character.CONNECTOR_PUNCTUATION ||
							type == Character.OTHER_SYMBOL      || type == Character.INITIAL_QUOTE_PUNCTUATION ||
							type == Character.FINAL_QUOTE_PUNCTUATION || type == Character.SPACE_SEPARATOR) {
						// System.out.print("\t { ");
						// for (int count = 0; count < 5; count++) {
						//     if (count < index) {
						// 	column[count] = str.charAt(count);
						// 	if (column[count] < 'a' || column[count] > 'z') column[count] = 0;
						// 	if (((int) column[count]) >= 10 || ((int) column[count]) <= 99) System.out.print(' ');
						// 	if (((int) column[count]) <= 9) System.out.print(' ');
						// 	System.out.print(((int)	column[count]));
						//     } else {
						// 	System.out.print("  0");
						//     }
						//     System.out.print(", ");
						// }
						// System.out.println((int) str.charAt(index + 1) + " }, ");

						String cangjie = str.substring(0, index).trim();

						char   ch      = str.charAt(index + 1);
						if (!codeList.contains(cangjie)) codeList.add(cangjie);
						ArrayList<CangjieChar> list = null;
						if (codeMap.containsKey(cangjie)) {
							list = codeMap.get(cangjie);
						} else {
							list = new ArrayList<CangjieChar>();
						}
						CangjieChar cc = new CangjieChar(ch, hkchar);
						list.add(cc);
						codeMap.put(cangjie, list);

						// total++;
					} else {
						System.err.println("Character Not Found : " + str.charAt(index + 1) + " " + Character.getType(str.charAt(index + 1)));
					}
				}
			}

			Collections.sort(codeList);

			total = 0;
			for (int count0 = 0; count0 < codeList.size(); count0++) {
				String _str = codeList.get(count0);
				ArrayList<CangjieChar> ca = codeMap.get(_str);
				for (int count1 = 0; count1 < ca.size(); count1++) {

					int i = _str.charAt(0) - 'a';
					counter[i]++;
					total++;

					int l = 0;
					for (int count2 = 0; count2 < 5; count2++) {
						if (count2 < _str.length()) {
							l = count2;
							System.out.print("'" + _str.charAt(count2) + "', ");
						} else {
							System.out.print("  0, ");
						}
					}
					System.out.println(((int) ca.get(count1).c) + ", " + (ca.get(count1).hk ? 1 : 0) + ", " + (l + 1) + ", ");
				}
			}

			System.out.println("};");

			int offset = 0;
			System.out.println("jint cangjie_code_index[26][2] = {");
			for (int count0 = 0; count0 < counter.length; count0++) {
				System.out.println("{ " + offset + "," + counter[count0] + " },");
				offset += counter[count0];
			}
			System.out.println("};");

			System.out.println("jint cangjie_index[" + total + "];");
			System.out.println("jint cangjie_frequency[" + total + "];");
			reader.close();
			input.close();
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void convertCangjie5() {
		try {
			ArrayList<String> codeList = new ArrayList<String>();
			HashMap<String, ArrayList<CangjieChar>> codeMap = new HashMap<String, ArrayList<CangjieChar>>();
			int totalCangjieColumn = 8;
			InputStream fis = Convert.class.getClassLoader().getResourceAsStream("cj5.lime");
			InputStreamReader input = new InputStreamReader(fis, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(input);
			String str = null;
			int index = 0;
			char[] column = new char[5];
			boolean hkchar = false;
			int[] counter = new int[26];

			for (int count = 0; count < counter.length; count++) counter[count] = 0;

			System.out.println("#define CANGJIE_COLUMN " + totalCangjieColumn);
			System.out.println("const jchar cangjie[][CANGJIE_COLUMN] = {");
			while((str = reader.readLine()) != null) {
				if (str.compareTo("#####") == 0) {
					hkchar = true;
					continue;
				}
				index = str.indexOf('\t');
				if (index < 0) index = str.indexOf(' ');
				if (index > 0) {
					int type = Character.getType(str.charAt(index + 1));
					if (Character.isLetter(str.charAt(index + 1)) ||
							type == Character.START_PUNCTUATION || type == Character.END_PUNCTUATION ||
							type == Character.OTHER_PUNCTUATION || type == Character.MATH_SYMBOL ||
							type == Character.DASH_PUNCTUATION  || type == Character.CONNECTOR_PUNCTUATION ||
							type == Character.OTHER_SYMBOL      || type == Character.INITIAL_QUOTE_PUNCTUATION ||
							type == Character.FINAL_QUOTE_PUNCTUATION || type == Character.SPACE_SEPARATOR) {
						// System.out.print("\t { ");
						// for (int count = 0; count < 5; count++) {
						//     if (count < index) {
						// 	column[count] = str.charAt(count);
						// 	if (column[count] < 'a' || column[count] > 'z') column[count] = 0;
						// 	if (((int) column[count]) >= 10 || ((int) column[count]) <= 99) System.out.print(' ');
						// 	if (((int) column[count]) <= 9) System.out.print(' ');
						// 	System.out.print(((int)	column[count]));
						//     } else {
						// 	System.out.print("  0");
						//     }
						//     System.out.print(", ");
						// }
						// System.out.println((int) str.charAt(index + 1) + " }, ");

						String cangjie = str.substring(0, index).trim();

						char   ch      = str.charAt(index + 1);
						if (!codeList.contains(cangjie)) codeList.add(cangjie);
						ArrayList<CangjieChar> list = null;
						if (codeMap.containsKey(cangjie)) {
							list = codeMap.get(cangjie);
						} else {
							list = new ArrayList<CangjieChar>();
						}
						CangjieChar cc = new CangjieChar(ch, hkchar);
						list.add(cc);
						codeMap.put(cangjie, list);

						// total++;
					} else {
						System.err.println("Character Not Found : " + str.charAt(index + 1) + " " + Character.getType(str.charAt(index + 1)));
					}
				}
			}

			Collections.sort(codeList);

			int total = 0;
			for (int count0 = 0; count0 < codeList.size(); count0++) {
				String _str = codeList.get(count0);
				ArrayList<CangjieChar> ca = codeMap.get(_str);
				for (int count1 = 0; count1 < ca.size(); count1++) {

					int i = _str.charAt(0) - 'a';
					counter[i]++;
					total++;

					int l = 0;
					for (int count2 = 0; count2 < 5; count2++) {
						if (count2 < _str.length()) {
							l = count2;
							System.out.print("'" + _str.charAt(count2) + "', ");
						} else {
							System.out.print("  0, ");
						}
					}
					System.out.println(((int) ca.get(count1).c) + ", " + (ca.get(count1).hk ? 1 : 0) + ", " + (l + 1) + ",");
				}
			}

			System.out.println("};");

			int offset = 0;
			System.out.println("jint cangjie_code_index[26][2] = {");
            for (int i : counter) {
                System.out.println("{ " + offset + "," + i + " },");
                offset += i;
            }
			System.out.println("};");

			System.out.println("jint cangjie_index[" + total + "];");
			System.out.println("jint cangjie_frequency[" + total + "];");
			reader.close();
			input.close();
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void convertCantonese() {
		try {
			ArrayList<String> codeList = new ArrayList<String>();
			HashMap<String, ArrayList<CangjieChar>> codeMap = new HashMap<String, ArrayList<CangjieChar>>();
			int totalCangjieColumn = 9;
			InputStream fis = Convert.class.getClassLoader().getResourceAsStream("Cantonese.txt.in");
			InputStreamReader input = new InputStreamReader(fis, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(input);
			String str = null;
			int index = 0;
			char[] column = new char[5];
			boolean hkchar = false;
			int[] counter = new int[26];

            System.out.println("#define CANTONESE_COLUMN " + totalCangjieColumn);
			System.out.println("const jchar cantonese[][CANTONESE_COLUMN] = {");
			while((str = reader.readLine()) != null) {
				if (str.compareTo("#####") == 0) {
					hkchar = true;
					continue;
				}
				index = str.indexOf('\t');
				if (index < 0) index = str.indexOf(' ');
				if (index > 0) {
					int type = Character.getType(str.charAt(index + 1));
					if (Character.isLetter(str.charAt(index + 1)) ||
							type == Character.START_PUNCTUATION || type == Character.END_PUNCTUATION ||
							type == Character.OTHER_PUNCTUATION || type == Character.MATH_SYMBOL ||
							type == Character.DASH_PUNCTUATION  || type == Character.CONNECTOR_PUNCTUATION ||
							type == Character.OTHER_SYMBOL      || type == Character.INITIAL_QUOTE_PUNCTUATION ||
							type == Character.FINAL_QUOTE_PUNCTUATION || type == Character.SPACE_SEPARATOR) {
						// System.out.print("\t { ");
						// for (int count = 0; count < 5; count++) {
						//     if (count < index) {
						// 	column[count] = str.charAt(count);
						// 	if (column[count] < 'a' || column[count] > 'z') column[count] = 0;
						// 	if (((int) column[count]) >= 10 || ((int) column[count]) <= 99) System.out.print(' ');
						// 	if (((int) column[count]) <= 9) System.out.print(' ');
						// 	System.out.print(((int)	column[count]));
						//     } else {
						// 	System.out.print("  0");
						//     }
						//     System.out.print(", ");
						// }
						// System.out.println((int) str.charAt(index + 1) + " }, ");

						String cangjie = str.substring(0, index).trim();

						char   ch      = str.charAt(index + 1);
						if (!codeList.contains(cangjie)) codeList.add(cangjie);
						ArrayList<CangjieChar> list = null;
						if (codeMap.containsKey(cangjie)) {
							list = codeMap.get(cangjie);
						} else {
							list = new ArrayList<CangjieChar>();
						}
						CangjieChar cc = new CangjieChar(ch, hkchar);
						list.add(cc);
						codeMap.put(cangjie, list);

						// total++;
					} else {
						System.err.println("Character Not Found : " + str.charAt(index + 1) + " " + Character.getType(str.charAt(index + 1)));
					}
				}
			}

			Collections.sort(codeList);

			int total = 0;
			for (int count0 = 0; count0 < codeList.size(); count0++) {
				String _str = codeList.get(count0);
				ArrayList<CangjieChar> ca = codeMap.get(_str);
				for (int count1 = 0; count1 < ca.size(); count1++) {

					int i = _str.charAt(0) - 'a';
					counter[i]++;
					total++;

					int l = 0;
					for (int count2 = 0; count2 < 6; count2++) {
						if (count2 < _str.length()) {
							l = count2;
							System.out.print("'" + _str.charAt(count2) + "', ");
						} else {
							System.out.print("  0, ");
						}
					}
					System.out.println(((int) ca.get(count1).c) + ", " + (ca.get(count1).hk ? 1 : 0) + ", " + (l + 1) + ", ");
				}
			}

			System.out.println("};");

			int offset = 0;
			System.out.println("jint cantonese_code_index[26][2] = {");
			for (int count0 = 0; count0 < counter.length; count0++) {
				System.out.println("{ " + offset + "," + counter[count0] + " },");
				offset += counter[count0];
			}
			System.out.println("};");

			System.out.println("jint cantonese_index[" + total + "];");
			System.out.println("jint cantonese_frequency[" + total + "];");
			reader.close();
			input.close();
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void convertDayi3() {
		try {
			String keyMap = ",./0123456789;ABCDEFGHIJKLMNOPQRSTUVWXYZ";

			ArrayList<String> codeList = new ArrayList<String>();
			HashMap<String, ArrayList<CangjieChar>> codeMap = new HashMap<String, ArrayList<CangjieChar>>();
			int totalCangjieColumn = 9;
			InputStream fis = Convert.class.getClassLoader().getResourceAsStream("dayi3.cin");
			InputStreamReader input = new InputStreamReader(fis, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(input);
			String str = null;
			int index = 0;
			char[] column = new char[5];
			boolean hkchar = false;
			int[] counter = new int[40];

            System.out.println("#define DAYI3_COLUMN " + totalCangjieColumn);
			System.out.println("const jchar dayi3[][DAYI3_COLUMN] = {");
			while((str = reader.readLine()) != null) {
				if (str.compareTo("#####") == 0) {
					hkchar = true;
					continue;
				}
				index = str.indexOf('\t');
				if (index < 0) index = str.lastIndexOf(' ');
				if (index > 0) {
					int type = Character.getType(str.charAt(index + 1));
					if (Character.isLetter(str.charAt(index + 1)) ||
							type == Character.START_PUNCTUATION || type == Character.END_PUNCTUATION ||
							type == Character.OTHER_PUNCTUATION || type == Character.MATH_SYMBOL ||
							type == Character.DASH_PUNCTUATION  || type == Character.CONNECTOR_PUNCTUATION ||
							type == Character.OTHER_SYMBOL      || type == Character.INITIAL_QUOTE_PUNCTUATION ||
							type == Character.FINAL_QUOTE_PUNCTUATION || type == Character.SPACE_SEPARATOR) {
						// System.out.print("\t { ");
						// for (int count = 0; count < 5; count++) {
						//     if (count < index) {
						// 	column[count] = str.charAt(count);
						// 	if (column[count] < 'a' || column[count] > 'z') column[count] = 0;
						// 	if (((int) column[count]) >= 10 || ((int) column[count]) <= 99) System.out.print(' ');
						// 	if (((int) column[count]) <= 9) System.out.print(' ');
						// 	System.out.print(((int)	column[count]));
						//     } else {
						// 	System.out.print("  0");
						//     }
						//     System.out.print(", ");
						// }
						// System.out.println((int) str.charAt(index + 1) + " }, ");

						String cangjie = str.substring(0, index).trim();

						char   ch      = str.charAt(index + 1);
						if (!codeList.contains(cangjie)) codeList.add(cangjie);
						ArrayList<CangjieChar> list = null;
						if (codeMap.containsKey(cangjie)) {
							list = codeMap.get(cangjie);
						} else {
							list = new ArrayList<CangjieChar>();
						}
						CangjieChar cc = new CangjieChar(ch, hkchar);
						list.add(cc);
						codeMap.put(cangjie, list);

						// total++;
					} else {
						System.err.println("Character Not Found : " + str.charAt(index + 1) + " " + Character.getType(str.charAt(index + 1)));
					}
				}
			}

            Collections.sort(codeList);

			int total = 0;
			for (int count0 = 0; count0 < codeList.size(); count0++) {
				String _str = codeList.get(count0);
				ArrayList<CangjieChar> ca = codeMap.get(_str);
				for (int count1 = 0; count1 < ca.size(); count1++) {

					// int i = _str.charAt(0) - 'a';
					// System.err.println("Key " + _str.charAt(0) + " " +
					// 		       keyMap.indexOf(_str.charAt(0)));
					int i = keyMap.indexOf(_str.charAt(0));
					if (i < 0) continue;
					counter[i]++;
					total++;

					int l = 0;
					for (int count2 = 0; count2 < 6; count2++) {
						if (count2 < _str.length()) {
							l = count2;
							System.out.print("'" + _str.charAt(count2) + "', ");
						} else {
							System.out.print("  0, ");
						}
					}
					System.out.println(((int) ca.get(count1).c) + ", " + (ca.get(count1).hk ? 1 : 0) + ", " + (l + 1) + ", ");
				}
			}

			System.out.println("};");

			int offset = 0;
			System.out.println("jint dayi3_code_index[40][2] = {");
            for (int i : counter) {
                System.out.println("{ " + offset + "," + i + " },");
                offset += i;
            }
			System.out.println("};");

			System.out.println("jint dayi3_index[" + total + "];");
			System.out.println("jint dayi3_frequency[" + total + "];");
			reader.close();
			input.close();
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void convertStroke() {
		try {
			StringBuffer sb = new StringBuffer();
			InputStream fis = Convert.class.getClassLoader().getResourceAsStream("StrokeOrder.sql");
			InputStreamReader input = new InputStreamReader(fis, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(input);

			HashMap<String, String> mapping = new HashMap<String, String>();
			ArrayList<String> order = new ArrayList<String>();
			String line = null;
			int total = 0, max = 0;
			while((line = reader.readLine()) != null) {
				if (!line.startsWith("INSERT INTO StrokeOrder"))
					continue;
				line = line.substring(62);
				if (line.length() < 3)
					continue;
				line = line.substring(0, line.length() - 2);
				StringTokenizer token = new StringTokenizer(line, ",");
				if (token.countTokens() != 3)
					continue;
				String stroke = token.nextToken();
				String ch     = token.nextToken();

				if (!ch.startsWith("'") || !ch.endsWith("'"))
					continue;

				if (!stroke.startsWith("'") || !stroke.endsWith("'"))
					continue;

				byte[] b = ch.substring(1, 2).getBytes("Big5-HKSCS");
				if (b.length != 2)
					continue;

				if (stroke.compareTo("'6'") == 0 ||
						stroke.compareTo("'7'") == 0)
					continue;

				stroke = stroke.substring(1, stroke.length() - 1);
				ch     = ch.substring(1, 2);

				if (stroke.length() > max) max = stroke.length();

				if (mapping.containsKey(stroke)) {
					mapping.put(stroke, mapping.get(stroke) + ch);
				} else {
					mapping.put(stroke, ch);
				}

				if (!order.contains(stroke)) order.add(stroke);
				// System.out.println(total + " " + stroke + " " + ch + " " + b.length + " " + ch + " " + b[0]);

				total++;
			}

            int allkey = 0;
			int lastKey = 0;
			int[] first_index = new int[5];
			int[] first_count = new int[5];
			int _max = ((max & ~0x01) + ((max & 0x01) << 1)) >> 1;
			for (int count = 0; count < 5; count++) first_count[count] = 0;
			System.out.println("struct STROKE_ORDER {");
			System.out.println("char stroke[" + (_max + 1) + "];");
			System.out.println("int ch;");
			System.out.println("int num;");
			System.out.println("} stroke[] = {");
			for (int count = 0; count < order.size(); count++) {
				String ch = mapping.get(order.get(count));
				for (int count0 = 0; count0 < ch.length(); count0++) {
					if (lastKey != order.get(count).charAt(0)) {
						lastKey = order.get(count).charAt(0);
						System.err.println("Last Key " + lastKey + " " + count);
						if (lastKey == '1')
							first_index[lastKey - '1'] = 0;
						else
							first_index[lastKey - '1'] = first_count[lastKey - '1' - 1] + first_index[lastKey - '1' - 1];
					}
					sb.setLength(0);
					String key = order.get(count);
					int keylen = key.length();
					keylen = (keylen & ~0x01) + ((keylen & 1) << 1);
					char pair = 0;
					for (int count1 = 0; count1 < keylen; count1++) {
						pair = (char) (pair << 4);
						if (count1 < key.length()) {
							if (key.charAt(count1) < '1' || key.charAt(count1) > '5')
								continue;
							char c = key.charAt(count1);
							pair = (char) (pair | (char) (c - '0'));
							if ((count1 & 0x01) == 0)
								continue;
						}
						sb.append("\\x");
						sb.append(Integer.toHexString(pair));
						pair = 0;
					}
					System.out.print(" { \"" + sb + "\", ");
					System.out.print((int) ch.charAt(count0));
					System.out.print(", ");
					System.out.print(order.get(count).length());
					System.out.println(" },");
					allkey++;
				}
				first_count[lastKey - '1'] += ch.length();
			}
			System.out.println("};");

			System.out.println("struct STROKE_MAP {\nint index;\nint count;\n} stroke_map[5] = {");
			for (int count = 0; count < 5; count++) {
				System.out.println("{ " + first_index[count] + ", " + first_count[count] + " },");
			}
			System.out.println("};");

			max = ((max & ~0x01) + ((max & 0x01) << 1)) >> 1;
			System.out.println("#define STROKE_MAXKEY " + max);
			System.out.println("#define STROKE_TOTAL " + allkey);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {

		if (args.length != 1) return;
		if (args[0].compareTo("0") == 0)
			Convert.convertQuick();
		if (args[0].compareTo("1") == 0)
			Convert.convertCangjieHK();
		if (args[0].compareTo("2") == 0)
			Convert.convertStroke();
		if (args[0].compareTo("3") == 0)
			Convert.convertCangjie5();
		if (args[0].compareTo("4") == 0)
			Convert.convertCantonese();
		if (args[0].compareTo("5") == 0)
			Convert.convertDayi3();
	}

}
