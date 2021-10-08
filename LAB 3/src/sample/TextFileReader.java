package sample;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TextFileReader {
	public String read(File file) throws FileNotFoundException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		return br.lines().collect(Collectors.joining(System.lineSeparator()));
	}
}