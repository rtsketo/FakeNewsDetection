package eu.rtsketo.fnd.tagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author DatQuocNguyen
 * 
 */
public class Utils
{
	static List<WordTag> getWordTagList(String initializedSentence)
	{
		List<WordTag> wordTagList = new ArrayList<WordTag>();
		for (String wordTag : initializedSentence.split("\\s+")) {
			wordTag = wordTag.trim();
			if (wordTag.length() == 0)
				continue;

			if (wordTag.equals("///"))
				wordTagList.add(new WordTag("/", "/"));
			else {
				int index = wordTag.lastIndexOf("/");
				wordTagList.add(new WordTag(wordTag.substring(0, index),
					wordTag.substring(index + 1)));
			}
		}
		return wordTagList;
	}

	public static HashMap<String, String> getDictionary(String dictPath)
	{
		HashMap<String, String> dict = new HashMap<>();
		BufferedReader buffer;
		try {
			buffer = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(dictPath)), StandardCharsets.UTF_8));
			for (String line; (line = buffer.readLine()) != null;) {
				String[] wordTag = line.split(" ");
				dict.put(wordTag[0], wordTag[1]);
			}
			buffer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return dict;
	}

	static boolean isVnProperNoun(String word)
	{
		if (Character.isUpperCase(word.charAt(0))) {
			if (word.split("_").length >= 5)
				return true;
			int index = word.indexOf("_");
			while (index >= 0 && index < word.length() - 1) {
				if (Character.isLowerCase(word.charAt(index + 1))) {
					return false;
				}
				index = word.indexOf("_", index + 1);
			}
			return true;
		}
		else
			return false;

	}

	static boolean isAbbre(String word)
	{
		for (int i = 0; i < word.length(); i++) {
			if (Character.isLowerCase(word.charAt(i)) || word.charAt(i) == '_')
				return false;
		}
		return true;
	}

	static FWObject getCondition(String strCondition)
	{
		FWObject condition = new FWObject(false);

		for (String rule : strCondition.split(" and ")) {
			rule = rule.trim();
			String key = rule.substring(rule.indexOf(".") + 1,
				rule.indexOf(" "));
			String value = getConcreteValue(rule);

			switch (key) {
				case "prevWord2":
					condition.context[4] = value;
					break;
				case "prevTag2":
					condition.context[5] = value;
					break;
				case "prevWord1":
					condition.context[2] = value;
					break;
				case "prevTag1":
					condition.context[3] = value;
					break;
				case "word":
					condition.context[1] = value;
					break;
				case "tag":
					condition.context[0] = value;
					break;
				case "nextWord1":
					condition.context[6] = value;
					break;
				case "nextTag1":
					condition.context[7] = value;
					break;
				case "nextWord2":
					condition.context[8] = value;
					break;
				case "nextTag2":
					condition.context[9] = value;
					break;
				case "suffixL2":
					condition.context[10] = value;
					break;
				case "suffixL3":
					condition.context[11] = value;
					break;
				case "suffixL4":
					condition.context[12] = value;
					break;
			}
		}

		return condition;
	}

	static FWObject getObject(List<WordTag> wordtags, int size, int index)
	{
		FWObject object = new FWObject(true);

		if (index > 1) {
			object.context[4] = wordtags.get(index - 2).word;
			object.context[5] = wordtags.get(index - 2).tag;
		}

		if (index > 0) {
			object.context[2] = wordtags.get(index - 1).word;
			object.context[3] = wordtags.get(index - 1).tag;
		}

		String currentWord = wordtags.get(index).word;
		String currentTag = wordtags.get(index).tag;

		object.context[1] = currentWord;
		object.context[0] = currentTag;

		int numChars = currentWord.length();
		if (numChars >= 4) {
			object.context[10] = currentWord.substring(numChars - 2);
			object.context[11] = currentWord.substring(numChars - 3);
		}
		if (numChars >= 5) {
			object.context[12] = currentWord.substring(numChars - 4);
		}

		if (index < size - 1) {
			object.context[6] = wordtags.get(index + 1).word;
			object.context[7] = wordtags.get(index + 1).tag;
		}

		if (index < size - 2) {
			object.context[8] = wordtags.get(index + 2).word;
			object.context[9] = wordtags.get(index + 2).tag;
		}

		return object;
	}

	static String getConcreteValue(String str)
	{
		if (str.contains("\"\"")) {
			if (str.contains("Word"))
				return "<W>";
			else if (str.contains("suffixL"))
				return "<SFX>";
			else
				return "<T>";
		}
		return str.substring(str.indexOf("\"") + 1,
			str.length() - 1);
	}

	public static void main(String[] args)
	{
	}
}
