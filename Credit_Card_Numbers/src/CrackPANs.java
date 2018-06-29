import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

public class CrackPANs {
	public static void main(String[] args) {
		//Initialize Variables
		long duration, startTime, endTime, iterations, timeLastHash, averageDuration;
		int amountOfHashes, found, lengthIAI;
		
		BufferedWriter fileWriter = null;
		
		File hash_file, resultFile;
		HashSet<String> hashSet = null;
		
		MessageDigest md = null;
		
		StringBuilder sb = new StringBuilder();
		String hash, pan;
		
		//Save time at the beginning of program runtime to calculate duration of program
		startTime = timeLastHash = System.currentTimeMillis();
				
		//Output current time
		System.out.print("Starting at: \t");
		System.out.println(java.time.LocalTime.now()); 
				
		//Save some necessary information in variables for the algorithm
		hash_file = new File("C:\\Users\\mario.furtado\\Hashing\\hashes_SHA-1_1000.txt");
		byte[] bin = {5, 2, 0, 6, 3, 6};
		lengthIAI = 9;
		
		//Create MessageDigest to later hash-the-PANs
		try {
			md = MessageDigest.getInstance("Sha-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		//Create hashSet with all of the Hashes from the file
		try {
			hashSet = (HashSet<String>) new BufferedReader(new FileReader(hash_file)).lines().collect(Collectors.toSet());
			//System.out.println(hashSet.toString());
		} catch (FileNotFoundException e) {
			System.exit(0);
			e.printStackTrace();
		}
		
		//Get amount of hahes in given file and output it
		amountOfHashes = hashSet.size();
		System.out.println("Amount of hashes found in file: " + amountOfHashes);
		
		System.out.println("");
		
		//Create file for results and tell create FileWriter to write into said file
		resultFile = createResultFile(hash_file);
		
		if (resultFile == null) {
			return;
		}
		
		try {
			fileWriter = new BufferedWriter(new FileWriter(resultFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		//Calculate iterations( amount of possible hashes
		iterations = (long) Math.pow(10, lengthIAI);
		
		//For every possible PAN create it's hash and look if it's contained within the hashhSet
		found = 0;
		for (long i = 0; i < iterations && found < amountOfHashes; i++) {
			pan = createPAN(bin, i, sb);
			hash = hash64(pan, md);
			
			if(hashSet.contains(hash)) {
				found++;
				System.out.println(hash + " --> " + pan + "\t [" + found + "/" + amountOfHashes + "]" + "  " + printTime(System.currentTimeMillis() - timeLastHash));
				
				//Write result to file
				try {
					fileWriter.write(hash + " --> " + pan);
					fileWriter.newLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				timeLastHash = System.currentTimeMillis();
			}
			
			sb.setLength(0);
		}
		
		//Save time at the end of program runtime to calculate duration of program
		endTime = System.currentTimeMillis();
				
		//Calculate duration
		duration = endTime - startTime;
		
		System.out.println("");
		
		//Output duration
		System.out.println("Cracking the credit card numbers took: ");
		System.out.println(duration + " Millisecond(s)");
		System.out.println(printTime(duration));
		
		System.out.println("");
		
		//Output average time per credit card
		averageDuration = duration / amountOfHashes;
		System.out.println("Cracking one card took on average: ");
		System.out.println(averageDuration + " Millisecond(s)");
		System.out.println(printTime(averageDuration ));
		
		//Output information about resultFile
		System.out.println("The results are stored in this text file: ");
		System.out.println(resultFile.getAbsolutePath());
		
		//Close the fileWriter
		try {
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Function that creates PANs
	static String createPAN (byte[] bin, long iai, StringBuilder sb) {
		byte[] result = new byte[] {0,0,0,0,0,0, 0,0,0, 0,0,0, 0,0,0, 0};
		
		System.arraycopy(bin, 0, result, 0, bin.length);
		
		int index = result.length - 2;
		
		while (iai > 0) {
			//System.out.println(iai % 10);
			result[index--] = (byte) ((byte) (iai % 10));
			iai /= 10;
		}
		
		result[result.length - 1] = checkSum(result);
		//System.out.println(Arrays.toString(result));
		return getText(result, sb);
	}
	
	//Function to return Hash in Base64
	static String hash64(String text, MessageDigest md) {
		byte[] hash;
		//System.out.println(Arrays.toString(digits));
	
		try {
			hash = hash(text, md);
		} catch(Exception e) {
			System.out.println("Problem hashing text: " + text);
			return null;
		}
		
		return Base64.getEncoder().encodeToString(hash);
	}

	//Function to hash a string using SHA-1
	static byte[] hash(String text, MessageDigest md) throws Exception {
		md.update(text.getBytes());
		//System.out.println(Arrays.toString(new String("6203631148983247").getBytes()));
		return md.digest();
	}
	
	// Function used to output time
	static String printTime(long duration) {
		/*	time[0] => Hours
		 * 	time[1] => Minutes
		 * 	time[2] => Seconds
		 * 	time[3] => Milliseconds
		 */
		String result;
		long rest = duration;
		long[] time = new long[4];
	
		time[3] = duration % 1000;
		time[2] = duration / 1000;
		rest -= (time[3]) + (time[2] * 1000);
		
		for (int i = 1; i >= 0; i--) {
			time[i] = time[i + 1] / 60;
			time[i + 1] = time[i + 1] % 60;
		}
		result = time[0] + ":" + time[1] + ":" + time[2] + ":" + time[3];
	
		return result;
	}
	
	//Function to calculate checksum for credit card numbers
	static byte checkSum (byte[] pan) {
		int sum, parity, add;
		sum = parity = add = 0;
				
		if((pan.length - 1) % 2 == 1) {
			parity = 1;
		}
				
		//System.out.println("Parity: " + parity);
				
		for(int i = 1; i < pan.length; i++) {
			if (i % 2 == parity) {
				add = pan[i - 1] * 2;
				//System.out.println(add > 9 ? (add - 9) : add);
				sum += add > 9 ? (add - 9) : add;
			} else {
				sum += pan[i - 1];
			}
		}
				
		//System.out.println(sum * 9 % 10);
		return (byte) (sum * 9 % 10);
	}

	//Function to convert byte Array into String
	static String getText(byte[] digits, StringBuilder sb) {
		for (byte digit : digits) {
			sb.append(digit);
		}
		
		return sb.toString();
	}

	//Function that creates file where results are written into
	static File createResultFile (File hash_file) {
		File result;
		
		
		result = new File(hash_file.getParent() + "//results.txt");
		
		try {
			int i = 0;
			while (!result.createNewFile()) {
				result = new File(hash_file.getParent() + "//results_" + i + ".txt");
				i++;
			}
		} catch (IOException e1) {
			System.out.println("Problem creating file for results.");
			return null;
		}
		
		return result;
	}
}
