package app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.google.common.io.PatternFilenameFilter;

public class MSETool {

	private int size = -1;
	private int parts = 1;
	private String content = "";
	private String path = "";
	
	public enum SPLIT_MODE {SIZE, COUNT};
	
	public MSETool () {
		
	}
	
	public void readMSE(String path) {
		try {
			System.out.println("Reading "+path);
			this.content = FileUtils.readFileToString(new File(path));
			this.path = path;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void splitMSE(int size, SPLIT_MODE mode) {
		//TODO: check result for completeness
		int length = content.length();

		switch (mode) {
		case COUNT:
			this.setParts(size);
			this.size = length / this.getParts();
			break;
		case SIZE:
			this.size = size;
			this.setParts(length / this.size);
			break;
		default:
			break;
		}
		
		String[] elements = content.split("\n\n");
		System.out.println(elements.length+" elements, "+length+" bytes");
		System.out.println("  -> Split into "+this.getParts()+" parts, around "+this.size+" bytes or less each");

		String part = "";
		int p = 0;
		
		String prefix = "";
		String suffix = "\n)\n";
		String delimiter = "\n\n";
		
		ArrayList<String> filteredElements = new ArrayList<>();
		filteredElements.add(prefix);

		for (String e : elements) {
			filteredElements.add(e);
			filteredElements.add(delimiter);

			if (part.length()>this.size) {
				System.out.println("  -> Writing "+p+"/"+this.getParts() + " with "+filteredElements.size()/2+" elements");
				filteredElements.add(suffix);
				writeMSEelements(filteredElements, p+"");
				prefix = "(\n";
				filteredElements.clear();
				filteredElements.add(prefix);
				p++;
			}
		}
		System.out.println("  -> Writing "+p+"/"+this.getParts() + " with "+filteredElements.size()/2+" elements");
		filteredElements.add(suffix);
		writeMSEelements(filteredElements, p+"");
	}

	
	public void filterMSE(String[] filter, String pathSuffix) {
		//TODO: check result for completeness
		int length = content.length();
			
		String[] elements = content.split("\n\n");
		System.out.println(elements.length+" elements, "+length+" bytes");

		String prefix = "";
		String suffix = "\n)\n";
		String delimiter = "\n\n";
		
		prefix = "(\n";
		
		ArrayList<String> filteredElements = new ArrayList<>();
		filteredElements.add(prefix);
		
		boolean regex = false;
		String pattern = "(?s)^\\(FAMIX\\.((Method)|(FileAnchor)|(Function)|(Class)|(Module)).+";
		if (filter.length > 0) {
			pattern = "(?s)^\\(FAMIX\\.(";
			String alternatives = "";
			for (String f : filter) {
				alternatives+="|("+f+")";
			}
			pattern += alternatives.substring(1);
			pattern += ").+";
			regex = true;
		}

		for (String e : elements) {
			if (regex) {
				if (e.matches(pattern)) {
					filteredElements.add(e);
					filteredElements.add(delimiter);
				}
			} else {
				if (e.startsWith("(FAMIX.Method") 
						|| e.startsWith("(FAMIX.Function") 
						|| e.startsWith("(FAMIX.FileAnchor") 
						|| e.startsWith("(FAMIX.Class")
						|| e.startsWith("(FAMIX.Module")) {
					filteredElements.add(e);
					filteredElements.add(delimiter);
				}
			}
		}

		System.out.println("  -> Writing "+filteredElements.size()/2+"/"+elements.length + " filtered elements");
		filteredElements.add(suffix);
		writeMSEelements(filteredElements, pathSuffix);
	}

	public void splitMSEi(int size, SPLIT_MODE mode) {
		//TODO: check result for completeness
		int length = content.length();
		
		switch (mode) {
		case COUNT:
			this.setParts(size);
			this.size = length / this.getParts();
			break;
		case SIZE:
			this.size = size;
			this.setParts(length / this.size);
			break;
		default:
			break;
		}
			
		System.out.println("  -> Split into "+this.getParts()+" parts, around "+this.size+" bytes or less each");
		
		String part = "";
		int p = 0;
		
		String prefix = "";
		String suffix = "\n)\n";

		int fromIndex = 0;
		int toIndex = 0;
		while (fromIndex<length-1 && p<=this.getParts()) {
			//TODO: count elements?
			toIndex = content.indexOf(")\n\n", fromIndex+this.size)+2;
			if (toIndex < fromIndex) {
				toIndex = length-1;
				suffix = "";
			}
			part+=content.substring(fromIndex, toIndex);
			System.out.println("  -> Writing "+p+"/"+this.getParts() + " with "+(toIndex-fromIndex)+" bytes");
			
			part+=suffix;
			writeMSE(part, p+"");
			prefix = "(\n";
			part = prefix;
			p++;

			fromIndex=toIndex;
		}
	}

	private void writeMSEelements(ArrayList<String> lines, String p) {
		File source = new File(this.path);
		String partPath = source.getAbsoluteFile().getParentFile().getAbsolutePath()+"/"+p+"/model.mse";
		System.out.println("    -> To file "+partPath); 
		try {
			File target = new File(partPath);
			target.getParentFile().mkdirs();
			FileUtils.writeLines(target, lines);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void writeMSE(String part, String p) {
		File source = new File(this.path);
		String partPath = source.getAbsoluteFile().getParentFile().getAbsolutePath()+"/"+p+"/model.mse";
		System.out.println("    -> To file "+partPath); 
		try {
			File target = new File(partPath);
			target.getParentFile().mkdirs();
			FileUtils.writeStringToFile(target, part);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getParts() {
		return parts;
	}

	public void setParts(int parts) {
		this.parts = parts;
	}
}
