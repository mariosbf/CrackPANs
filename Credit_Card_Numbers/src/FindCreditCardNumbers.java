import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

import javax.swing.JOptionPane;

//PAN = bin + iai + checksum
//Directory of hash-file: C:\Users\mario.furtado\Hashing\hashes_SHA-1_1000.txt
//BIN:520636
//Length of IAI: 9

public class FindCreditCardNumbers {
	public static void main(String[] args) {
		long duration, startTime, endTime, bin;
		int lengthIAI; 
		File hash_file;
		
		hash_file = new File("C:\\Users\\mario.furtado\\Hashing\\hashes_SHA-1.txt");
		bin = 520636;
		lengthIAI = 9;
		
		
		//Save time at the beginning of program runtime to calculate duration of program
		startTime = System.currentTimeMillis();
		
		//Output current time
		System.out.print("Starting at: \t");
		System.out.println(java.time.LocalTime.now()); 
		System.out.println("");
		//System.out.println("");
		
		//Finds PANs
		findPANs (bin, lengthIAI, hash_file);
		
		//Save time at the end of program runtime to calculate duration of program
		endTime = System.currentTimeMillis();
		
		//Calculate duration
		duration = endTime - startTime;
		
		//Output duration
		System.out.println("Cracking the credit card numbers took: ");
		System.out.println(duration + " Millisecond(s)");
		System.out.println(printTime(duration));
	}
	
	//Function that finds payment card numbers
		static void findPANs (long bin, int lenIAI, File hash_file) {
			String hash, pan, iai, hashPAN;
			
			int checksum, found, numberOfHashes;
			checksum = found = numberOfHashes = 0;
			
			boolean done = false;
			
			FileReader fileReader = null;
			FileWriter fileWriter = null;
			
			//Create File for results
			File results;
			
			results = new File(hash_file.getParent() + "//results.txt");
			
			try {
				int i = 0;
				while (!results.createNewFile()) {
					results = new File(hash_file.getParent() + "//results_" + i + ".txt");
					i++;
				}
				
				fileWriter = new FileWriter(results);
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(null , "Problem creating file");
				return;
			}
				
			
			//Getting amount of possible PANs
			long iterations = tenToThePowerOf(lenIAI);
			if(iterations == -1) {
				System.out.println("LenIAI is invalid: " + lenIAI);
				return;
			}
			
			//Creating filereader to read hashes
			try {
				fileReader = new FileReader(hash_file);
			} catch (FileNotFoundException e) {
				System.out.println("File not found: Problem creating Filereader.");
				e.printStackTrace();
				return;
			}
			
			//Getting amount of Hashes
			hash = nextHash(fileReader);
			
			while(hash != null) {
				hash = nextHash(fileReader);
				numberOfHashes++;
			}
			
			System.out.println("Number of Hashes: " + numberOfHashes);
			
			//Resetting fileReader in order to be able to re-read hashes
			try {
				fileReader = new FileReader(hash_file);
			} catch (FileNotFoundException e) {
				//System.out.println("File not found: Problem creating Filereader.");
				e.printStackTrace();
				return;
			}
			
			//For every hash look at all the PANs
			hash = nextHash(fileReader);
			
			while(hash != null) {
				System.out.print(hash);
				for(long i = 0; i < iterations && !done; i++) {
					iai = String.format("%0" + lenIAI + "d", i);
					checksum = checkSum(bin, iai);
					
					pan = bin + iai + checksum;
					hashPAN = hash64(pan);	
					//System.out.println(pan + " : " + hashPAN);
					
					if(hashPAN.equals(hash)) {
						found++;
						System.out.print(" => " + pan + " [" + found + "/" + numberOfHashes + "]");
						System.out.println("");
						
						try {
							fileWriter.write(hash + "\t => \t" + pan + "\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						done = true;
					}
				}
				
				if(!done) {
					System.out.println("Failed to get PAN for" + hash);
				}
				
				hash = nextHash(fileReader);
				done = false;
			}
			
			//Closing FileReader
			try {
				fileReader.close();
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		//Function to return Hash in Base64
		static String hash64(String text) {
			byte[] hash;
		
			try {
				hash = hash(text);
			} catch(Exception e) {
				System.out.println("Problem hashing text: " + text);
				return null;
			}
		
			return Base64.getEncoder().encodeToString(hash);
		}

		//Function to calculate checksum for credit card numbers
		static int checkSum (long binIn, String iaiIn) {
			long iai;
			iai = 0;
			
			try {
				iai = Long.parseLong(iaiIn);
			} catch (Exception e) {
				System.out.println("Problem cnverting iai to long");
				System.exit(0);
			}
				
				
			int result, sum, parity, add;
			result = sum = parity = add = 0;
			
			int[] digits = appendArrays(convertLongToArray(binIn, 6), convertLongToArray(iai, 9));
			
			if(digits.length % 2 == 1) {
				parity = 1;
			}
			
			for(int i = 1; i <= digits.length; i++) {
				if (i % 2 == parity) {
					add = digits[i - 1] * 2;
					if (add > 9) {
						sum += add - 9;
					} else {
						sum += add;
					}
					
				} else {
					sum += digits[i - 1];
				}
			}
				
			result = sum * 9 % 10;			
			return result;
		}
			
		
		//Function that reads next hash from File
			static String nextHash (FileReader fileReader) {
				String result = null;
				char[] resultChars = new char[28];
				int currentCharacter;
				
				if(fileReader == null) {
					System.out.println("FileReader is null.");
					return result;
				}
				
				try {
					
					for(int i = 0; i < resultChars.length; i++) {
						currentCharacter = fileReader.read();
						
						if(currentCharacter == -1) {
							return null;
						}
						
						resultChars[i] = (char) currentCharacter;
						//System.out.print(currentCharacter + " ");
					}
					
					fileReader.read();
					result = String.valueOf(resultChars);
				} catch (Exception e) {
					System.out.println("Problem reading next hash");
					System.exit(0);
				}
				//System.out.println("Next hash: " + String.valueOf(resultChars));
				return result;
			}

			//Function to hash a string using SHA-1
			static byte[] hash(String text) throws Exception {
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				md.update(text.getBytes());
				
				return md.digest();
			}

			//Function to create an array with the digits of a number
			static int[] convertLongToArray(long number, int length) {
				if(String.valueOf(number).length() > length || length <= 0) {
					return null;
				}
				
				int[] result = new int[length];
				long rest = number;
				int potencyOfTen = 10;
				int iteration = 0;
				
				while(rest != 0) {
					result[length - iteration - 1] = (int) ((rest % potencyOfTen) / (potencyOfTen / 10));
					rest -= (rest % potencyOfTen);
					potencyOfTen *= 10;
					iteration++;
				}
				
				return result;
			}

			//Function to append one array to another
			static int[] appendArrays(int[] array1, int[] array2) {
				if(array1 != null && array2 != null) {
					int[] result = new int[array1.length + array2.length];
				
					for (int i = 0; i < array1.length; i++) {
						result[i] = array1[i];
					}
				
					for (int i = array1.length; i < array1.length + array2.length; i++) {
						result[i] = array2[i - array1.length];
					}
					
					return result;
				} else {
					return null;
				}
			}

			//Function to calculate amount of iterations/possible payment card numbers
			static long tenToThePowerOf(int power) {
				if (power < 0) {
					return -1;
				} else {
					long result = 1;
					for(int i = 0; i < power; i++) {
						result *= 10;
					}
					
					return result;
				}
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
					result = time[0] + ":" + time[1] + ":" + time[2] + ":" + time[3] + "\n" + "[Hours:Minutes:Seconds:Milliseconds]";
				
					return result;
				}
}
  