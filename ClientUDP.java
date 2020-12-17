import java.net.*;  // for DatagramSocket, DatagramPacket, and InetAddress
import java.io.*;   // for IOException
import java.util.*; // for Scanner
import java.lang.*; // for Math
import java.nio.*;  // for byte buffer

/***
  //Emma Mills and Jake Galanopoulos Project 2
  //Group 2 Project
***/

public class ClientUDP {

	private static final int TIMEOUT = 3000;   // Resend timeout (milliseconds)
	private static final int MAXTRIES = 5;     // Maximum retransmissions

	public static void main(String[] args) throws IOException {

		if (args.length < 2)  // Test for correct # of args
			throw new IllegalArgumentException("Parameter(s): <Server> [<Port>]");

		InetAddress serverAddress = InetAddress.getByName(args[0]);  // Server address
		int servPort = (args.length == 2) ? Integer.parseInt(args[1]) : 7;
		
		boolean finished = false;
		int request = (int)(Math.random() * 65534 + 1);
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Welcome to Group 2's Polynomial Calculator");
		System.out.println("Reminder: x and a0 through a4 may only be between 0 and 64");
	
		while (!finished) {
			int x = 0, a0 = 0, a1 = 0, a2 = 0, a3 = 0, a4 = 0;
			int messageLength = 10;
		
			System.out.println("Please input your data below.");
			x = getInput("x", scan);
			a4 = getInput("a4", scan);
			a3 = getInput("a3", scan);
			a2 = getInput("a2", scan);
			a1 = getInput("a1", scan);
			a0 = getInput("a0", scan);
	
			String xToHex = Integer.toHexString(x).toUpperCase();
			String messageLengthToHex = Integer.toHexString(messageLength & 0xFF).toUpperCase();
			String a0ToHex = Integer.toHexString(a0 & 0xFF).toUpperCase();
			String a1ToHex = Integer.toHexString(a1 & 0xFF).toUpperCase();
			String a2ToHex = Integer.toHexString(a2 & 0xFF).toUpperCase();
			String a3ToHex = Integer.toHexString(a3 & 0xFF).toUpperCase();
			String a4ToHex = Integer.toHexString(a4 & 0xFF).toUpperCase();
			String requestToHex = Integer.toHexString(request & 0xFFFF).toUpperCase();
			
			ByteBuffer buf = ByteBuffer.allocate(messageLength - 1);
			buf.put((byte) messageLength);
			buf.putShort((short) request);
			buf.put((byte) x);
			buf.put((byte) a4);
			buf.put((byte) a3);
			buf.put((byte) a2);
			buf.put((byte) a1);
			buf.put((byte) a0);
			
			//{ (byte) messageLength, (byte) request, (byte) x, (byte) a4, (byte) a3, (byte) a2, (byte) a1, (byte) a0};
			byte checkSum = (byte)calculateChecksum(buf.array());
			System.out.println("Checksum: " + checkSum);
			String checkSumToHex = Integer.toHexString(checkSum & 0xFF).toUpperCase();
		
			System.out.println("Message sent to server: 0x" + messageLengthToHex + " 0x" + requestToHex + " 0x" +  xToHex  + " 0x" + a4ToHex  + " 0x" + 
								a3ToHex + " 0x" + a2ToHex + " 0x" + a1ToHex + " 0x" + a0ToHex + " 0x" + checkSumToHex);
			
			ByteBuffer standBy = ByteBuffer.allocate(messageLength);
			standBy.put((byte) messageLength);
			standBy.putShort((short) request);
			standBy.put((byte) x);
			standBy.put((byte) a4);
			standBy.put((byte) a3);
			standBy.put((byte) a2);
			standBy.put((byte) a1);
			standBy.put((byte) a0);
			standBy.put((byte) checkSum);

			byte[] bytesToSend = standBy.array();
			
			//byte[] bytesToSend = { (byte) messageLength, (byte) request, (byte) x, (byte) a4, (byte) a3, (byte) a2, (byte) a1, (byte) a0, (byte) checkSum};

			DatagramSocket socket = new DatagramSocket();
	
			socket.setSoTimeout(TIMEOUT);  // Maximum receive blocking time (milliseconds)

			DatagramPacket sendPacket = new DatagramPacket(bytesToSend,  // Sending packet
				bytesToSend.length, serverAddress, servPort);

			DatagramPacket receivePacket =                              // Receiving packet
				new DatagramPacket(new byte[bytesToSend.length], bytesToSend.length);

			int tries = 0;      // Packets may be lost, so we have to keep trying
			boolean receivedResponse = false;
			long startTime = System.nanoTime();
			do {
				socket.send(sendPacket);          // Send the echo string
				try {
					socket.receive(receivePacket);  // Attempt echo reply reception

					if (!receivePacket.getAddress().equals(serverAddress))  // Check source
						throw new IOException("Received packet from an unknown source");
					receivedResponse = true;
				} catch (InterruptedIOException e) {  // We did not get anything
					tries += 1;
					System.out.println("Timed out, " + (MAXTRIES-tries) + " more tries...");
				}
			} while ((!receivedResponse) && (tries < MAXTRIES));
			long endTime = System.nanoTime();
			long durationInNS = (endTime - startTime);
			double durationInMS = (double)durationInNS;
			durationInMS = durationInMS / 1000000;
			if (receivedResponse) {
				byte[] rawData = receivePacket.getData();
				ByteBuffer bb = ByteBuffer.wrap(rawData);
				byte TML = bb.get();
				short requestID = bb.getShort();
				byte errorCode = bb.get();
				int result = bb.getInt();
				byte checksum = bb.get();
				
				if (errorCode == (byte) 63) {
					System.out.println("Error Code 63. Checksum does not match");
				} else if (errorCode == (byte) 127) {
					System.out.println("Error Code 127. Request invalid");
				} else {
					System.out.println("Original Polynomial: " + a4 + "x^4 + " + a3 + "x^3 + " + a2 + "x^2 + " + a1 + "x + " + a0);
					System.out.println("Value of X: " + x);
					System.out.println("Result: " + result);
					System.out.println("Round Trip Time: " + durationInMS + " ms"); 
				}
			} else
				System.out.println("No response -- giving up.");

			socket.close();
			request++;
			// Continue Control
			System.out.print("Continue? [y/n] ");
			String c =  scan.nextLine();
			if (c.compareTo("n") == 0) {
				finished = true;
			}
		}
	}
  
	public static int getInput(String input, Scanner scan) {
		int holder = 0;
		boolean validInput = false;
		while (!validInput) {
			System.out.print("Please insert " + input + ": ");
			holder = scan.nextInt();
			if (holder < 0 || holder > 64) {
				System.out.println("That number is out of bounds. Please try again");
			} else
				validInput = true;
			scan.nextLine(); //Cleans input buffer.
		}
		return holder;
	}
  
	public static long calculateChecksum(byte[] buf) {
		int length = buf.length;
		int i = 0;

		long sum = 0;
		long data;
	
		while (length > 1) {
			data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
			sum += data;
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}

			i += 2;
			length -= 2;
		}
		if (length > 0) {
			sum += (buf[i] << 8 & 0xFF00);
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}

		sum = ~sum;
		sum = sum & 0xFFFF;
		return sum;
	}
}
